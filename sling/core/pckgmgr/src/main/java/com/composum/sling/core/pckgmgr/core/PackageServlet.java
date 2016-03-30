package com.composum.sling.core.pckgmgr.core;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.pckgmgr.util.PackageUtil;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

/** The servlet to provide download and upload of content packages and package definitions. */
@SlingServlet(paths = "/bin/core/package", methods = {"GET", "POST", "PUT", "DELETE"})
public class PackageServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PackageServlet.class);

    public static final String PARAM_GROUP = "group";
    public static final String PARAM_PACKAGE = "package";
    public static final String PARAM_FORCE = "force";

    public static final String ZIP_CONTENT_TYPE = "application/zip";

    public static final boolean AUTO_SAVE = true;

    // service references

    @Reference
    private CoreConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {
        html, json, zip, txt
    }

    public enum Operation {
        download, upload, create, build, install, delete, tree, view,
        filterList, filterChange, filterAdd, filterRemove, filterMoveUp, filterMoveDown,
        coverage
    }

    protected PackageOperationSet operations = new PackageOperationSet();

    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /** setup of the servlet operation set for this servlet instance */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.tree, new TreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.filterList, new ListFiltersOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.coverage, new CoverageOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.zip,
                Operation.download, new DownloadOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.create, new CreateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.upload, new UploadOperation());

        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.filterChange, new ChangeFilterOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.filterAdd, new AddFilterOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.filterRemove, new RemoveFilterOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.filterMoveUp, new MoveFilterOperation(true));
        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.filterMoveDown, new MoveFilterOperation(false));

        // PUT

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.create, new DeleteOperation());
    }

    public class PackageOperationSet extends ServletOperationSet<Extension, Operation> {

        public PackageOperationSet() {
            super(Extension.json);
        }

        @Override
        public ResourceHandle getResource(SlingHttpServletRequest request) {
            Resource resource = null;
            try {
                String path = PackageUtil.getPath(request);
                resource = PackageUtil.getResource(request, path);
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
            return ResourceHandle.use(resource);
        }
    }

    //
    // operation implementations
    //

    protected class TreeOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            PackageUtil.TreeNode treeNode = PackageUtil.getTreeNode(request);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            treeNode.sort();
            treeNode.toJson(writer);
        }
    }

    protected class CreateOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            String group = request.getParameter(PARAM_GROUP);
            String name = request.getParameter(PARAM_NAME);
            String version = request.getParameter(PARAM_VERSION);

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = manager.create(group, name, version);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            jsonAnswer(writer, "create", "successful", jcrPackage);
        }
    }

    protected class UpdateOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            /*
             * { "group":"", "name":"", "version":"", "filter":[{ "root":"", "rules:[{ "modifier":"include","pattern:"" },{ "modifier":"exclude","pattern:"" }]
             * },{ }], "providerName":"provider name", "providerUrl":"http://provider.url/", "providerLink":"http://provider.url/link/to/package.zip ",
             * "acHandling":"Overwrite" }
             */
        }
    }

    protected class DeleteOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            if (jcrPackage != null) {

                manager.remove(jcrPackage);

                JsonWriter writer = ResponseUtil.getJsonWriter(response);
                jsonAnswer(writer, "delete", "successful", jcrPackage);
            }
        }
    }

    protected class UploadOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            RequestParameterMap parameters = request.getRequestParameterMap();

            RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);
            if (file != null) {
                InputStream input = file.getInputStream();
                boolean force = RequestUtil.getParameter(request, PARAM_FORCE, false);

                JcrPackageManager manager = PackageUtil.createPackageManager(request);
                JcrPackage jcrPackage = manager.upload(input, true, force);

                JsonWriter writer = ResponseUtil.getJsonWriter(response);
                jsonAnswer(writer, "upload", "successful", jcrPackage);

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "no upload file accessible");
            }
        }
    }

    protected class DownloadOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            if (jcrPackage != null) {

                Property data;
                Binary binary;
                InputStream stream;
                if (jcrPackage != null &&
                        (data = jcrPackage.getData()) != null &&
                        (binary = data.getBinary()) != null &&
                        (stream = binary.getStream()) != null) {

                    PackageUtil.PackageItem item = new PackageUtil.PackageItem(jcrPackage);

                    response.setHeader("Content-Disposition", "inline; filename=" + item.getFilename());
                    Calendar lastModified = item.getLastModified();
                    if (lastModified != null) {
                        response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
                    }

                    response.setContentType(ZIP_CONTENT_TYPE);
                    OutputStream output = response.getOutputStream();
                    IOUtils.copy(stream, output);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            PackageUtil.getPath(request) + " is not a package or has no content");
                }

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        PackageUtil.getPath(request) + " can not be found in the repository");
            }
        }
    }

    protected class ListFiltersOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            Session session = RequestUtil.getSession(request);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();

            List<PathFilterSet> filters = PackageUtil.getFilterList(jcrPackage.getDefinition());
            for (PathFilterSet filter : filters) {
                writer.beginObject();
                writer.name("root").value(filter.getRoot());
                List<FilterSet.Entry<PathFilter>> filterRules = filter.getEntries();
                if (!filterRules.isEmpty()) {
                    writer.name("rules").beginArray();
                    for (FilterSet.Entry<PathFilter> entry : filterRules) {
                        writer.beginObject();
                        writer.name("type").value(entry.isInclude() ? "include" : "exclude");
                        writer.name("pattern").value(((DefaultPathFilter) entry.getFilter()).getPattern());
                        writer.endObject();
                    }
                    writer.endArray();
                }
                writer.endObject();
            }

            writer.endArray();
        }
    }

    protected class CoverageOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            Session session = RequestUtil.getSession(request);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();

            PackageUtil.JsonTracking pump = new PackageUtil.JsonTracking(writer);
            PackageUtil.getCoverage(jcrPackage.getDefinition(), session, pump);

            writer.endArray();
        }
    }

    // Package Filters

    protected class FilterRequest {

        public final SlingHttpServletRequest request;

        public final JcrPackageManager manager;
        public final JcrPackage jcrPackage;
        public final JcrPackageDefinition definition;

        public final MetaInf metaInf;
        public final WorkspaceFilter workspaceFilter;
        public final List<PathFilterSet> filters;

        public final int index;
        public final PathFilterSet filter;

        public FilterRequest(SlingHttpServletRequest request, Resource resource)
                throws RepositoryException {
            this.request = request;

            manager = PackageUtil.createPackageManager(request);
            jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            definition = jcrPackage.getDefinition();

            metaInf = definition.getMetaInf();
            workspaceFilter = metaInf.getFilter();
            filters = workspaceFilter.getFilterSets();

            index = RequestUtil.getParameter(request, "index", -1);

            String root = request.getParameter("root");
            if (StringUtils.isNotBlank(root)) {

                filter = new PathFilterSet(root);
                String[] ruleTypes = request.getParameterValues("ruleType");
                String[] ruleExpressions = request.getParameterValues("ruleExpression");

                if (ruleTypes != null && ruleExpressions != null && ruleTypes.length == ruleExpressions.length) {
                    for (int i = 0; i < ruleTypes.length; i++) {
                        if (StringUtils.isNotBlank(ruleExpressions[i])) {
                            switch (ruleTypes[i]) {
                                case "include":
                                    filter.addInclude(new DefaultPathFilter(ruleExpressions[i]));
                                    break;
                                case "exclude":
                                    filter.addExclude(new DefaultPathFilter(ruleExpressions[i]));
                                    break;
                            }
                        }
                    }
                }
            } else {
                filter = null;
            }
        }
    }

    protected class ChangeFilterOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            FilterRequest filterRequest = new FilterRequest(request, resource);
            if (filterRequest.filter != null) {
                int index = filterRequest.index;
                if (index >= 0 && index < filterRequest.filters.size()) {
                    filterRequest.filters.set(index, filterRequest.filter);
                    filterRequest.definition.setFilter(filterRequest.workspaceFilter, true);
                    PackageUtil.setLastModified(filterRequest.definition);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentLength(0);
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid filter index '" + index + "'");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid filter");
            }
        }
    }

    protected class AddFilterOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            FilterRequest filterRequest = new FilterRequest(request, resource);
            if (filterRequest.filter != null) {
                int index = filterRequest.index;
                if (index < 0 || index > filterRequest.filters.size()) {
                    index = filterRequest.filters.size();
                }
                filterRequest.filters.add(index, filterRequest.filter);
                filterRequest.definition.setFilter(filterRequest.workspaceFilter, true);
                PackageUtil.setLastModified(filterRequest.definition);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentLength(0);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid filter");
            }
        }
    }

    protected class RemoveFilterOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            FilterRequest filterRequest = new FilterRequest(request, resource);
            int index = filterRequest.index;
            if (index >= 0 && index < filterRequest.filters.size()) {
                filterRequest.filters.remove(index);
                filterRequest.definition.setFilter(filterRequest.workspaceFilter, true);
                PackageUtil.setLastModified(filterRequest.definition);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentLength(0);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid filter index '" + index + "'");
            }
        }
    }

    protected class MoveFilterOperation implements ServletOperation {

        public final boolean up;

        public MoveFilterOperation(boolean up) {
            super();
            this.up = up;
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            FilterRequest filterRequest = new FilterRequest(request, resource);
            int index = filterRequest.index;
            if (index >= 0 && index < filterRequest.filters.size()) {
                if (up) {
                    if (index > 0) {
                        move(filterRequest, index - 1);
                    }
                } else {
                    if (index < filterRequest.filters.size() - 1) {
                        move(filterRequest, index + 1);
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentLength(0);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid filter index '" + index + "'");
            }
        }

        protected void move(FilterRequest filterRequest, int newIndex) {
            PathFilterSet filter = filterRequest.filters.remove(filterRequest.index);
            filterRequest.filters.add(newIndex, filter);
            filterRequest.definition.setFilter(filterRequest.workspaceFilter, true);
        }
    }

    //
    // JSON mapping helpers
    //

    protected static void jsonAnswer(JsonWriter writer,
                                     String operation, String status, JcrPackage jcrPackage)
            throws IOException, RepositoryException {
        writer.beginObject();
        writer.name("operation").value(operation);
        writer.name("status").value(status);
        writer.name("package");
        PackageUtil.toJson(writer, jcrPackage, null);
        writer.endObject();
    }

    protected static void fromJson(JsonReader reader, JcrPackage jcrPackage)
            throws RepositoryException, IOException {
        reader.beginObject();
        JsonToken token;
        while (reader.hasNext() && (token = reader.peek()) == JsonToken.NAME) {
            String name = reader.nextName();
            switch (name) {
                case "definition":
                    fromJson(reader, jcrPackage.getDefinition());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected static void fromJson(JsonReader reader, JcrPackageDefinition definition)
            throws RepositoryException, IOException {
        reader.beginObject();
        JsonToken token;
        while (reader.hasNext() && (token = reader.peek()) == JsonToken.NAME) {
            String name = reader.nextName();
            switch (name) {
                case "filter":
                    DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
                    PathFilterSet pathFilterSet = new PathFilterSet();
                    filter.add(pathFilterSet);
                    break;
                default:
                    switch (reader.peek()) {
                        case STRING:
                            String strVal = reader.nextString();
                            definition.set(name, strVal, AUTO_SAVE);
                            break;
                        case BOOLEAN:
                            Boolean boolVal = reader.nextBoolean();
                            definition.set(name, boolVal, AUTO_SAVE);
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                    break;
            }
        }
        reader.endObject();
    }
}
