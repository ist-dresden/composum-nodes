package com.composum.sling.nodes.servlet;

import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.config.FilterConfiguration;
import com.composum.sling.core.exception.ParameterValidationException;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.resource.SyntheticQueryResult;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.NodeTreeServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.cpnl.CpnlElFunctions;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.mount.ExtendedResolver;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.tika.mime.MimeType;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.core.mapping.MappingRules.CHARSET;

/**
 * The JCR nodes service servlet to walk though and modify the entire hierarchy.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Node Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + NodeServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_DELETE,
                "sling.auth.requirements=" + NodeServlet.SERVLET_PATH
        }
)
public class NodeServlet extends NodeTreeServlet {

    private static final Logger LOG = LoggerFactory.getLogger(NodeServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/nodes/node";

    public static final String SCRIPT_STATUS_HEADER = "Script-Status";

    public static final String FILE_CONTENT_TYPE = "application/binary";
    public static final String FILE_NAME_EXT = ".json";

    /**
     * the names of the default filters configured statically
     */
    public static final String KEY_DEFAULT = "default";
    public static final String KEY_PAGE = "page";
    public static final String KEY_REFERENCEABLE = "referenceable";
    public static final String KEY_UNFILTERD = "unfiltered";

    public static final String ILLEGAL_NAME_CHARS = "<>&\"";
    public static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[^/" + ILLEGAL_NAME_CHARS + "]+$");
    public static final Pattern NODE_PATH_PATTERN = Pattern.compile("^(/[^/" + ILLEGAL_NAME_CHARS + "]+)+$");

    @Reference
    protected NodesConfiguration nodesConfig;

    protected final Map<String, ResourceFilter> nodeFilters = new LinkedHashMap<>();

    /**
     * injection of the filter configurations provided by the OSGi configuration
     */
    protected final List<FilterConfiguration> filterConfigurations = new ArrayList<>();

    /**
     * for each configured filter in the OSGi configuration
     * a tree filter is added to the filter set
     *
     * @param config the OSGi filter configuration object
     */
    @Reference(service = FilterConfiguration.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected synchronized void bindFilterConfiguration(final FilterConfiguration config) {
        filterConfigurations.add(config);
        if (nodesConfig != null) { // initialize on bind if activated already
            String key = config.getName();
            ResourceFilter filter = config.getFilter();
            if (StringUtils.isNotBlank(key) && filter != null) {
                nodeFilters.put(key, buildTreeFilter(filter));
            }
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

    @Activate
    protected synchronized void activate() {
        // initialize configurartions bound before activation
        for (FilterConfiguration config : filterConfigurations) {
            String key = config.getName();
            ResourceFilter filter = config.getFilter();
            if (StringUtils.isNotBlank(key) && filter != null) {
                nodeFilters.put(key, buildTreeFilter(filter));
            }
        }
    }

    @Deactivate
    protected synchronized void deactivate() {
        nodeFilters.clear();
        filterConfigurations.clear();
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
                        nodesConfig.getDefaultNodeFilter()),
                nodesConfig.getTreeIntermediateFilter());
    }

    /**
     * Determines the filter to use for node retrieval; scans the request for filter parameter or selector.
     */
    @Override
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
            filter = nodesConfig.getDefaultNodeFilter();
        }
        return filter;
    }

    protected boolean checkNodeName(@Nullable final String name) {
        return name != null && NODE_NAME_PATTERN.matcher(name).matches();
    }

    protected boolean checkNodePath(@Nullable final String path) {
        return path != null && NODE_PATH_PATTERN.matcher(path).matches();
    }

    //
    // Servlet operations
    //

    public enum Extension {json, html, lock, groovy, bin}

    public enum Operation {
        create, copy, move, reorder, delete, toggle,
        tree, reference, mixins, resolve, typeahead,
        query, filters, map, load, download, fileUpdate
    }

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return nodesConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // filter configuration
        nodeFilters.put(KEY_REFERENCEABLE, buildTreeFilter(nodesConfig.getReferenceableNodesFilter()));
        nodeFilters.put(KEY_UNFILTERD, ResourceFilter.ALL);
        nodeFilters.put(KEY_PAGE, buildTreeFilter(nodesConfig.getPageNodeFilter()));
        nodeFilters.put(KEY_DEFAULT, nodesConfig.getDefaultNodeFilter());

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.map, new MapGetOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.tree, new TreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.reference, new ReferenceOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.mixins, new GetMixinsOperation());
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
        operations.setOperation(ServletOperationSet.Method.GET, Extension.bin,
                Operation.load, new LoadBinaryOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.bin,
                Operation.download, new DownloadBinaryOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.map, new MapPostOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.create, new CreateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.bin,
                Operation.query, new ExportQueryOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.copy, new CopyOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.move, new MoveOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.lock,
                Operation.toggle, new ToggleLockOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.fileUpdate, new UpdateFileOperation());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.map, new MapPutOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.create, new PutCreateOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.copy, new PutCopyOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.move, new PutMoveOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.delete, new DeleteOperation());
    }

    //
    // access to the current filter configuration
    //

    protected class ListFiltersAsHtml implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html;charset=" + CHARSET); // XSS? - checked (2019-05-04)
            Writer writer = response.getWriter();

            for (String key : nodeFilters.keySet()) {
                writer.append("<li data-filter=\"").append(key)
                        .append("\"><a href=\"#\">").append(key).append("</a></li>");
            }
        }
    }

    protected class ListFiltersAsJson implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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

    /**
     * the 'some words' query input pattern string
     */
    public static final String WORDS = "[^/( \\[*]+( +[^/ (\\[]+)*";

    /**
     * the pattern to check for a XPATH query and their simplified variation
     */
    public static final Pattern XPATH_QUERY = Pattern.compile(
            "^(?<path>(/jcr:root)?/[^ (\\[*]*)( +(?<words>" + WORDS + ")*|(.*))$"
    );

    /**
     * the pattern to check for a XPATH query and their simplified variation
     */
    public static final Pattern SQL2_QUERY = Pattern.compile(
            "(?i)^\\s?select(\\s+(?<words>" + WORDS + ")\\s*|(.*))$"
    );

    protected abstract class AbstractQueryOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            String queryString = RequestUtil.getParameter(request, PARAM_QUERY, "").trim();
            @SuppressWarnings("deprecation") String queryLang = Query.XPATH;

            String text;
            // check for a X-PATH rule (starts with a path)
            Matcher matcher = XPATH_QUERY.matcher(queryString);
            if (matcher.matches()) {
                // check for a single word after the path pattern - use it as simple text pattern
                text = matcher.group("words");
                if (StringUtils.isNotBlank(text)) {
                    // use simple text separated from path
                    queryString = getSimpleQuery(matcher.group("path"), text);
                }
                // add the 'jcr:root' if not present
                if (!queryString.startsWith("/jcr:root")) {
                    queryString = "/jcr:root" + queryString;
                }
            } else {
                matcher = SQL2_QUERY.matcher(queryString);
                if (matcher.matches()) {
                    // SQL-2...
                    queryLang = Query.JCR_SQL2;
                } else {
                    // simple text query relative to the path from the URL (suffix or parameter)
                    queryString = getSimpleQuery(getPath(request), queryString);
                }
            }

            if (StringUtils.isNotBlank(queryString)) {
                try {
                    ResourceResolver resolver = request.getResourceResolver();
                    Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
                    Workspace workspace = session.getWorkspace();
                    QueryManager queryManager = workspace.getQueryManager();

                    Query query = queryManager.createQuery(queryString, queryLang);
                    Long queryLimit = getQueryLimit();
                    if (queryLimit != null) {
                        query.setLimit(queryLimit);
                    }
                    QueryResult result = query.execute();

                    ResourceFilter filter = getNodeFilter(request);
                    writeQueryResult(request, response, queryString, result, filter, resolver);

                } catch (RepositoryException rex) {
                    LOG.error(rex.getMessage(), rex);
                    writeError(response, queryString, rex);
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

        @Nullable
        protected Long getQueryLimit() {
            return nodesConfig.getQueryResultLimit() + 1;
        }

        protected abstract void writeQueryResult(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                                 String queryString, QueryResult result,
                                                 ResourceFilter filter, ResourceResolver resolver)
                throws RepositoryException, ServletException, IOException;

        protected void writeError(SlingHttpServletResponse response, String queryString, Exception ex)
                throws IOException {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "query: '" + CpnlElFunctions.text(queryString) + "' (" + ex.getMessage() + ")"); // XSS? - checked (2019-05-04)
        }
    }

    protected class JsonQueryOperation extends AbstractQueryOperation {

        @Override
        protected void writeQueryResult(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                        String queryString, QueryResult result,
                                        ResourceFilter filter, ResourceResolver resolver)
                throws RepositoryException, IOException {

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            response.setStatus(HttpServletResponse.SC_OK);

            TreeNodeStrategy nodeStrategy = new DefaultTreeNodeStrategy(getNodeFilter(request));
            NodeIterator iterator = result.getNodes();

            writer.beginArray();

            writer.name("result").beginArray();
            int count = 0;
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                ResourceHandle resource = ResourceHandle.use(resolver.getResource(node.getPath()));
                if (resource.isValid() && accept(filter, resource)) {
                    writer.beginObject();
                    String type = writeNodeIdentifiers(writer, nodeStrategy, resource, LabelType.name, false);
                    writeNodeJcrState(writer, resource);
                    writer.endObject();
                    count++;
                }
            }
            writer.endArray();

            writer.name("summary").beginObject();
            writer.name("query").value(CpnlElFunctions.script(queryString)); // XSS? - checked (2019-05-04)
            writer.name("count").value(count);
            writer.name("limit").value(nodesConfig.getQueryResultLimit());
            writer.endObject();

            writer.endObject();
        }
    }

    protected class HtmlQueryOperation extends AbstractQueryOperation {

        @Override
        protected void writeQueryResult(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                        String queryString, QueryResult result,
                                        ResourceFilter filter, ResourceResolver resolver)
                throws RepositoryException, IOException {

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html;charset=" + CHARSET); // XSS? - checked (2019-05-04)
            PrintWriter writer = response.getWriter();

            TreeNodeStrategy nodeStrategy = new DefaultTreeNodeStrategy(getNodeFilter(request));
            NodeIterator iterator = result.getNodes();

            writer.append("<tbody>");
            try {
                long limit = nodesConfig.getQueryResultLimit();
                int count = 0;
                while (count < limit && iterator.hasNext()) {
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
                        writer.append("<td class=\"icon\" data-type=\"").append(nodeStrategy.getTypeKey(resource))
                                .append("\"><span></span></td>");
                        writer.append("<td class=\"id\">").append(resource.getId()).append("</td>");
                        writer.append("<td class=\"name\">")
                                .append(CpnlElFunctions.text(resource.getName())).append("</td>");
                        writer.append("<td class=\"text\">")
                                .append(CpnlElFunctions.text(getNodeLabel(resource, LabelType.title)))
                                .append("</td>");
                        writer.append("<td class=\"path\"><a href=\"")
                                .append(CpnlElFunctions.path(path)).append("\">")
                                .append(CpnlElFunctions.text(path)).append("</a></td>");
                        writer.append("<td class=\"type\">").append(resource.getPrimaryType()).append("</td>");
                        writer.append("</tr>");
                        count++;
                    }
                }
                StringBuilder message = new StringBuilder();
                message.append(count).append(" items found");
                if (count >= limit) {
                    message.append(" (current limit: ").append(limit).append(", ")
                            .append(iterator.hasNext() ? "more items present" : "no more items").append(")");
                }
                message.append(".");
                writeSummary(queryString, message.toString(), writer, "summary info");
            } catch (RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                writeSummary(queryString, ex.getMessage(), writer, "error danger");
            }
            writer.append("</tbody>");
        }

        protected void writeSummary(String queryString, String message,
                                    PrintWriter writer, String cssClasses) {
            writer.append("<tr class=\"").append(cssClasses).append("\">");
            writer.append("<td class=\"icon\" data-type=\"summary\" rowspan=\"2\"><span></span></td>");
            writer.append("<td class=\"message\" colspan=\"5\">").append(CpnlElFunctions.text(message))
                    .append("<br/>query: '").append(CpnlElFunctions.text(queryString)).append("'</td>"); // XSS! (2019-05-04)
            writer.append("</tr>");
        }

        @Override
        protected void writeError(SlingHttpServletResponse response, String queryString, Exception ex)
                throws IOException {
            PrintWriter writer = response.getWriter();
            writer.append("<tbody>");
            writeSummary(queryString, ex.getMessage(), writer, "error danger");
            writer.append("</tbody>");
        }
    }

    protected class ExportQueryOperation extends AbstractQueryOperation {

        @Override
        @Nullable
        protected Long getQueryLimit() {
            return null;
        }

        @Override
        protected void writeQueryResult(@Nonnull final SlingHttpServletRequest request,
                                        @Nonnull final SlingHttpServletResponse response,
                                        @Nonnull final String queryString, @Nonnull final QueryResult result,
                                        @Nonnull final ResourceFilter filter, @Nonnull final ResourceResolver resolver)
                throws ServletException, IOException {

            String rendererType = XSS.filter(request.getParameter("export"));

            String syntheticPath = "/libs/composum/nodes/browser/query/export";
            SyntheticQueryResult resultResource = new SyntheticQueryResult(resolver, syntheticPath, result, filter, rendererType);
            resultResource.putValue("query", queryString);

            Enumeration<?> parameters = request.getParameterNames();
            while (parameters.hasMoreElements()) {
                String name = (String) parameters.nextElement();
                String value = XSS.filter(request.getParameter(name));
                if (StringUtils.isNotEmpty(value)) {
                    switch (name) {
                        case "properties":
                            resultResource.putValue(name, StringUtils.split(value, ','));
                            break;
                        default:
                            resultResource.putValue(name, value);
                            break;
                    }
                }
            }

            RequestDispatcherOptions options = new RequestDispatcherOptions();
            options.setForceResourceType(rendererType);

            RequestDispatcher dispatcher = request.getRequestDispatcher(resultResource, options);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
            }
        }
    }

    //
    // node retrieval
    //

    /**
     * sort children of orderable nodes
     */
    @Override
    protected List<Resource> prepareTreeItems(ResourceHandle resource, List<Resource> items) {
        if (!nodesConfig.getOrderableNodesFilter().accept(resource)) {
            items.sort(Comparator.comparing(this::getSortName));
        }
        return items;
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
     * - http://host/bin/cpm/nodes/node.reference.json/node-id
     * - http://host/bin/cpm/nodes/node.reference.title.json/node-id
     * - http://host/bin/cpm/nodes/node.reference.json?id=node-id
     */
    protected class ReferenceOperation implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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
                    Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
                    Node node = session.getNodeByIdentifier(reference);

                    Resource nodeResource;
                    if (node != null &&
                            (nodeResource = resolver.getResource(node.getPath())) != null) {

                        resource = ResourceHandle.use(nodeResource);

                        TreeNodeStrategy strategy = new DefaultTreeNodeStrategy(getNodeFilter(request));
                        LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                                RequestUtil.getSelector(request, LabelType.name));

                        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                        response.setStatus(HttpServletResponse.SC_OK);
                        writeJsonNode(jsonWriter, strategy, resource, labelType, false);

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
        @SuppressWarnings("Duplicates")
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            ResourceResolver resolver = request.getResourceResolver();
            String paramUrl = RequestUtil.getParameter(request, PARAM_URL, "");

            if (StringUtils.isNotBlank(paramUrl)) {

                URL url = new URL(paramUrl);
                String path = url.getPath();
                Resource target = resolver.resolve(request, path);

                if (ResourceUtil.isNonExistingResource(target)) {
                    String contextPath = request.getContextPath();
                    if (path.startsWith(contextPath)) {
                        path = path.substring(contextPath.length());
                        target = resolver.resolve(request, path);
                    }
                }

                if (!ResourceUtil.isNonExistingResource(target)) {
                    ResourceHandle handle = ResourceHandle.use(target);

                    TreeNodeStrategy strategy = new DefaultTreeNodeStrategy(getNodeFilter(request));
                    LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                            RequestUtil.getSelector(request, LabelType.name));

                    JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                    response.setStatus(HttpServletResponse.SC_OK);
                    writeJsonNode(jsonWriter, strategy, handle, labelType, false);

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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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

    protected class GetMixinsOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                response.setStatus(HttpServletResponse.SC_OK);
                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                jsonWriter.beginArray();

                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    NodeType[] mixins = node.getMixinNodeTypes();
                    if (mixins != null) {
                        for (NodeType type : mixins) {
                            jsonWriter.value(type.getName());
                        }
                    }
                } else {
                    ValueMap values = resource.getValueMap();
                    for (String mixin : values.get(JcrConstants.JCR_MIXINTYPES, new String[0])) {
                        jsonWriter.value(mixin);
                    }
                }
                jsonWriter.endArray();

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected class LoadBinaryOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            Binary binary = ResourceUtil.getBinaryData(resource);
            if (binary != null) {

                try {
                    prepareResponse(response, resource);

                    response.setContentLength((int) binary.getSize());
                    response.setStatus(HttpServletResponse.SC_OK);

                    try (InputStream input = binary.getStream();
                         BufferedInputStream buffered = new BufferedInputStream(input)) {
                        IOUtils.copy(buffered, response.getOutputStream());
                    }

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                } finally {
                    binary.dispose();
                }

            } else {

                FileHandle fileHandle = new FileHandle(resource);
                if (fileHandle.isValid()) {

                    prepareResponse(response, resource);
                    response.setStatus(HttpServletResponse.SC_OK);

                    try (InputStream input = fileHandle.getStream();
                         BufferedInputStream buffered = new BufferedInputStream(input)) {
                        IOUtils.copy(buffered, response.getOutputStream());
                    }

                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }

        protected void prepareResponse(SlingHttpServletResponse response, ResourceHandle resource) {
            MimeType mimeType = MimeTypeUtil.getMimeType(resource);
            if (mimeType != null) {
                response.setContentType(mimeType.toString());
            }
        }
    }

    protected class DownloadBinaryOperation extends LoadBinaryOperation {

        @Override
        @SuppressWarnings("Duplicates")
        protected void prepareResponse(@Nonnull final SlingHttpServletResponse response,
                                       @Nonnull final ResourceHandle resource) {
            super.prepareResponse(response, resource);

            String filename = MimeTypeUtil.getFilename(resource, null);
            if (StringUtils.isNotBlank(filename)) {
                response.setHeader("Content-Disposition", "inline; filename=" + filename);
            }

            Calendar lastModified = resource.getProperty(ResourceUtil.PROP_LAST_MODIFIED, Calendar.class);
            if (lastModified != null) {
                response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
            }
        }
    }

    //
    // Change Operations
    //

    /**
     * The 'fileUpdate' via POST (multipart form) implementation expects:
     * <ul>
     * <li>the 'path' parameter with the path of the new nodes parent</li>
     * <li>the 'file' part (form element / parameter) with the binary content (optional)</li>
     * </ul>
     */
    protected class UpdateFileOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Resource content = resource;

            // use resource as content if name is always 'jcr:content' else use child 'jcr:content'
            if (resource != null && resource.isValid() && (JcrConstants.JCR_CONTENT.equals(content.getName()) ||
                    (content = resource.getChild(JcrConstants.JCR_CONTENT)) != null)) {

                RequestParameterMap parameters = request.getRequestParameterMap();
                RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);

                InputStream input;
                if (file != null && (input = file.getInputStream()) != null) {

                    ModifiableValueMap values = content.adaptTo(ModifiableValueMap.class);

                    if (values != null) {
                        ResourceResolver resolver = resource.getResourceResolver();

                        if (resolver instanceof ExtendedResolver) {

                            ((ExtendedResolver) resolver).upload(resource.getPath(), input,
                                    file.getFileName(), file.getContentType(), null);

                        } else {

                            values.put(JcrConstants.JCR_DATA, input);

                            if (RequestUtil.getParameter(request, "adjustLastModified", Boolean.FALSE)) {
                                values.put(JcrConstants.JCR_LASTMODIFIED, GregorianCalendar.getInstance());
                                values.put(JcrConstants.JCR_LASTMODIFIED + "By", resolver.getUserID());
                            }
                        }

                        resolver.commit();

                        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                        writeJsonNode(jsonWriter, MappingRules.DEFAULT_TREE_NODE_STRATEGY, resource, LabelType.name, false);

                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "can't modify '" + resource.getPath() + "'");
                    }

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no file content found");
                }

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "no valid file resource '" + (resource != null ? resource.getPath() : "NULL") + "'");
            }
        }

        public NodeParameters getNodeParameters(SlingHttpServletRequest request) {
            return getFormParameters(request);
        }
    }

    protected class ToggleLockOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            resource = AbstractServiceServlet.tryToUseRawSuffix(request, resource);

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            ResourceResolver resolver = resource.getResourceResolver();
            NodeParameters params = getNodeParameters(request);
            String name = params.name;
            if (StringUtils.isBlank(name)) {
                name = resource.getName();
            }
            String newPath = params.path;
            if (!newPath.endsWith("/")) {
                newPath += "/";
            }

            newPath += name;
            if (checkNodePath(newPath)) {

                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    Session session = node.getSession();

                    String oldPath = node.getPath();
                    Node oldParentNode = node.getParent();
                    Node newParentNode = session.getNode(params.path);

                    String beforeName = null;
                    if (StringUtils.isNotBlank(params.before)) {
                        beforeName = params.before;
                        if (NODE_PATH_PATTERN.matcher(beforeName).matches()) {
                            beforeName = null;
                            try {
                                Node before = session.getNode(params.before);
                                beforeName = before.getName();
                            } catch (PathNotFoundException ex) {
                                LOG.error(ex.toString());
                            }
                        }
                    }
                    Integer index = null;
                    if (beforeName == null && (index = params.index()) != null && index >= 0) {
                        NodeIterator siblingsIterator = newParentNode.getNodes();
                        for (int i = 0; i < index && siblingsIterator.hasNext(); ) {
                            if (!siblingsIterator.nextNode().getPath().equals(oldPath)) {
                                i++; // skip the node itself by index count
                            }
                        }
                        if (siblingsIterator.hasNext()) {
                            beforeName = siblingsIterator.nextNode().getName();
                        }
                    }
                    if (beforeName == null && index == null && oldParentNode != null
                            && params.path.equals(oldParentNode.getPath())) {
                        // preserve position in case of a simple rename
                        NodeIterator siblingsIterator = oldParentNode.getNodes();
                        while (siblingsIterator.hasNext()) {
                            Node sibling = siblingsIterator.nextNode();
                            if (sibling.getPath().equals(oldPath)) {
                                if (siblingsIterator.hasNext()) {
                                    beforeName = siblingsIterator.nextNode().getName();
                                }
                                break;
                            }
                        }
                    }

                    boolean changesMade = false;
                    if (!oldPath.equals(newPath)) {
                        session.move(oldPath, newPath);
                        changesMade = true;
                    }

                    ResourceHandle newResource = ResourceHandle.use(resolver.getResource(newPath));

                    if (newResource.isValid()) {

                        String newName = newResource.getName();
                        if (!newName.equals(beforeName)) {
                            try {
                                newParentNode.orderBefore(newName, beforeName);
                                changesMade = true;
                            } catch (UnsupportedRepositoryOperationException ex) {
                                // ordering not supported... ignore it
                            }
                        }

                        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                        if (changesMade) {

                            session.save();
                            response.setStatus(HttpServletResponse.SC_OK);
                            writeJsonNode(jsonWriter, MappingRules.DEFAULT_TREE_NODE_STRATEGY,
                                    newResource, LabelType.name, false);

                        } else {

                            response.setStatus(HttpServletResponse.SC_ACCEPTED);
                            jsonWriter.beginObject();
                            jsonWriter.name("success").value(true);
                            jsonWriter.name("messages").beginArray();
                            jsonWriter.beginObject();
                            jsonWriter.name("level").value("info");
                            jsonWriter.name("text").value(I18N.get(request, "no modification"));
                            jsonWriter.endObject();
                            jsonWriter.endArray();
                            jsonWriter.endObject();
                        }

                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "invalid node after move '" + newPath + "'");
                    }
                } else {

                    String parentPath = StringUtils.substringBeforeLast(newPath, "/");

                    if (resolver instanceof ExtendedResolver) {

                        String order = null;
                        if (StringUtils.isNotBlank(params.before)) {
                            order = SlingPostConstants.ORDER_BEFORE
                                    + StringUtils.substringAfterLast(params.before, "/");
                        } else if (params.index() != null && params.index() >= 0) {
                            order = "" + params.index;
                        }

                        ((ExtendedResolver) resolver).move(resource.getPath(), parentPath, name, order);

                    } else {
                        resolver.move(resource.getPath(), parentPath);
                    }

                    resolver.commit();
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "invalid node path '" + newPath + "'");
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
     * The 'create' via POST (multipart form) implementation expects:
     * <ul>
     * <li>the 'path' parameter with the path of the new nodes parent</li>
     * <li>the 'name' parameter with the name of the new node</li>
     * <li>the 'file' part (form element / parameter) with the binary content (optional)</li>
     * </ul>
     */
    protected class CreateOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            ResourceResolver resolver = resource.getResourceResolver();

            NodeParameters params = getNodeParameters(request);
            String name = params.name;

            try {
                if (StringUtils.isBlank(params.type)) {
                    throw new ParameterValidationException("invalid node type '" + params.type + "'");
                }
                if (!checkNodeName(name)) {
                    throw new ParameterValidationException("invalid node name '" + name + "'");
                }

                ResourceHandle newResource = null;

                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    Node newNode = NodeFactory.SINGLETON.createNode(request, node, name, params);
                    if (newNode != null) {
                        Session session = node.getSession();
                        session.save();

                        newResource = ResourceHandle.use(resolver.getResource(newNode.getPath()));

                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "creation failed for node '" + resource.getPath() + "/" + name + "'");
                        return;
                    }

                } else {

                    if (resolver instanceof ExtendedResolver) {
                        if (JcrConstants.NT_FILE.equals(params.type)) {

                            RequestParameterMap parameters = request.getRequestParameterMap();
                            RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);

                            InputStream input;
                            if (file != null && (input = file.getInputStream()) != null) {

                                MimeType mimeType = MimeTypeUtil.getMimeType(
                                        StringUtils.isNotBlank(params.mimeType) ? params.mimeType : name);
                                String contentType = mimeType != null ? mimeType.getName() : null;

                                newResource = ResourceHandle.use(((ExtendedResolver) resolver).upload(
                                        resource.getPath() + "/" + name, input, null, contentType,
                                        contentType != null ? StandardCharsets.UTF_8.name() : null));
                            }
                        }
                    }

                    if (newResource == null) {
                        newResource = ResourceHandle.use(resolver.create(resource, name, params.asMap()));
                    }

                    resolver.commit();
                }

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                writeJsonNode(jsonWriter, MappingRules.DEFAULT_TREE_NODE_STRATEGY,
                        newResource, LabelType.name, false);

            } catch (ParameterValidationException pvex) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, pvex.getMessage());
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                NodeParameters params = getNodeParameters(request);
                String path = params.path;
                if (StringUtils.isNotBlank(path)) {
                    ResourceResolver resolver = resource.getResourceResolver();

                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        Session session = node.getSession();
                        Node templateNode = session.getNode(path);

                        if (templateNode != null) {
                            try {
                                String name = StringUtils.isNotBlank(params.name) ? params.name : templateNode.getName();
                                if (!checkNodeName(name)) {
                                    throw new ParameterValidationException("invalid node name '" + name + "'");
                                }
                                String newNodePath = node.getPath() + "/" + name;

                                Workspace workspace = session.getWorkspace();
                                workspace.copy(path, newNodePath);
                                session.save();

                                ResourceHandle newResource = ResourceHandle.use(resolver.getResource(newNodePath));
                                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                                writeJsonNode(jsonWriter, MappingRules.DEFAULT_TREE_NODE_STRATEGY,
                                        newResource, LabelType.name, false);

                            } catch (ItemExistsException itex) {
                                jsonAnswerItemExists(request, response);
                            }
                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                    "can't determine template node '" + path + "'");
                        }

                    } else {

                        Resource template = resolver.getResource(path);
                        if (template != null) {

                            resolver.copy(template.getPath(), resource.getPath());
                            resolver.commit();

                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                    "can't determine template node '" + path + "'");
                        }
                    }
                }
            } catch (ParameterValidationException pvex) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, pvex.getMessage());
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    Session session = node.getSession();
                    node.remove();
                    session.save();
                } else {
                    ResourceResolver resolver = resource.getResourceResolver();
                    resolver.delete(resource.getResource());
                    resolver.commit();
                }

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

    public static final Pattern MAP_DEPTH_SELECTOR = Pattern.compile("^d([\\d]+)$");
    public static final Pattern MAP_INDENT_SELECTOR = Pattern.compile("^i([\\d]+)$");

    public static MappingRules getJsonSelectorRules(SlingHttpServletRequest request) {
        boolean asSource = RequestUtil.checkSelector(request, "source");
        boolean embedTypes = !(asSource || RequestUtil.checkSelector(request, "notype"));
        ResourceFilter nodeFilter = null;
        if (asSource || RequestUtil.checkSelector(request, "nofile")) {
            nodeFilter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                    new ResourceFilter.PrimaryTypeFilter(new StringFilter.BlackList("^nt:(file|resource)$")),
                    MappingRules.MAPPING_NODE_FILTER);
        }
        return new MappingRules(MappingRules.getDefaultMappingRules(),
                nodeFilter,
                asSource ? MappingRules.SOURCE_EXPORT_FILTER : null, null,
                new MappingRules.PropertyFormat(
                        RequestUtil.getParameter(request, "format",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Scope.value)),
                        RequestUtil.getParameter(request, "binary",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Binary.base64)),
                        embedTypes),
                RequestUtil.getParameter(request, "depth",
                        RequestUtil.getIntSelector(request, MAP_DEPTH_SELECTOR,
                                RequestUtil.getIntSelector(request, 0))),
                null);
    }

    public static int getJsonSelectorIndent(SlingHttpServletRequest request) {
        return RequestUtil.getParameter(request, "indent",
                RequestUtil.getIntSelector(request, MAP_INDENT_SELECTOR, 0));
    }

    protected class MapGetOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                MappingRules rules = getJsonSelectorRules(request);
                int indent = getJsonSelectorIndent(request);

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

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                if (indent > 0) {
                    jsonWriter.setIndent(StringUtils.repeat(' ', indent));
                }
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
        protected String getPath(SlingHttpServletRequest request) {
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
            InputStream input;
            if (file != null && (input = file.getInputStream()) != null) {
                reader = new BufferedReader(
                        new InputStreamReader(input, CHARSET.name()));
            }
            return reader;
        }
    }

    protected class MapPutOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            request.setCharacterEncoding(CHARSET.name());
            Reader reader = getReader(request);

            if (reader != null) {
                try {
                    String path = getPath(request);
                    LOG.info(path + ": update PUT with JSON data...");

                    ResourceResolver resolver = request.getResourceResolver();
                    JsonReader jsonReader = new JsonReader(reader);
                    Resource newResource = JsonUtil.importJson(jsonReader, resolver, path);

                    Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
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

        protected String getPath(SlingHttpServletRequest request) {
            return AbstractServiceServlet.getPath(request);
        }

        protected Reader getReader(SlingHttpServletRequest request) throws IOException {
            return request.getReader();
        }
    }
}
