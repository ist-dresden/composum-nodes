package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.config.FilterConfiguration;
import com.composum.sling.core.exception.ParameterValidationException;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.script.GroovyService;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The JCR nodes service servlet to walk though and modify the entire hierarchy.
 */
@SlingServlet(
        paths = "/bin/core/node",
        methods = {"GET", "POST", "PUT", "DELETE"}
)
public class NodeServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(NodeServlet.class);

    public static final String FILE_CONTENT_TYPE = "application/binary";
    public static final String FILE_NAME_EXT = ".json";

    /** the possible tree name options */
    public enum LabelType {
        name, title
    }

    /** the names of the default filters configured statically */
    public static final String KEY_DEFAULT = "default";
    public static final String KEY_PAGE = "page";
    public static final String KEY_REFERENCEABLE = "referenceable";
    public static final String KEY_UNFILTERD = "unfiltered";

    public static final Pattern NODE_PATH_PATTERN = Pattern.compile("^(/[^/]+)+$");

    protected Map<String, ResourceFilter> nodeFilters = new LinkedHashMap<>();

    /**
     * injection of the configuration for the Composum core layer
     */
    @Reference
    private CoreConfiguration coreConfig;

    /**
     * injection of the filter configurations provided by the OSGi configuration
     */
    @Reference(referenceInterface = FilterConfiguration.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected List<FilterConfiguration> filterConfigurations;

    /**
     * for each configured filter in the OSGi configuration
     * a tree filter is added to the filter set
     *
     * @param config the OSGi filter configuration object
     */
    protected synchronized void bindFilterConfiguration(final FilterConfiguration config) {
        if (filterConfigurations == null) {
            filterConfigurations = new ArrayList<>();
        }
        filterConfigurations.add(config);
        String key = config.getName();
        ResourceFilter filter = config.getFilter();
        if (StringUtils.isNotBlank(key) && filter != null) {
            nodeFilters.put(key, buildTreeFilter(filter));
        }
    }

    /**
     * removing of a configuration which is not longer available;
     * removes the corresponding tree filter also
     *
     * @param config the OSGi filter configuration object to remove
     */
    protected synchronized void unbindFilterConfiguration(final FilterConfiguration config) {
        nodeFilters.remove(config.getName());
        filterConfigurations.remove(config);
    }

    /**
     * Creates a 'tree filter' as combination with the configured filter and the rules for the
     * 'intermediate' nodes (folders) to traverse up to the target nodes.
     *
     * @param configuredFilter the filter for the target nodes
     */
    protected ResourceFilter buildTreeFilter(ResourceFilter configuredFilter) {
        return new ResourceFilter.FilterSet(
                ResourceFilter.FilterSet.Rule.tree, // a tree filter including intermediate folders
                new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                        // the combination with the default mapping filter excludes 'rep:...' nodes
                        configuredFilter,
                        coreConfig.getDefaultNodeFilter()),
                coreConfig.getTreeIntermediateFilter());
    }

    /**
     * Determines the filter to use for node retrieval; scans the request for filter parameter or selector.
     */
    protected ResourceFilter getNodeFilter(SlingHttpServletRequest request) {
        ResourceFilter filter = null;
        String filterParam = RequestUtil.getParameter(request, PARAM_FILTER, (String) null);
        if (StringUtils.isNotBlank(filterParam)) {
            filter = nodeFilters.get(filterParam);
        }
        if (filter == null) {
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            for (String selector : pathInfo.getSelectors()) {
                filter = nodeFilters.get(selector);
                if (filter != null) {
                    break;
                }
            }
        }
        if (filter == null) {
            filter = coreConfig.getDefaultNodeFilter();
        }
        return filter;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    protected GroovyService groovyService;

    //
    // Servlet operations
    //

    public enum Extension {json, html, lock, groovy}

    public enum Operation {
        create, copy, move, reorder, delete, toggle,
        tree, reference, resolve, typeahead, query, filters, map,
        startScript, checkScript, stopScript
    }

    protected ServletOperationSet operations = new ServletOperationSet(Extension.json);

    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // filter configuration
        nodeFilters.put(KEY_REFERENCEABLE, buildTreeFilter(coreConfig.getReferenceableNodesFilter()));
        nodeFilters.put(KEY_UNFILTERD, ResourceFilter.ALL);
        nodeFilters.put(KEY_PAGE, buildTreeFilter(coreConfig.getPageNodeFilter()));
        nodeFilters.put(KEY_DEFAULT, coreConfig.getDefaultNodeFilter());

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.map, new MapGetOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.tree, new TreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.reference, new ReferenceOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.filters, new ListFiltersAsJson());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.filters, new ListFiltersAsHtml());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.typeahead, new TypeaheadOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.resolve, new ResolveOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.query, new JsonQueryOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.query, new HtmlQueryOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.groovy,
                Operation.startScript, new StartGroovyNodeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.groovy,
                Operation.checkScript, new CheckScriptOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.groovy,
                Operation.stopScript, new StopScriptOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.map, new MapPostOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.create, new CreateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.copy, new CopyOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.move, new MoveOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.reorder, new ReorderOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.lock,
                Operation.toggle, new ToggleLockOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.groovy,
                Operation.startScript, new StartGroovyNodeOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.groovy,
                Operation.checkScript, new CheckScriptOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.groovy,
                Operation.stopScript, new StopScriptOperation());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.map, new MapPutOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.create, new PutCreateOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.copy, new PutCopyOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.move, new PutMoveOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.reorder, new PutReorderOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.delete, new DeleteOperation());
    }

    //
    // access to the current filter configuration
    //

    protected class ListFiltersAsHtml implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            response.setContentType(MappingRules.CHARSET.name());
            response.setContentType("text/html;charset=" + MappingRules.CHARSET.name());
            Writer writer = response.getWriter();
            response.setStatus(HttpServletResponse.SC_OK);

            for (String key : nodeFilters.keySet()) {
                writer.append("<li data-filter=\"").append(key)
                        .append("\"><a href=\"#\">").append(key).append("</a></li>");
            }
        }
    }

    protected class ListFiltersAsJson implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
            response.setStatus(HttpServletResponse.SC_OK);

            jsonWriter.beginArray();
            for (String key : nodeFilters.keySet()) {
                jsonWriter.value(key);
            }
            jsonWriter.endArray();
        }
    }

    //
    // JCR queries
    //

    /** the pattern to check for a XPATH query and their simplified variation */
    public static final Pattern XPATH_QUERY = Pattern.compile(
            "^((/jcr:root)?/[^ \\(\\[]*)( +([^ /\\(\\[]+) *|(.*))$"
    );

    /** the pattern to check for a simple text (word) query */
    public static final Pattern WORD_QUERY = Pattern.compile("^ *([^ /]+) *$");

    protected class JsonQueryOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            String queryString = RequestUtil.getParameter(request, PARAM_QUERY, "");
            String queryLang = Query.XPATH;

            String text;
            // check for a X-PATH rule (starts with a path)
            Matcher matcher = XPATH_QUERY.matcher(queryString);
            if (matcher.matches()) {
                // check for a single word after the path pattern - use it as simple text pattern
                text = matcher.group(4);
                if (StringUtils.isNotBlank(text)) {
                    // use simple text separated from path
                    queryString = getSimpleQuery(matcher.group(1), text);
                }
                // add the 'jcr:root' if not present
                if (!queryString.startsWith("/jcr:root")) {
                    queryString = "/jcr:root" + queryString;
                }
            } else {
                // check for a single word only - use it as simple text pattern
                matcher = WORD_QUERY.matcher(queryString);
                if (matcher.matches()) {
                    // simple text
                    queryString = getSimpleQuery(getPath(request), matcher.group(1));
                } else {
                    // SQL-2...
                    queryLang = Query.JCR_SQL2;
                }
            }

            if (StringUtils.isNotBlank(queryString)) {
                try {
                    ResourceResolver resolver = request.getResourceResolver();
                    Session session = resolver.adaptTo(Session.class);
                    Workspace workspace = session.getWorkspace();
                    QueryManager queryManager = workspace.getQueryManager();

                    Query query = queryManager.createQuery(queryString, queryLang);
                    query.setLimit(coreConfig.getQueryResultLimit());
                    QueryResult result = query.execute();

                    ResourceFilter filter = getNodeFilter(request);
                    writeQueryResult(response, queryString, result, filter, resolver);

                } catch (RepositoryException rex) {
                    LOG.error(rex.getMessage(), rex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, rex.getMessage());
                }
            }
        }

        public String getSimpleQuery(String path, String text) {
            String queryString = "/jcr:root";
            if (StringUtils.isNotBlank(path)) {
                queryString += path;
            }
            queryString += queryString.endsWith("/") ? "/*" : "//*";
            queryString += "[jcr:contains(.,'" + text + "')] order by @path";
            return queryString;
        }

        protected boolean accept(ResourceFilter filter, Resource resource) {
            if (filter instanceof ResourceFilter.FilterSet) {
                ResourceFilter.FilterSet set = (ResourceFilter.FilterSet) filter;
                if (ResourceFilter.FilterSet.Rule.tree == set.getRule()) {
                    return filter.accept(resource) && !set.isIntermediate(resource);
                } else {
                    return filter.accept(resource);
                }
            } else {
                return filter.accept(resource);
            }
        }

        protected void writeQueryResult(SlingHttpServletResponse response, String queryString, QueryResult result,
                                        ResourceFilter filter, ResourceResolver resolver)
                throws RepositoryException, IOException {

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            response.setStatus(HttpServletResponse.SC_OK);

            NodeIterator iterator = result.getNodes();

            writer.beginArray();

            writer.name("result").beginArray();
            int count = 0;
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                ResourceHandle resource = ResourceHandle.use(resolver.getResource(node.getPath()));
                if (resource.isValid() && accept(filter, resource)) {
                    writer.beginObject();
                    String type = writeNodeIdentifiers(writer, resource, LabelType.name, false);
                    writeNodeJcrState(writer, resource);
                    writer.endObject();
                    count++;
                }
            }
            writer.endArray();

            writer.name("summary").beginObject();
            writer.name("query").value(queryString);
            writer.name("count").value(count);
            writer.name("limit").value(coreConfig.getQueryResultLimit());
            writer.endObject();

            writer.endObject();
        }
    }

    protected class HtmlQueryOperation extends JsonQueryOperation {

        @Override
        protected void writeQueryResult(SlingHttpServletResponse response, String queryString, QueryResult result,
                                        ResourceFilter filter, ResourceResolver resolver)
                throws RepositoryException, IOException {

            PrintWriter writer = response.getWriter();
            response.setStatus(HttpServletResponse.SC_OK);

            NodeIterator iterator = result.getNodes();

            writer.append("<tbody>");
            int count = 0;
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                ResourceHandle resource = ResourceHandle.use(resolver.getResource(node.getPath()));
                if (resource.isValid() && accept(filter, resource)) {
                    StringBuilder classes = new StringBuilder();
                    String nodePath = node.getPath();
                    Session session = node.getSession();
                    Workspace workspace = session.getWorkspace();
                    LockManager lockManager = workspace.getLockManager();
                    if (node.isCheckedOut()) {
                        classes.append(" checked-out");
                    }
                    boolean isLocked = node.isLocked();
                    if (isLocked) {
                        classes.append(" locked");
                        Lock lock = lockManager.getLock(nodePath);
                        String holderPath = lock.getNode().getPath();
                        if (lock.isDeep()) {
                            classes.append(" deep-lock");
                        }
                        if (holderPath.equals(nodePath)) {
                            classes.append(" lock-holder");
                        }
                    }
                    String path = resource.getPath();
                    writer.append("<tr class=\"").append(classes.toString().trim())
                            .append("\" data-path=\"").append(path)
                            .append("\">");
                    writer.append("<td class=\"icon\" data-type=\"").append(getTypeKey(resource))
                            .append("\"><span></span></td>");
                    writer.append("<td class=\"id\">").append(resource.getId()).append("</td>");
                    writer.append("<td class=\"name\">")
                            .append(StringEscapeUtils.escapeHtml4(resource.getName())).append("</td>");
                    writer.append("<td class=\"text\">")
                            .append(StringEscapeUtils.escapeHtml4(getNodeLabel(resource, LabelType.title)))
                            .append("</td>");
                    writer.append("<td class=\"path\"><a href=\"")
                            .append(StringEscapeUtils.escapeHtml4(path)).append("\">")
                            .append(StringEscapeUtils.escapeHtml4(path)).append("</a></td>");
                    writer.append("<td class=\"type\">").append(resource.getPrimaryType()).append("</td>");
                    writer.append("</tr>");
                    count++;
                }
            }
            StringBuilder message = new StringBuilder();
            message.append(count).append(" items found");
            long limit = coreConfig.getQueryResultLimit();
            if (count >= limit) {
                message.append(" (current limit: ").append(limit).append(")");
            }
            message.append(". &nbsp; ").append(queryString);
            writer.append("<tr class=\"summary info\">");
            writer.append("<td class=\"icon\" data-type=\"summary\"><span></span></td>");
            writer.append("<td class=\"message\" colspan=\"5\">").append(message).append("</td>");
            writer.append("</tr>");
            writer.append("</tbody>");
        }
    }

    //
    // node retrieval
    //

    /**
     * creates a JSON object for the requested node (requested by the suffix);
     * this JSON response contains the node identifiers, some node type hints and
     * a list of the children of the node; this operation provides the data for
     * a tree implementation which requests the nodes on demand
     * suffix: the path to the node
     * selectors / parameters:
     * - 'label': 'name' or 'title' - selects the value to use for the nodes 'text' attribute
     * URL examples:
     * - http://host/bin/core/node.tree.json/path/to/the/node
     * - http://host/bin/core/node.tree.title.json/path/to/the/node
     * - http://host/bin/core/node.tree.json/path/to/the/node?label=title
     */
    protected class TreeOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            ResourceFilter filter = getNodeFilter(request);
            LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                    RequestUtil.getSelector(request, LabelType.name));

            JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);

            response.setStatus(HttpServletResponse.SC_OK);

            writeJsonNode(jsonWriter, filter, resource, labelType, coreConfig, false);
        }
    }

    /**
     * similar to the 'tree' operation creates this operation a JSON object for the requested
     * node; but this node is requested by its id (UUID) in the suffix or as parameter; this
     * operation provides data for reference retrieval (get the node of a reference)
     * suffix: the reference of the node in a 'path notation'
     * selectors / parameters:
     * - 'label': 'name' or 'title' - selects the value to use for the nodes 'text' attribute
     * ' 'id': a parameter with the reference used instead of the id from the suffix
     * URL examples:
     * - http://host/bin/core/node.reference.json/node-id
     * - http://host/bin/core/node.reference.title.json/node-id
     * - http://host/bin/core/node.reference.json?id=node-id
     */
    protected class ReferenceOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            String reference = RequestUtil.getParameter(request, PARAM_ID,
                    AbstractServiceServlet.getPath(request));
            if (reference != null) {
                while (reference.startsWith("/")) {
                    reference = reference.substring(1);
                }

                try {
                    ResourceResolver resolver = request.getResourceResolver();
                    Session session = resolver.adaptTo(Session.class);
                    Node node = session.getNodeByIdentifier(reference);

                    Resource nodeResource;
                    if (node != null &&
                            (nodeResource = resolver.getResource(node.getPath())) != null) {

                        resource = ResourceHandle.use(nodeResource);

                        ResourceFilter filter = getNodeFilter(request);
                        LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                                RequestUtil.getSelector(request, LabelType.name));

                        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                        response.setStatus(HttpServletResponse.SC_OK);
                        writeJsonNode(jsonWriter, filter, resource, labelType, coreConfig, false);

                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "no resource found for id: " + reference);
                    }

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no reference found");
            }
        }
    }

    protected class ResolveOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            ResourceResolver resolver = request.getResourceResolver();
            String paramUrl = RequestUtil.getParameter(request, PARAM_URL, "");

            if (StringUtils.isNotBlank(paramUrl)) {

                URL url = new URL(paramUrl);
                Resource target = resolver.resolve(request, url.getPath());

                if (target != null) {
                    ResourceHandle handle = ResourceHandle.use(target);

                    ResourceFilter filter = getNodeFilter(request);
                    LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                            RequestUtil.getSelector(request, LabelType.name));

                    JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                    response.setStatus(HttpServletResponse.SC_OK);
                    writeJsonNode(jsonWriter, filter, handle, labelType, coreConfig, false);

                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "no resource found for url: " + url);
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no URL for node resolving");
            }
        }
    }

    protected class TypeaheadOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            String query = AbstractServiceServlet.getPath(request);
            String parentPath = "";
            if (query.startsWith("/")) {
                parentPath = query.substring(0, query.lastIndexOf('/'));
                query = query.substring(parentPath.length() + 1);
            }
            if (StringUtils.isBlank(parentPath)) {
                parentPath = "/";
            }

            ResourceResolver resolver = request.getResourceResolver();
            resource = ResourceHandle.use(resolver.getResource(parentPath));

            if (resource.isValid()) {

                ResourceFilter filter = new ResourceFilter.FilterSet(
                        ResourceFilter.FilterSet.Rule.and,
                        new ResourceFilter.NameFilter(
                                new StringFilter.WhiteList(".*" + query + ".*")),
                        getNodeFilter(request));

                List<Resource> matchingChilds = new ArrayList<>();
                for (Resource child : resource.getChildren()) {
                    if (filter.accept(child)) {
                        matchingChilds.add(child);
                    }
                }

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                response.setStatus(HttpServletResponse.SC_OK);

                jsonWriter.beginArray();
                for (Resource matching : matchingChilds) {
                    jsonWriter.value(matching.getPath());
                }
                jsonWriter.endArray();
            }
        }
    }

    //
    // Change Operations
    //

    protected class ToggleLockOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Node node = resource.adaptTo(Node.class);

            if (node != null) {

                Session session = node.getSession();
                Workspace workspace = session.getWorkspace();
                LockManager lockManager = workspace.getLockManager();

                try {
                    if (node.isLocked()) {
                        Lock lock = lockManager.getLock(node.getPath());
                        String token = lock.getLockToken();
                        lockManager.addLockToken(token);
                        lockManager.unlock(node.getPath());
                    } else {
                        if (!node.isNodeType(ResourceUtil.TYPE_LOCKABLE)) {
                            node.addMixin(ResourceUtil.TYPE_LOCKABLE);
                            session.save();
                        }
                        lockManager.lock(node.getPath(), true, false, Long.MAX_VALUE, session.getUserID());
                    }
                    session.save();

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                }
            }
        }
    }

    protected class MoveOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Node node = resource.adaptTo(Node.class);

            if (node != null) {

                NodeParameters params = getNodeParameters(request);
                String name = params.name;
                if (StringUtils.isBlank(name)) {
                    name = node.getName();
                }

                String newPath = params.path;
                if (!newPath.endsWith("/")) {
                    newPath += "/";
                }
                newPath += name;

                if (NODE_PATH_PATTERN.matcher(newPath).matches()) {

                    JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                    response.setStatus(HttpServletResponse.SC_OK);

                    String oldPath = node.getPath();
                    if (oldPath.equals(newPath)) {

                        response.setStatus(HttpServletResponse.SC_ACCEPTED);

                        jsonWriter.beginObject();
                        jsonWriter.name("message").value("no modification");
                        jsonWriter.endObject();

                    } else {

                        Session session = node.getSession();
                        session.move(oldPath, newPath);
                        session.save();

                        ResourceResolver resolver = resource.getResourceResolver();
                        ResourceHandle newResource = ResourceHandle.use(resolver.getResource(newPath));

                        writeJsonNode(jsonWriter, MappingRules.DEFAULT_NODE_FILTER,
                                newResource, LabelType.name, coreConfig, false);
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "invalid node path '" + newPath + "'");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "can't determine target node '" + resource.getPath() + "'");
            }
        }

        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getFormParameters(request);
        }
    }

    protected class PutMoveOperation extends MoveOperation {

        @Override
        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getJsonObject(request, NodeParameters.class);
        }
    }

    /**
     * move a node to a new position (before a specified sibling); if the node is not a
     * child of the new siblings parent the node is moved and than placed
     * 'url path' the node to move
     * - 'path' parameter: the path of the new successor
     */
    protected class ReorderOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Node node = resource.adaptTo(Node.class);

            if (node != null) {
                ResourceResolver resolver = resource.getResourceResolver();
                NodeParameters params = getNodeParameters(request);

                String name = params.name;
                if (StringUtils.isBlank(name)) {
                    name = node.getName();
                }
                String nodePath = node.getPath();

                String siblingPath = params.path;
                Resource sibling = resolver.getResource(siblingPath);
                Node siblingNode;
                Node parentNode;
                String parentPath;

                if (sibling != null && (siblingNode = sibling.adaptTo(Node.class)) != null &&
                        (parentNode = siblingNode.getParent()) != null &&
                        !nodePath.equals(parentPath = parentNode.getPath())) {

                    Session session = node.getSession();

                    // implicit move if parent of the new sibling differs or name is changed
                    String newNodePath = parentPath + (parentPath.endsWith("/") ? "" : "/") + name;
                    if (!nodePath.equals(newNodePath)) {
                        session.move(nodePath, newNodePath);
                        nodePath = newNodePath;
                    }

                    // reorder node before the specified sibling
                    parentNode.orderBefore(node.getName(), siblingNode.getName());
                    session.save();

                    ResourceHandle newResource = ResourceHandle.use(resolver.getResource(nodePath));
                    JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                    writeJsonNode(jsonWriter, MappingRules.DEFAULT_NODE_FILTER,
                            newResource, LabelType.name, coreConfig, false);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "invalid reorder paths path '" + nodePath + " / " + siblingPath + "'");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "can't determine target node '" + resource.getPath() + "'");
            }
        }

        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getFormParameters(request);
        }
    }

    protected class PutReorderOperation extends ReorderOperation {

        @Override
        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getJsonObject(request, NodeParameters.class);
        }
    }

    /**
     * The 'create' via POST (multipart form) implementation expects:
     * <ul>
     * <li>the 'path' parameter with the path of the new nodes parent</li>
     * <li>the 'name' parameter with the name of the new node</li>
     * <li>the 'file' part (form element / parameter) with the binary content (optional)</li>
     * </ul>
     */
    protected class CreateOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Node node = resource.adaptTo(Node.class);

            if (node != null) {

                NodeParameters params = getNodeParameters(request);
                String name = params.name;

                try {

                    if (StringUtils.isBlank(params.type)) {
                        throw new ParameterValidationException("invalid node type '" + params.type + "'");
                    }
                    if (StringUtils.isBlank(name)) {
                        throw new ParameterValidationException("invalid node name '" + name + "'");
                    }

                    Node newNode = NodeFactory.SINGLETON.createNode(request, node, name, params);
                    if (newNode != null) {
                        ResourceResolver resolver = resource.getResourceResolver();
                        Session session = node.getSession();
                        session.save();

                        ResourceHandle newResource = ResourceHandle.use(resolver.getResource(newNode.getPath()));
                        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                        writeJsonNode(jsonWriter, MappingRules.DEFAULT_NODE_FILTER,
                                newResource, LabelType.name, coreConfig, false);

                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "creation failed for node '" + resource.getPath() + "/" + name + "'");
                    }

                } catch (ParameterValidationException pvex) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, pvex.getMessage());
                }

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "can't determine parent node '" + resource.getPath() + "'");
            }
        }

        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getFormParameters(request);
        }
    }

    protected class PutCreateOperation extends CreateOperation {

        @Override
        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getJsonObject(request, NodeParameters.class);
        }
    }

    /**
     * The 'create' via POST (multipart form) implementation expects:
     * <ul>
     * <li>the 'path' parameter with the path of the new nodes parent</li>
     * <li>the 'name' parameter with the name of the new node</li>
     * <li>the 'file' part (form element / parameter) with the binary content (optional)</li>
     * </ul>
     */
    protected class CopyOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Node node = resource.adaptTo(Node.class);

            if (node != null) {
                Session session = node.getSession();
                ResourceResolver resolver = resource.getResourceResolver();
                NodeParameters params = getNodeParameters(request);
                String path = params.path;
                Node templateNode;

                if (StringUtils.isNotBlank(path) && (templateNode = session.getNode(path)) != null) {

                    String nodePath = node.getPath() + "/"
                            + (StringUtils.isNotBlank(params.name) ? params.name : templateNode.getName());
                    String newNodePath = nodePath;
                    try {
                        for (int i = 1; session.getNode(newNodePath) != null; i++) {
                            newNodePath = nodePath + i;
                        }
                    } catch (PathNotFoundException pnfex) {
                    }

                    Workspace workspace = session.getWorkspace();
                    workspace.copy(path, newNodePath);
                    session.save();

                    ResourceHandle newResource = ResourceHandle.use(resolver.getResource(newNodePath));
                    JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                    writeJsonNode(jsonWriter, MappingRules.DEFAULT_NODE_FILTER,
                            newResource, LabelType.name, coreConfig, false);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "can't determine template node '" + path + "'");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "can't determine parent node '" + resource.getPath() + "'");
            }
        }

        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getFormParameters(request);
        }
    }

    protected class PutCopyOperation extends CopyOperation {

        @Override
        public NodeParameters getNodeParameters(SlingHttpServletRequest request)
                throws IOException {
            return getJsonObject(request, NodeParameters.class);
        }
    }

    protected class DeleteOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);

                Session session = node.getSession();

                node.remove();

                session.save();

                // answer 'OK' (200)
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    //
    // raw mapping
    //

    protected class MapGetOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                MappingRules rules = new MappingRules(MappingRules.getDefaultMappingRules(),
                        null, null, null, new MappingRules.PropertyFormat(
                        RequestUtil.getParameter(request, "format",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Scope.value)),
                        RequestUtil.getParameter(request, "binary",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Binary.base64))),
                        RequestUtil.getParameter(request, "depth",
                                RequestUtil.getIntSelector(request, 0)),
                        null);

                int indent = RequestUtil.getParameter(request, "indent", 0);
                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                if (indent > 0) {
                    jsonWriter.setIndent(StringUtils.repeat(' ', indent));
                }

                if (RequestUtil.checkSelector(request, "download")) {
                    response.setContentType(FILE_CONTENT_TYPE);
                    String filename = MimeTypeUtil.getFilename(resource, null);
                    if (filename.endsWith(".bin")) {
                        filename = filename.substring(0, filename.length() - 4);
                    }
                    if (!filename.endsWith(FILE_NAME_EXT)) {
                        filename += FILE_NAME_EXT;
                    }
                    if (StringUtils.isNotBlank(filename)) {
                        response.setHeader("Content-Disposition", "inline; filename=" + filename);
                    }
                }

                response.setStatus(HttpServletResponse.SC_OK);

                JsonUtil.exportJson(jsonWriter, resource, rules);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    // receiving JSON ...

    protected class MapPostOperation extends MapPutOperation {

        @Override
        protected String getPath(SlingHttpServletRequest request) throws IOException {
            String path = AbstractServiceServlet.getPath(request);
            String name = RequestUtil.getParameter(request, "name", "");
            if (StringUtils.isNotBlank(name)) {
                path += "/" + name;
            }
            return path;
        }

        @Override
        protected Reader getReader(SlingHttpServletRequest request) throws IOException {

            RequestParameterMap parameters = request.getRequestParameterMap();
            RequestParameter file = parameters.getValue(PARAM_FILE);

            Reader reader = null;
            if (file != null) {
                InputStream input = file.getInputStream();
                reader = new BufferedReader(
                        new InputStreamReader(input, MappingRules.CHARSET.name()));
            }
            return reader;
        }
    }

    protected class MapPutOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            request.setCharacterEncoding(MappingRules.CHARSET.name());
            Reader reader = getReader(request);

            if (reader != null) {
                try {
                    String path = getPath(request);
                    LOG.info(path + ": update PUT with JSON data...");

                    ResourceResolver resolver = request.getResourceResolver();
                    JsonReader jsonReader = new JsonReader(reader);
                    Resource newResource = JsonUtil.importJson(jsonReader, resolver, path);

                    Session session = resolver.adaptTo(Session.class);
                    session.save();

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentLength(0);

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no data found");
            }
        }

        protected String getPath(SlingHttpServletRequest request) throws IOException {
            return AbstractServiceServlet.getPath(request);
        }

        protected Reader getReader(SlingHttpServletRequest request) throws IOException {
            return request.getReader();
        }
    }

    //
    // Scripting operations
    //

    protected class StartGroovyNodeOperation extends ScriptOperation {

        @Override
        protected void doScript(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                ResourceHandle resource, String key, PrintWriter writer)
                throws ServletException, IOException {
            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                GroovyService.JobState status = groovyService.startScript(key, session, resource.getPath(), writer);
                switch (status) {
                    case initialized:
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        break;
                    case error:
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        break;
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, rex.getMessage());
            }
        }
    }

    protected class CheckScriptOperation extends ScriptOperation {

        @Override
        protected void doScript(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                ResourceHandle resource, String key, PrintWriter writer)
                throws ServletException, IOException {
            GroovyService.JobState status = groovyService.checkScript(key, writer);
            switch (status) {
                case finished:
                case aborted:
                    response.setStatus(HttpServletResponse.SC_RESET_CONTENT);
                    break;
                case error:
                    response.setStatus(HttpServletResponse.SC_GONE);
                    break;
            }
        }
    }

    protected class StopScriptOperation extends ScriptOperation {

        @Override
        protected void doScript(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                ResourceHandle resource, String key, PrintWriter writer)
                throws ServletException, IOException {
            GroovyService.JobState status = groovyService.stopScript(key, writer);
            switch (status) {
                case starting:
                case running:
                case error:
                    response.setStatus(HttpServletResponse.SC_GONE);
                    break;
            }
        }
    }

    protected abstract class ScriptOperation implements ServletOperation {

        protected abstract void doScript(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                         ResourceHandle resource, String key, PrintWriter writer)
                throws ServletException, IOException;

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            PrintWriter writer = new PrintWriter(response.getOutputStream());
            if (groovyService != null) {
                String key = resource.getPath();
                doScript(request, response, resource, key, writer);
            } else {
                writer.println("no scripting service available");
            }
            writer.flush();
        }
    }

    //
    // JSON helpers
    //

    public static void writeJsonNode(JsonWriter writer, ResourceFilter filter,
                                     ResourceHandle resource, LabelType labelType,
                                     CoreConfiguration coreConfig, boolean isVirtual)
            throws IOException {
        writer.beginObject();
        String type = writeNodeIdentifiers(writer, resource, labelType, isVirtual);
        writeNodeTreeType(writer, filter, resource, isVirtual);
        writeNodeJcrState(writer, resource);
        List<Resource> children = new ArrayList<>();
        boolean hasChildren = false;
        for (Resource child : resource.getChildren()) {
            hasChildren = true;
            if (filter.accept(child)) {
                children.add(child);
            }
        }
        if (!hasChildren) {
            if (!isVirtual) {
                addVirtualContent(writer, filter, resource, labelType, coreConfig);
            }
        } else {
            if (!coreConfig.getOrderableNodesFilter().accept(resource)) {
                Collections.sort(children, new Comparator<Resource>() {
                    @Override
                    public int compare(Resource r1, Resource r2) {
                        return getSortName(r1).compareTo(getSortName(r2));
                    }
                });
            }
            writer.name("children").beginArray();
            for (Resource child : children) {
                ResourceHandle handle = ResourceHandle.use(child);
                writer.beginObject();
                writeNodeIdentifiers(writer, ResourceHandle.use(child), labelType, isVirtual);
                writeNodeTreeType(writer, filter, handle, isVirtual);
                writeNodeJcrState(writer, handle);
                writer.name("state").beginObject(); // that's the 'jstree' state object
                writer.name("loaded").value(false);
                writer.endObject();
                writer.endObject();
            }
            writer.endArray();
        }
        writer.endObject();
    }

    public static void writeNodeTreeType(JsonWriter writer, ResourceFilter filter,
                                         ResourceHandle resource, boolean isVirtual)
            throws IOException {
        String treeType = isVirtual ? "virtual" : "";
        if (StringUtils.isBlank(treeType) &&
                filter instanceof ResourceFilter.FilterSet &&
                ((ResourceFilter.FilterSet) filter).isIntermediate(resource)) {
            treeType = "intermediate";
        }
        if (StringUtils.isNotBlank(treeType)) {
            writer.name("treeType").value(treeType);
        }
    }

    public static void addVirtualContent(JsonWriter writer, ResourceFilter filter,
                                         ResourceHandle resource, LabelType labelType,
                                         CoreConfiguration coreConfig)
            throws IOException {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                Property property = node.getProperty(ResourceUtil.CONTENT_NODE);
                if (property != null && PropertyType.REFERENCE == property.getType()) {
                    ResourceResolver resolver = resource.getResourceResolver();
                    String reference = property.getString();
                    Session session = node.getSession();
                    Node targetNode;
                    Resource targetResource;
                    if (StringUtils.isNotBlank(reference) &&
                            (targetNode = session.getNodeByIdentifier(reference)) != null &&
                            (targetResource = resolver.getResource(targetNode.getPath() +
                                    "/" + ResourceUtil.CONTENT_NODE)) != null) {
                        writer.name("children").beginArray();
                        writeJsonNode(writer, filter, ResourceHandle.use(targetResource),
                                labelType, coreConfig, true);
                        writer.endArray();
                    }
                }
            } catch (RepositoryException ex) {
            }
        }
    }

    public static String getSortName(Resource resource) {
        String name = resource.getName().toLowerCase();
        if (name.startsWith("rep:")) {
            name = "a" + name;
        } else if (name.startsWith("jcr:")) {
            name = "b" + name;
        } else {
            name = "x" + name;
        }
        return name;
    }

    public static String getNodeLabel(ResourceHandle resource, LabelType labelType) {
        String text = resource.getName();
        if (labelType == LabelType.title) {
            String title = resource.getProperty(ResourceUtil.PROP_TITLE, "");
            if (StringUtils.isBlank(title)) {
                Resource contentResource = resource.getContentResource();
                if (contentResource != null) {
                    title = ResourceHandle.use(contentResource).getProperty(ResourceUtil.PROP_TITLE, "");
                }
            }
            if (StringUtils.isNotBlank(title)) {
                text = title;
            }
        }
        if (StringUtils.isBlank(text) && "/".equals(resource.getPath())) {
            text = "jcr:root";
        }
        return text;
    }

    public static String writeNodeIdentifiers(JsonWriter writer, ResourceHandle resource,
                                              LabelType labelType, boolean isVirtual)
            throws IOException {
        String text = getNodeLabel(resource, labelType);
        String type = getTypeKey(resource);
        String contentType = getContentTypeKey(resource, null);
        if (StringUtils.isNotBlank(contentType)) {
            writer.name("contentType").value(contentType);
            if ("designer".equals(contentType)) {
                type += "-" + contentType;
            }
        }
        if (StringUtils.isNotBlank(type)) {
            writer.name("type").value(type);
        }
        String path = resource.getPath();
        writer.name("id").value(path + (isVirtual ? "-v" : ""));
        writer.name("name").value(resource.getName());
        writer.name("text").value(text);
        writer.name("path").value(path);
        String uuid = resource.getProperty(ResourceUtil.PROP_UUID);
        if (StringUtils.isNotBlank(uuid)) {
            writer.name("uuid").value(uuid);
        }
        return type;
    }

    public static void writeNodeJcrState(JsonWriter writer,
                                         ResourceHandle resource)
            throws IOException {
        writer.name("jcrState").beginObject();
        try {
            Node node = resource.adaptTo(Node.class);
            if (node != null) {
                try {
                    String nodePath = node.getPath();
                    Session session = node.getSession();
                    Workspace workspace = session.getWorkspace();
                    LockManager lockManager = workspace.getLockManager();
                    JsonUtil.writeValue(writer, "checkedOut", node.isCheckedOut());
                    JsonUtil.writeValue(writer, "isVersionable", isVersionable(node));
                    boolean isLocked = node.isLocked();
                    JsonUtil.writeValue(writer, "locked", isLocked);
                    if (isLocked) {
                        writer.name("lock").beginObject();
                        try {
                            Lock lock = lockManager.getLock(nodePath);
                            String holderPath = lock.getNode().getPath();
                            JsonUtil.writeValue(writer, "isDeep", lock.isDeep());
                            JsonUtil.writeValue(writer, "isHolder", holderPath.equals(nodePath));
                            JsonUtil.writeValue(writer, "node", holderPath);
                            JsonUtil.writeValue(writer, "owner", lock.getLockOwner());
                            JsonUtil.writeValue(writer, "sessionScoped", lock.isSessionScoped());
                        } finally {
                            writer.endObject();
                        }
                    }
                } catch (RepositoryException rex) {
                    LOG.error(rex.getMessage(), rex);
                }
            }
        } finally {
            writer.endObject();
        }
    }

    private static boolean isVersionable(Node node) throws RepositoryException {
        final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
        for (final NodeType mixinNodeType : mixinNodeTypes) {
            if (mixinNodeType.isNodeType(NodeType.MIX_VERSIONABLE) || mixinNodeType.isNodeType(NodeType.MIX_SIMPLE_VERSIONABLE)) {
                return true;
            }
        }
        return false;
    }
    // receiving JSON ...

    /**
     * the structure for parsing property values from JSON using Gson
     */
    public static class NodeParameters {

        public String type;
        public String path;
        public String name;
        public String title;
        public String mimeType;
        public String resourceType;
        public String jcrContent;
    }

    public static NodeParameters getFormParameters(SlingHttpServletRequest request) {
        // copy parameters from request
        NodeParameters params = new NodeParameters();
        params.name = request.getParameter(PARAM_NAME);
        params.path = request.getParameter(PARAM_PATH);
        params.type = request.getParameter(PARAM_TYPE);
        params.title = request.getParameter(PARAM_TITLE);
        params.mimeType = request.getParameter(PARAM_MIME_TYPE);
        params.resourceType = request.getParameter(PARAM_RESOURCE_TYPE);
        params.jcrContent = request.getParameter(PARAM_JCR_CONTENT);
        return params;
    }

    //
    // node type key generation
    //

    public static String getTypeKey(ResourceHandle resource) {
        String type = getPrimaryTypeKey(resource);
        if ("file".equals(type)) {
            type = getFileTypeKey(resource, "file-");
        } else if ("resource".equals(type)) {
            type = getMimeTypeKey(resource, "resource-");
        } else if (StringUtils.isBlank(type) || "unstructured".equals(type)) {
            type = getResourceTypeKey(resource, "resource-");
        }
        return type;
    }

    public static String getContentTypeKey(ResourceHandle resource, String prefix) {
        String contentType = null;
        if (!ResourceUtil.CONTENT_NODE.equals(resource.getName())) {
            resource = ResourceHandle.use(resource.getChild(ResourceUtil.CONTENT_NODE));
        }
        if (resource.isValid()) {
            contentType = getResourceTypeKey(resource, prefix);
        }
        return contentType;
    }

    public static String getPrimaryTypeKey(ResourceHandle resource) {
        String primaryType = resource.getPrimaryType();
        String type = primaryType;
        if (StringUtils.isNotBlank(type)) {
            int namespace = type.lastIndexOf(':');
            if (namespace >= 0) {
                type = type.substring(namespace + 1);
            }
            type = type.toLowerCase();
        }
        return type;
    }

    public static String getResourceTypeKey(ResourceHandle resource, String prefix) {
        String primaryType = resource.getPrimaryType();
        String type = null;
        if (resource.isValid()) {
            String resourceType = resource.getResourceType();
            if (StringUtils.isNotBlank(resourceType) && !resourceType.equals(primaryType)) {
                int namespace = resourceType.lastIndexOf(':');
                if (namespace >= 0) {
                    resourceType = resourceType.substring(namespace + 1);
                }
                int dot = resourceType.lastIndexOf('.');
                if (dot >= 0) {
                    resourceType = resourceType.substring(dot + 1);
                }
                type = resourceType.substring(resourceType.lastIndexOf('/') + 1);
                type = type.toLowerCase();
            }
        }
        if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(prefix)) {
            type = prefix + type;
        }
        return type;
    }

    public static String getFileTypeKey(ResourceHandle resource, String prefix) {
        String type = null;
        ResourceHandle content = ResourceHandle.use(resource.getChild(ResourceUtil.CONTENT_NODE));
        if (content.isValid()) {
            type = getMimeTypeKey(content, prefix);
        }
        return type;
    }

    public static String getMimeTypeKey(ResourceHandle resource, String prefix) {
        String type = null;
        String mimeType = MimeTypeUtil.getMimeType(resource, "");
        if (StringUtils.isNotBlank(mimeType)) {
            int delim = mimeType.indexOf('/');
            String major = mimeType.substring(0, delim);
            String minor = mimeType.substring(delim + 1);
            type = major;
            if ("text".equals(major)) {
                type += "-" + minor;
            } else if ("application".equals(major)) {
                type = minor;
            }
            type = type.toLowerCase();
        }
        if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(prefix)) {
            type = prefix + type;
        }
        return type;
    }
}
