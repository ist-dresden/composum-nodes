package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.PackageUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
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
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The servlet to provide download and upload of content packages and package definitions. */
@SlingServlet(paths = "/bin/core/package", methods = { "GET", "POST", "PUT", "DELETE" })
public class PackageServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PackageServlet.class);

    public static final String PARAM_GROUP = "group";
    public static final String PARAM_PACKAGE = "package";

    public static final String ZIP_CONTENT_TYPE = "application/zip";

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

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
        download, upload, create, build, install, delete, tree, view, coverage
    }

    protected PackageOperationSet operations = new PackageOperationSet(Extension.json);

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
                Operation.coverage, new CoverageOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.zip,
                Operation.download, new DownloadOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.create, new CreateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.upload, new UploadOperation());

        // PUT

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.create, new DeleteOperation());
    }

    public class PackageOperationSet extends ServletOperationSet {

        public PackageOperationSet(Enum defaultExtension) {
            super(defaultExtension);
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

            String path = PackageUtil.getPath(request);

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            List<JcrPackage> jcrPackages = manager.listPackages();

            TreeNode treeNode = new TreeNode(path);
            for (JcrPackage jcrPackage : jcrPackages) {
                if (treeNode.addPackage(jcrPackage)) {
                    break;
                }
            }

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

                JcrPackageManager manager = PackageUtil.createPackageManager(request);
                JcrPackage jcrPackage = manager.upload(input, true);

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

                    PackageItem item = new PackageItem(jcrPackage);

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

    protected class CoverageOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.createPackageManager(request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            Session session = RequestUtil.getSession(request);

            PackageUtil.TrackingBuffer buffer = PackageUtil.getCoverage(jcrPackage, session);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();
            for (PackageUtil.TrackingBuffer.Item item : buffer.getItems()) {
                writer.beginObject();
                writer.name("action").value(item.action);
                writer.name("value").value(item.path != null ? item.path : item.message);
                writer.name("error").value(item.error);
                writer.endObject();
            }
            writer.endArray();
        }
    }

    //
    // Tree Mapping of the flat Package list
    //

    public interface TreeItem {

        String getName();

        void toJson(JsonWriter writer) throws RepositoryException, IOException;
    }

    public static class FolderItem extends LinkedHashMap<String, Object> implements TreeItem {

        public FolderItem(String path, String name) {
            put("id", path);
            put("path", path);
            put("name", name);
            put("text", name);
            put("type", "/".equals(path) ? "root" : "folder");
            Map<String, Object> treeState = new LinkedHashMap<>();
            treeState.put("loaded", Boolean.FALSE);
            put("state", treeState);
        }

        @Override
        public String getName() {
            return (String) get("text");
        }

        @Override
        public void toJson(JsonWriter writer) throws IOException {
            JsonUtil.jsonMap(writer, this);
        }

        @Override
        public boolean equals(Object other) {
            return getName().equals(((TreeItem) other).getName());
        }
    }

    public static class PackageItem implements TreeItem {

        private final JcrPackage jcrPackage;
        private final JcrPackageDefinition definition;

        public PackageItem(JcrPackage jcrPackage) throws RepositoryException {
            this.jcrPackage = jcrPackage;
            definition = jcrPackage.getDefinition();
        }

        @Override
        public String getName() {
            return definition.get(JcrPackageDefinition.PN_NAME);
        }

        public JcrPackageDefinition getDefinition() {
            return definition;
        }

        @Override
        public void toJson(JsonWriter writer) throws RepositoryException, IOException {
            String name = getFilename();
            String path = "/" + definition.get(JcrPackageDefinition.PN_GROUP) + "/" + name;
            Map<String, Object> treeState = new LinkedHashMap<>();
            treeState.put("loaded", Boolean.TRUE);
            Map<String, Object> additionalAttributes = new LinkedHashMap<>();
            additionalAttributes.put("id", path);
            additionalAttributes.put("path", path);
            additionalAttributes.put("name", name);
            additionalAttributes.put("text", name);
            additionalAttributes.put("type", "package");
            additionalAttributes.put("state", treeState);
            additionalAttributes.put("file", getFilename());
            PackageServlet.toJson(writer, jcrPackage, additionalAttributes);
        }

        public String getFilename() {
            return PackageUtil.getFilename(jcrPackage);
        }

        public Calendar getLastModified() {
            Calendar lastModified = PackageUtil.getLastModified(jcrPackage);
            if (lastModified != null) {
                return lastModified;
            }
            return PackageUtil.getCreated(jcrPackage);
        }

        @Override
        public boolean equals(Object other) {
            return getName().equals(((TreeItem) other).getName());
        }
    }

    /** the tree node implementation for the requested path (folder or package) */
    protected static class TreeNode extends ArrayList<TreeItem> {

        private final String path;
        private boolean isLeaf = false;

        public TreeNode(String path) {
            this.path = path;
        }

        /** adds a package or the appropriate folder to the nodes children if it is a child of this node
         *
         * @param jcrPackage the current package in the iteration
         * @return true, if this package is the nodes target and a leaf - iteration can be stopped
         * @throws RepositoryException */
        public boolean addPackage(JcrPackage jcrPackage) throws RepositoryException {
            String groupPath = path.endsWith("/") ? path : path + "/";
            JcrPackageDefinition definition = jcrPackage.getDefinition();
            String group = "/" + definition.get(JcrPackageDefinition.PN_GROUP) + "/";
            if (group.startsWith(groupPath)) {
                TreeItem item;
                if (group.equals(groupPath)) {
                    // this node the the packages parent - use the package as node child
                    item = new PackageItem(jcrPackage);
                } else {
                    // this node is a group parent - insert a folder for the subgroup
                    String name = group.substring(path.length());
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    int nextDelimiter = name.indexOf("/");
                    if (nextDelimiter > 0) {
                        name = name.substring(0, nextDelimiter);
                    }
                    item = new FolderItem(groupPath + name, name);
                }
                if (!contains(item)) {
                    add(item);
                }
                return false;
            } else {
                PackageItem item = new PackageItem(jcrPackage);
                if (path.equals(group + item.getFilename())) {
                    // this node (teh path) represents the package itself and is a leaf
                    isLeaf = true;
                    add(item);
                    // we can stop the iteration
                    return true;
                }
                return false;
            }
        }

        public boolean isLeaf() {
            return isLeaf;
        }

        public void sort() {
            Collections.sort(this, new Comparator<TreeItem>() {

                @Override
                public int compare(TreeItem o1, TreeItem o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }

        public void toJson(JsonWriter writer) throws IOException, RepositoryException {
            if (isLeaf()) {
                get(0).toJson(writer);
            } else {
                int lastPathSegment = path.lastIndexOf("/");
                String name = path.substring(lastPathSegment + 1);
                if (StringUtils.isBlank(name)) {
                    name = "packages ";
                }
                FolderItem myself = new FolderItem(path, name);

                writer.beginObject();
                JsonUtil.jsonMapEntries(writer, myself);
                writer.name("children");
                writer.beginArray();
                for (TreeItem item : this) {
                    item.toJson(writer);
                }
                writer.endArray();
                writer.endObject();
            }
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
        toJson(writer, jcrPackage, null);
        writer.endObject();
    }

    protected static void toJson(JsonWriter writer, JcrPackage jcrPackage,
            Map<String, Object> additionalAttributes)
                    throws RepositoryException, IOException {
        writer.beginObject();
        Node node = jcrPackage.getNode();
        writer.name("definition");
        toJson(writer, jcrPackage.getDefinition());
        JsonUtil.jsonMapEntries(writer, additionalAttributes);
        writer.endObject();
    }

    protected static void toJson(JsonWriter writer, JcrPackageDefinition definition)
            throws RepositoryException, IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String version = definition.get(JcrPackageDefinition.PN_VERSION);
        String description = definition.get(JcrPackageDefinition.PN_DESCRIPTION);
        Calendar lastModified = definition.getCalendar(JcrPackageDefinition.PN_LASTMODIFIED);
        writer.beginObject();
        writer.name(JcrPackageDefinition.PN_GROUP).value(definition.get(JcrPackageDefinition.PN_GROUP));
        writer.name(JcrPackageDefinition.PN_NAME).value(definition.get(JcrPackageDefinition.PN_NAME));
        if (version != null) {
            writer.name(JcrPackageDefinition.PN_VERSION).value(version);
        }
        if (description != null) {
            writer.name(JcrPackageDefinition.PN_DESCRIPTION).value(description);
        }
        if (lastModified != null) {
            writer.name(JcrPackageDefinition.PN_LASTMODIFIED).value(dateFormat.format(lastModified.getTime()));
        }
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
