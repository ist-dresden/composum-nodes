package com.composum.sling.core.pckgmgr.jcrpckg;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.concurrent.JobFacade;
import com.composum.sling.core.concurrent.JobMonitor;
import com.composum.sling.core.concurrent.JobUtil;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.Packages.Mode;
import com.composum.sling.core.pckgmgr.jcrpckg.service.impl.PackageJobExecutor;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.JcrPackageItem;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeNode;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageProgressTracker;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryItem;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryTree;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.NoSuchPackageException;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageExistsException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The servlet to provide download and upload of content packages and package definitions.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Package Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + PackageServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_DELETE,
                "sling.auth.requirements=" + PackageServlet.SERVLET_PATH
        }
)
@Designate(ocd = PackageServlet.Configuration.class)
public class PackageServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PackageServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/package";
    public static final String PARAM_GROUP = "group";
    public static final String PARAM_FORCE = "force";
    /** The key (namespace) of the PackageRegistry, {@link PackageRegistries.Registries#getNamespaces()}.  */
    public static final String PARAM_REGISTRY = "registry";

    private volatile long jobIdleTimeout;

    public static final String ZIP_CONTENT_TYPE = "application/zip";

    public static final boolean AUTO_SAVE = true;

    // service references

    @Reference
    private NodesConfiguration nodesConfig;

    @Reference
    private JobManager jobManager;

    @Reference
    private Packaging packaging;

    @Reference
    private PackageRegistries packageRegistries;

    private BundleContext bundleContext;

    //
    // Servlet operations
    //

    public enum Extension {
        html, json, zip
    }

    public enum Operation {
        create, update, delete, download, upload, install, uninstall, deploy, service, list, tree, view, query,
        coverage, filterList, filterChange, filterAdd, filterRemove, filterMoveUp, filterMoveDown,
        cleanup,
        mode, registryTree, registries, registriesTree
    }

    protected PackageOperationSet operations = new PackageOperationSet();

    @Override
    protected PackageOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return nodesConfig.isEnabled(this);
    }

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext, Configuration configuration) {
        this.bundleContext = bundleContext;
        jobIdleTimeout = configuration.package_job_timeout();
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.list, new ListOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.tree, new TreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.query, new QueryOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.filterList, new ListFiltersOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.coverage, new CoverageOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.zip,
                Operation.download, new DownloadOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.download, new DownloadOperation());

        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.cleanup, new CleanupOperation());

        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.mode, new PackageManagerModeOperation());

        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.registryTree, new RegistryTreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.registries, new RegistriesOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.registriesTree, new RegistriesTreeOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.html,
                Operation.service, new ServiceOperation());

        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.create, new CreateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.update, new UpdateOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.upload, new UploadOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.install, new InstallOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.uninstall, new UninstallOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.deploy, new ServiceOperation());

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
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.update, new JsonUpdateOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.delete, new DeleteOperation());
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
                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                resource = PackageUtil.getResource(manager, request, path);
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
            return ResourceHandle.use(resource);
        }
    }

    protected class PackageManagerModeOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            Status status = new Status(request, response);
            Mode mode = Mode.jcrpckg;
            try {
                mode = Mode.valueOf(request.getParameter("mode"));
            } catch (Exception ex) {
                if (RequestUtil.checkSelector(request, Mode.regpckg.name())) {
                    mode = Mode.regpckg;
                } else if (!RequestUtil.checkSelector(request, Mode.jcrpckg.name())) {
                    RequestPathInfo pathInfo = request.getRequestPathInfo();
                    String suffix = pathInfo.getSuffix();
                    if (StringUtils.isNotBlank(suffix)) {
                        if (suffix.startsWith("/")) {
                            suffix = suffix.substring(1);
                            try {
                                mode = Mode.valueOf(suffix);
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(Packages.SA_PCKGMGR_MODE, mode.name());
            status.data("pckgmgr").put("mode", mode.name());
            status.sendJson();
        }
    }

    //
    // registry operation implementations
    //

    protected class RegistryTreeOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
            String path = PackageUtil.getPath(request);
            boolean merged = RequestUtil.checkSelector(request, "merged")
                    || !Packages.REGISTRY_BASED_PATH.matcher(path).matches() && !"/".equals(path);
            RegistryTree tree = new RegistryTree(merged);
            RegistryItem treeItem = tree.getItem(context, path);
            if (treeItem != null) {
                JsonWriter writer = ResponseUtil.getJsonWriter(response);
                if (!treeItem.isLoaded()) {
                    treeItem.load(context);
                }
                treeItem.toTree(writer, true, true);
            } else {
                new Status(request, response).setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    protected class RegistriesOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            PackageRegistries.Registries registries = packageRegistries.getRegistries(request.getResourceResolver());
            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();
            for (PackageRegistry registry : registries.iterable()) {
                writer.beginObject();
                writer.name("type").value(registry.getClass().getSimpleName());
                writer.name("packages").beginArray();
                for (PackageId pckgId : registry.packages()) {
                    writer.beginObject();
                    writer.name("id").value(pckgId.toString());
                    writer.name("filename").value(RegistryUtil.getFilename(pckgId));
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
            }
            writer.endArray();
        }
    }

    protected class RegistriesTreeOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            RegistryTree tree = new RegistryTree(false);
            toJson(writer, context, tree);
        }

        protected void toJson(JsonWriter writer, BeanContext context, RegistryItem item) throws IOException {
            item.load(context);
            writer.beginObject();
            JsonUtil.jsonMapEntries(writer, item);
            writer.name("items").beginArray();
            for (RegistryItem child : item.getItems()) {
                toJson(writer, context, child);
            }
            writer.endArray();
            writer.endObject();
        }
    }

    //
    // operation implementations
    //

    protected class ListOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            List<JcrPackage> jcrPackages = manager.listPackages();
            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();
            for (JcrPackage jcrPackage : jcrPackages) {
                new JcrPackageItem(jcrPackage).toJson(writer);
            }
            writer.endArray();
        }
    }

    protected class TreeOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            TreeNode treeNode = PackageUtil.getTreeNode(manager, request);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            treeNode.sort();
            treeNode.toJson(writer);
        }
    }

    protected class QueryOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            String suffix = XSS.filter(request.getRequestPathInfo().getSuffix());
            if (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }
            suffix = suffix.replaceAll("[ '\"]", "").trim();

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();

            if (suffix.length() > 1) {

                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);

                ResourceResolver resolver = request.getResourceResolver();
                String rootPath = manager.getPackageRoot().getPath();
                @SuppressWarnings("deprecation")
                Iterator<Resource> found = resolver.findResources("/jcr:root" + rootPath
                                + "//element(*,vlt:PackageDefinition)[jcr:contains(.,'" + suffix + "')]/../..",
                        Query.XPATH);

                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

                while (found.hasNext()) {
                    JcrPackage jcrPckg = PackageUtil.getJcrPackage(manager, found.next());
                    if (jcrPckg != null) {

                        JcrPackageDefinition pckgDef = jcrPckg.getDefinition();
                        if (pckgDef != null) {
                            String group = pckgDef.get(JcrPackageDefinition.PN_GROUP);
                            if (group != null && !group.endsWith("/.snapshot")) {
                                String name = pckgDef.get(JcrPackageDefinition.PN_NAME);
                                String version = pckgDef.get(JcrPackageDefinition.PN_VERSION);

                                writer.beginObject();
                                writer.name("state").beginObject();
                                writer.name("installed").value(jcrPckg.isInstalled() ? "on" : "off");
                                writer.name("sealed").value(jcrPckg.isSealed() ? "on" : "off");
                                writer.name("valid").value(jcrPckg.isValid() ? "on" : "off");
                                writer.endObject();
                                writer.name("group").value(group);
                                writer.name("name").value(name);
                                writer.name("version").value(version);
                                writer.name("lastModified").value(dateFormat.format(PackageUtil.getLastModified(jcrPckg).getTime()));
                                writer.name("path").value(Packages.getMode(request) == Mode.jcrpckg
                                        ? PackageUtil.getPackagePath(manager, jcrPckg)
                                        : RegistryUtil.toPath((String) null, new PackageId(group, name, version)));
                                writer.endObject();
                            }
                        }
                    }
                }
            }
            writer.endArray();
        }
    }

    protected class CreateOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            String group = XSS.filter(request.getParameter(PARAM_GROUP));
            String name = XSS.filter(request.getParameter(PARAM_NAME));
            String version = XSS.filter(request.getParameter(PARAM_VERSION));

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = manager.create(group, name, version);

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            jsonAnswer(writer, "create", "successful", manager, jcrPackage);
        }
    }

    // updating package properties

    protected class UpdateOperation implements ServletOperation {

        protected Map<String, Object> getParameters(SlingHttpServletRequest request) throws IOException {
            Map<String, Object> result = new HashMap<>();
            RequestParameterMap parameters = request.getRequestParameterMap();
            for (Map.Entry<String, RequestParameter[]> parameter : parameters.entrySet()) {
                String key = parameter.getKey();
                Object value;
                RequestParameter[] param = parameter.getValue();
                if (param.length > 1) {
                    String[] values = new String[param.length];
                    for (int i = 0; i < param.length; i++) {
                        values[i] = XSS.filter(param[i].getString());
                    }
                    value = values;
                } else {
                    value = param.length < 1 ? Boolean.TRUE : XSS.filter(param[0].getString());
                }
                result.put(key, value);
            }
            return result;
        }

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            try {

                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

                if (jcrPackage != null) {
                    JcrPackageDefinition pckgDef = jcrPackage.getDefinition();

                    String group = XSS.filter(request.getParameter(PARAM_GROUP));
                    String name = XSS.filter(request.getParameter(PARAM_NAME));
                    String version = XSS.filter(request.getParameter(PARAM_VERSION));

                    if (StringUtils.isNotBlank(name) &&
                            (!PackageUtil.isGroup(pckgDef, group) ||
                                    !PackageUtil.isName(pckgDef, name) ||
                                    !PackageUtil.isVersion(pckgDef, version))) {
                        manager.rename(jcrPackage, group, name, version);
                    }
                    Map<String, Object> parameters = getParameters(request);
                    parameters.put("includeVersions", parameters.containsKey("includeVersions"));
                    for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                        String key = parameter.getKey();
                        PackageUtil.DefinitionSetter<?> setter = PackageUtil.DEFINITION_SETTERS.get(key);
                        if (setter != null) {
                            Object value = parameter.getValue();
                            try {
                                setter.set(pckgDef, key, value, true);
                            } catch (ParseException ex) {
                                LOG.error("can't parse '" + key + "'='" + value + "' (" + ex.toString() + ")");
                            }
                        }
                    }

                    JsonWriter writer = ResponseUtil.getJsonWriter(response);
                    jsonAnswer(writer, "update", "successful", manager, jcrPackage);

                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Package not found: " + PackageUtil.getPath(request));
                }
            } catch (PackageException pex) {
                LOG.error(pex.getMessage(), pex);
                throw new RepositoryException(pex);
            }
        }
    }

    protected class JsonUpdateOperation extends UpdateOperation {

        @Override
        protected Map<String, Object> getParameters(SlingHttpServletRequest request) throws IOException {
            Map<String, Object> result = new HashMap<>();
            JsonReader reader = new JsonReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
            reader.setLenient(true);
            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                String key = reader.nextName();
                PackageUtil.DefinitionSetter<?> setter = PackageUtil.DEFINITION_SETTERS.get(key);
                if (setter != null) {
                    result.put(key, setter.get(reader));
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
            return result;
        }
    }

    protected class DeleteOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {
            String deletedPath = request.getRequestPathInfo().getSuffix();
            String namespace = RegistryUtil.namespace(deletedPath);
            if (StringUtils.isBlank(namespace)) {
                deleteJcrPackage(request, response, resource);
            } else {
                PackageId packageId = RegistryUtil.fromPath(deletedPath);
                PackageRegistries.Registries registries = packageRegistries.getRegistries(request.getResourceResolver());
                PackageRegistry registry = registries.getRegistry(namespace);
                JsonWriter writer = ResponseUtil.getJsonWriter(response);
                try {
                    registry.remove(packageId);
                    jsonAnswer(writer, "delete", "successful", namespace, packageId);
                } catch (NoSuchPackageException | ClassCastException e) {
                    // (probably bug in FileVault: if package deletion requested in JCR but is in FS -> ClassCastException.
                    LOG.warn("Registry {} : could not find requested package {}", namespace, packageId, e);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            packageId + " can not be found in the registry " + namespace);
                }

            }
        }

        private void deleteJcrPackage(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response, ResourceHandle resource) throws RepositoryException, IOException {
            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            if (jcrPackage != null) {
                manager.remove(jcrPackage);

                JsonWriter writer = ResponseUtil.getJsonWriter(response);
                jsonAnswer(writer, "delete", "successful", manager, jcrPackage);
            }
        }
    }

    /**
     * Enables downloading packages. If the path is a JCR path to a package, we deliver it from there, otherwise we try to
     * find it in the PackageRegistries.
     */
    protected class DownloadOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            boolean delivered;
            if (jcrPackage != null) {
                delivered = deliverJcrPackage(response, jcrPackage);
            } else {
                delivered = deliverRegistryPackage(request, response, manager);
            }

            if (!delivered) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        PackageUtil.getPath(request) + " can not be found in the repository");
            }

        }

        protected boolean deliverJcrPackage(@Nonnull SlingHttpServletResponse response, @Nonnull JcrPackage jcrPackage) throws RepositoryException, IOException {
            Property data;
            Binary binary = null;
            InputStream stream = null;
            boolean delivered = false;

            try {
                if ((data = jcrPackage.getData()) != null &&
                        (binary = data.getBinary()) != null &&
                        (stream = binary.getStream()) != null) {

                    JcrPackageItem item = new JcrPackageItem(jcrPackage);

                    response.setHeader("Content-Disposition", "inline; filename=" + item.getFilename());
                    Calendar lastModified = item.getLastModified();
                    if (lastModified != null) {
                        response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
                    }

                    response.setContentType(ZIP_CONTENT_TYPE);
                    if (jcrPackage.getSize() > 0) {
                        response.setContentLength((int) jcrPackage.getSize());
                    }
                    OutputStream output = response.getOutputStream();
                    IOUtils.copy(stream, output);
                    delivered = true;
                }
            } finally {
                if (stream != null) stream.close();
                if (binary != null) binary.dispose();
                jcrPackage.close();
            }

            return delivered;
        }

        protected boolean deliverRegistryPackage(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response, @Nonnull JcrPackageManager manager) throws IOException, RepositoryException {
            boolean delivered;
            PackageRegistries.Registries registries = packageRegistries.getRegistries(request.getResourceResolver());
            String path = RegistryUtil.requestPath(request);
            Pair<String, PackageId> location = registries.resolve(path);
            Pair<String, RegisteredPackage> pckgEntry = location != null ? registries.open(location.getRight()) : null;
            try (RegisteredPackage pckg = pckgEntry != null ? pckgEntry.getRight() : null;
                 VaultPackage vaultPckg = pckg != null ? pckg.getPackage() : null) {

                if (vaultPckg != null && vaultPckg.getFile() != null && vaultPckg.getFile().canRead()) {
                    File file = vaultPckg.getFile();

                    response.setHeader("Content-Disposition", "inline; filename=" + RegistryUtil.getFilename(location.getRight()));
                    response.setContentType(ZIP_CONTENT_TYPE);
                    Calendar lastModified = RegistryUtil.readPackagePropertyDate(vaultPckg.getProperties().getLastModified(), vaultPckg.getProperties().getProperty(PackageProperties.NAME_LAST_MODIFIED));
                    if (lastModified == null) {
                        lastModified = RegistryUtil.readPackagePropertyDate(vaultPckg.getProperties().getCreated(), vaultPckg.getProperties().getProperty(PackageProperties.NAME_CREATED));
                    }
                    if (lastModified != null) {
                        response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
                    }
                    response.setContentLength((int) file.length());
                    OutputStream output = response.getOutputStream();
                    try (FileInputStream stream = new FileInputStream(file)) {
                        IOUtils.copy(stream, output);
                    }
                    delivered = true;

                } else { // no file -> probably a Jcr package
                    String pathNoNs = RegistryUtil.pathWithoutNamespace(path);
                    JcrPackage jcrPackage = manager.open(pckg.getId());
                    if (jcrPackage != null) {
                        delivered = deliverJcrPackage(response, jcrPackage);
                    } else { // shouln't happen - unknown registry type? No idea what to do here.
                        LOG.warn("Bug: Could not download package at {}", path);
                        delivered = false;
                    }
                }
            }
            return delivered;
        }

    }

    protected class UploadOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            boolean force = RequestUtil.getParameter(request, PARAM_FORCE, false);
            String namespace = request.getParameter(PARAM_REGISTRY);
            RequestParameter file = request.getRequestParameter(AbstractServiceServlet.PARAM_FILE);
            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            try (InputStream input = file != null ? file.getInputStream() : null) {
                if (input != null) {
                    if (StringUtils.isBlank(namespace)) {
                        JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                        JcrPackage jcrPackage = manager.upload(input, force);

                        jsonAnswer(writer, "upload", "successful", manager, jcrPackage);
                    } else {
                        PackageRegistries.Registries registries = packageRegistries.getRegistries(request.getResourceResolver());
                        PackageRegistry registry = registries.getRegistry(namespace);
                        if (registry != null) {
                            PackageId packageId = registry.register(input, force);
                            jsonAnswer(writer, "upload", "successful", namespace, packageId);
                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown registry " + registry);
                        }
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "no package file accessible");
                }
            } catch (PackageExistsException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "package exists already: " + e.getId());
            }
        }
    }

    /**
     * an 'emergency' operation to remove blocking jobs
     */
    protected class CleanupOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            LOG.warn("package service cleanup...");
            JsonWriter writer = new JsonWriter(response.getWriter());
            writer.beginObject();

            writer.name("removedJobs").beginArray();
            //noinspection unchecked
            for (Job job : jobManager.findJobs(JobManager.QueryType.ALL, PackageJobExecutor.TOPIC, 0)) {
                String created = new SimpleDateFormat(DATE_FORMAT).format(job.getCreated().getTime());
                LOG.warn("package service cleanup: remove job: {}, {}, {}, '{}', {}, {}",
                        job.getId(), job.getTopic(), created, job.getResultMessage(),
                        job.getRetryCount(), job.getQueueName());
                writer.beginObject();
                writer.name("id").value(job.getId());
                writer.name("topic").value(job.getTopic());
                writer.name("created").value(created);
                writer.name("result").value(job.getResultMessage());
                writer.name("retryCount").value(job.getRetryCount());
                writer.name("queue").value(job.getQueueName());
                writer.endObject();
                jobManager.removeJobById(job.getId());
            }
            writer.endArray();

            LOG.warn("package service cleanup ends.");
        }
    }

    protected class InstallOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            installPackage(request, response, manager, jcrPackage);
        }

        @SuppressWarnings("Duplicates")
        protected void installPackage(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                      JcrPackageManager manager, JcrPackage jcrPackage)
                throws RepositoryException, IOException {

            ResourceResolver resolver = request.getResourceResolver();
            Session session = resolver.adaptTo(Session.class);
            Node node = jcrPackage.getNode();
            String path = node != null
                    ? jcrPackage.getNode().getPath()
                    : PackageUtil.getPackagePath(manager, jcrPackage);
            String root = manager.getPackageRoot().getPath();
            if (path.startsWith(root)) {
                path = path.substring(root.length());
            }

            Map<String, Object> jobProperties = new HashMap<>();
            jobProperties.put("reference", path);
            jobProperties.put("operation", "install");
            if (session != null) {
                jobProperties.put("userid", session.getUserID());
            }
            JobUtil.buildOutfileName(jobProperties);

            Job job = jobManager.addJob(PackageJobExecutor.TOPIC, jobProperties);
            final JobMonitor.IsDone isDone = new JobMonitor.IsDone(jobManager, resolver, job.getId(), jobIdleTimeout);
            if (isDone.call()) {

                installationDone(request, response, manager, jcrPackage, isDone);

            } else {
                LOG.error("Job was not yet executed: {}", job);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Package install not started!");
            }
        }

        protected void installationDone(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                        JcrPackageManager manager, JcrPackage jcrPackage, JobMonitor jobMonitor)
                throws RepositoryException, IOException {

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            jsonAnswer(writer, "installation", "done", manager, jcrPackage);
        }

    }

    protected class UninstallOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);

            uninstallPackage(request, response, manager, jcrPackage);
        }

        @SuppressWarnings("Duplicates")
        protected void uninstallPackage(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                        JcrPackageManager manager, JcrPackage jcrPackage)
                throws RepositoryException, IOException {

            ResourceResolver resolver = request.getResourceResolver();
            Session session = resolver.adaptTo(Session.class);
            Node node = jcrPackage.getNode();
            String path = node != null
                    ? jcrPackage.getNode().getPath()
                    : PackageUtil.getPackagePath(manager, jcrPackage);
            String root = manager.getPackageRoot().getPath();
            if (path.startsWith(root)) {
                path = path.substring(root.length());
            }

            Map<String, Object> jobProperties = new HashMap<>();
            jobProperties.put("reference", path);
            jobProperties.put("operation", "uninstall");
            if (session != null) {
                jobProperties.put("userid", session.getUserID());
            }
            JobUtil.buildOutfileName(jobProperties);

            Job job = jobManager.addJob(PackageJobExecutor.TOPIC, jobProperties);
            final JobMonitor.IsDone isDone = new JobMonitor.IsDone(jobManager, resolver, job.getId(), jobIdleTimeout);
            if (isDone.call()) {
                uninstallationDone(request, response, manager, jcrPackage, isDone);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Package uninstall not started!");
            }
        }

        protected void uninstallationDone(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                          JcrPackageManager manager, JcrPackage jcrPackage, JobMonitor jobMonitor)
                throws RepositoryException, IOException {

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            jsonAnswer(writer, "uninstallation", "done", manager, jcrPackage);
        }
    }

    /**
     * The 'service' implementation based on the behaviour of the 'service' servlet provided by the CRX Package Manager.
     * <dl>
     * <dt>targetURL</dt>
     * <dd>http://${sling.host}:${sling.port}${sling.context}/bin/cpm/package.service.html</dd>
     * </dl>
     */
    protected class ServiceOperation extends InstallOperation {

        abstract class ServiceCommand {

            abstract void doCommand(SlingHttpServletRequest request, SlingHttpServletResponse response, RequestParameterMap parameters) throws RepositoryException, IOException;

            String getGroup(RequestParameterMap parameters) throws UnsupportedEncodingException {
                final RequestParameter groupParameter = parameters.getValue("group");
                String group = null;
                if (groupParameter != null) {
                    group = groupParameter.getString("UTF-8");
                }
                return group;
            }

            String getName(RequestParameterMap parameters) throws UnsupportedEncodingException {
                final RequestParameter nameParameter = parameters.getValue("name");
                String name = "";
                if (nameParameter != null) {
                    name = nameParameter.getString("UTF-8");
                }
                return name;
            }

        }

        class LsCommand extends ServiceCommand {

            @Override
            void doCommand(SlingHttpServletRequest request, SlingHttpServletResponse response, RequestParameterMap parameters) throws RepositoryException, IOException {
                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                final List<JcrPackage> jcrPackages = manager.listPackages();
                response.setStatus(HttpServletResponse.SC_OK);
                try (Writer writer = response.getWriter()) {
                    writer.append("<repo>");
                    writer.append(createRequestElement("ls", "", ""));
                    writer.append("<response>");
                    writer.append("<data>");
                    writer.append("<packages>");
                    for (JcrPackage jcrPackage : jcrPackages) {
                        writer.append(PackageUtil.packageToXMLResponse(jcrPackage));
                    }
                    writer.append("</packages>");
                    writer.append("</data>");
                    writer.append(createStatusElement("200", "ok"));
                    writer.append("</response>");
                    writer.append("</repo>");
                }
            }
        }

        class RmCommand extends ServiceCommand {

            @Override
            void doCommand(SlingHttpServletRequest request, SlingHttpServletResponse response, RequestParameterMap parameters) throws RepositoryException, IOException {
                String name = getName(parameters);
                String group = getGroup(parameters);
                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                final List<JcrPackage> jcrPackages = manager.listPackages();
                boolean found = false;
                for (JcrPackage jcrPackage : jcrPackages) {
                    String packageName = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_NAME);
                    String packageGroup = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_GROUP);
                    if (!StringUtils.isBlank(packageName) && packageName.equals(name)) {
                        if (!StringUtils.isBlank(group) && group.equals(packageGroup)) {
                            manager.remove(jcrPackage);
                            found = true;
                            break;
                        } else if (StringUtils.isBlank(group) && StringUtils.isBlank(packageGroup)) {
                            manager.remove(jcrPackage);
                            found = true;
                            break;
                        }
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
                try (Writer writer = response.getWriter()) {
                    writer.append("<repo>");
                    writer.append(createRequestElement("rm", name, group));
                    if (found) {
                        writer.append(createResponseElement("200", "ok"));
                    } else {
                        writer.append(createResponseElement("500", "Package '" + group + ":" + name + "' does not exist."));
                    }
                    writer.append("</repo>");
                }
            }

        }

        abstract class BuildUninstCommand extends ServiceCommand {
            abstract String getCommand();

            abstract String getOperation();

            @Override
            @SuppressWarnings("Duplicates")
            void doCommand(SlingHttpServletRequest request, SlingHttpServletResponse response, RequestParameterMap parameters) throws RepositoryException, IOException {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                String name = getName(parameters);
                String group = getGroup(parameters);
                JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                final JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, group, name);
                if (jcrPackage != null) {
                    Node node = jcrPackage.getNode();
                    String path = node != null
                            ? jcrPackage.getNode().getPath()
                            : PackageUtil.getPackagePath(manager, jcrPackage);
                    String root = manager.getPackageRoot().getPath();
                    if (path.startsWith(root)) {
                        path = path.substring(root.length());
                    }

                    Map<String, Object> jobProperties = new HashMap<>();
                    jobProperties.put("reference", path);
                    jobProperties.put("operation", getOperation());
                    if (session != null) {
                        jobProperties.put("userid", session.getUserID());
                    }
                    JobUtil.buildOutfileName(jobProperties);
                    Job job = jobManager.addJob(PackageJobExecutor.TOPIC, jobProperties);
                    final JobMonitor.IsDone isDone = new JobMonitor.IsDone(jobManager, resolver, job.getId(), jobIdleTimeout);
                    if (isDone.call()) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        try (Writer writer = response.getWriter()) {
                            writer.append("<repo>");
                            writer.append(createRequestElement(getCommand(), name, group));
                            if (isDone.succeeded()) {
                                writer.append(createResponseElement("200", "ok"));
                            } else {
                                writer.append(createResponseElement("500", getOperation() + " does not succeed"));
                            }
                            writer.append("</repo>");
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_OK);
                        try (Writer writer = response.getWriter()) {
                            writer.append("<repo>");
                            writer.append(createRequestElement(getCommand(), name, group));
                            writer.append(createResponseElement("500", "nok"));
                            writer.append("</repo>");
                        }
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    try (Writer writer = response.getWriter()) {
                        writer.append("<repo>");
                        writer.append(createRequestElement(getCommand(), name, group));
                        writer.append(createResponseElement("500", "Package '" + group + ":" + name + "' does not exist"));
                        writer.append("</repo>");
                    }
                }
            }
        }

        class BuildCommand extends BuildUninstCommand {

            @Override
            String getCommand() {
                return "build";
            }

            @Override
            String getOperation() {
                return "assemble";
            }
        }

        class UninstCommand extends BuildUninstCommand {

            @Override
            String getCommand() {
                return "uninst";
            }

            @Override
            String getOperation() {
                return "uninstall";
            }

        }

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            RequestParameterMap parameters = request.getRequestParameterMap();
            final RequestParameter cmd = parameters.getValue(AbstractServiceServlet.PARAM_CMD);
            if (cmd != null && !StringUtils.isBlank(cmd.getString())) {
                switch (cmd.getString()) {
                    case "ls":
                        new LsCommand().doCommand(request, response, parameters);
                        break;
                    case "rm":
                        new RmCommand().doCommand(request, response, parameters);
                        break;
                    case "build":
                        new BuildCommand().doCommand(request, response, parameters);
                        break;
                    case "uninst":
                        new UninstCommand().doCommand(request, response, parameters);
                        break;
                    default:
                        LOG.warn("unsupported command '{}' received. will ignore it.", cmd);
                        break;
                }
            } else {
                RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);
                if (file != null) {
                    InputStream input = file.getInputStream();
                    boolean force = RequestUtil.getParameter(request, PARAM_FORCE, true);

                    JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
                    JcrPackage jcrPackage = manager.upload(input, force);

                    installPackage(request, response, manager, jcrPackage);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no package file accessible");
                }
            }
        }

        @Override
        protected void installationDone(@Nonnull final SlingHttpServletRequest request,
                                        @Nonnull final SlingHttpServletResponse response,
                                        JcrPackageManager manager, JcrPackage jcrPackage, JobMonitor jobMonitor)
                throws RepositoryException, IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            try (Writer writer = response.getWriter()) {
                writer.append("<repo>");
//                writer.append("<crx version=\"\" user=\"\" workspace=\"\">");
//                writer.append("<request>");
//                writer.append("<param name=\"file\" value=\"\"/>");
//                writer.append("</request>");
                writer.append("<response>");
                writer.append("<data>");
                writer.append(PackageUtil.packageToXMLResponse(jcrPackage));
                //writer.append("<log><![CDATA[...]]></log>");
                writer.append("</data>");
                if (jobMonitor.succeeded()) {
                    writer.append(createStatusElement("200", "ok"));
                } else {
                    JobFacade jobFacade = jobMonitor.getJob();
                    final String msg = jobFacade != null ? jobFacade.getResultMessage() : "no job found";
                    writer.append(createStatusElement("500", msg));
                }
                writer.append("</response>");
//                writer.append("</crx>");
                writer.append("</repo>");
            }
        }

        private String createRequestElement(String cmd, String name, String group) {
            return "<request>" +
                    createParameterElement("cmd", cmd) +
                    (StringUtils.isBlank(name) ? "" : createParameterElement("name", name)) +
                    (StringUtils.isBlank(group) ? "" : createParameterElement("group", group)) +
                    "</request>";
        }

        private String createParameterElement(String name, String value) {
            return "<param name=\"" + name + "\" value=\"" + value + "\"/>";
        }

        private String createResponseElement(String code, String message) {
            return "<response>" + createStatusElement(code, message) + "</response>";
        }

        private String createStatusElement(String code, String message) {
            return "<status code=\"" + code + "\">" + message + "</status>";
        }

    }

    protected class CoverageOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            Session session = RequestUtil.getSession(request);

            PackageProgressTracker tracker = new PackageProgressTracker.JsonTracking(response, null);
            tracker.writePrologue();
            PackageUtil.getCoverage(jcrPackage.getDefinition(), session, tracker);
            tracker.writeEpilogue();
        }
    }

    protected class ListFiltersOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            List<PathFilterSet> filters;
            JcrPackageManager manager = PackageUtil.getPackageManager(packaging, request);
            JcrPackage jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            if (jcrPackage != null) {
                filters = PackageUtil.getFilterList(jcrPackage.getDefinition());
                jcrPackage.close();
            } else { // try to find it in the registries
                PackageRegistries.Registries registries = packageRegistries.getRegistries(request.getResourceResolver());
                String path = RegistryUtil.requestPath(request);
                Pair<String, PackageId> location = registries.resolve(path);
                Pair<String, RegisteredPackage> pckgEntry = location != null ? registries.open(location.getRight()) : null;
                try (RegisteredPackage pckg = pckgEntry != null ? pckgEntry.getRight() : null) {
                    filters = pckg != null ? pckg.getWorkspaceFilter().getFilterSets() : Collections.emptyList();
                }
            }

            JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();

            for (PathFilterSet filter : filters) {
                writer.beginObject();
                writer.name("root").value(filter.getRoot());
                ImportMode importMode = filter.getImportMode();
                if (importMode != null) {
                    writer.name("importMode").value(importMode.name());
                }
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

            manager = PackageUtil.getPackageManager(packaging, request);
            jcrPackage = PackageUtil.getJcrPackage(manager, resource);
            definition = jcrPackage.getDefinition();

            metaInf = definition.getMetaInf();
            workspaceFilter = metaInf.getFilter();
            filters = workspaceFilter.getFilterSets();

            index = RequestUtil.getParameter(request, "index", -1);

            String root = XSS.filter(request.getParameter("root"));
            if (StringUtils.isNotBlank(root)) {

                filter = new PathFilterSet(root);
                String importMode = XSS.filter(request.getParameter("importMode"));
                if (StringUtils.isNotBlank(importMode)) {
                    ImportMode mode = ImportMode.valueOf(importMode.toUpperCase());
                    filter.setImportMode(mode);
                }
                String[] ruleTypes = XSS.filter(request.getParameterValues("ruleType"));
                String[] ruleExpressions = XSS.filter(request.getParameterValues("ruleExpression"));

                if (ruleTypes != null && ruleExpressions != null && ruleTypes.length == ruleExpressions.length) {
                    for (int i = 0; i < ruleTypes.length; i++) {
                        try {
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
                        } catch (Exception ex) { // ConfigurationException
                            LOG.error(ex.getMessage(), ex);
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
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
                                     String operation, String status,
                                     JcrPackageManager pckgMgr, JcrPackage jcrPackage)
            throws IOException, RepositoryException {
        writer.beginObject();
        writer.name("operation").value(operation);
        writer.name("status").value(status);
        writer.name("path").value(PackageUtil.getPackagePath(pckgMgr, jcrPackage));
        writer.name("package");
        PackageUtil.toJson(writer, jcrPackage, null);
        writer.endObject();
    }

    protected static void jsonAnswer(JsonWriter writer,
                                     String operation, String status, String namespace, PackageId packageId)
            throws IOException, RepositoryException {
        writer.beginObject();
        writer.name("operation").value(operation);
        writer.name("status").value(status);
        writer.name("path").value(RegistryUtil.toPath(namespace, packageId));
        writer.name("package");
        RegistryUtil.toJson(writer, namespace, packageId);
        writer.endObject();
    }

    protected static void fromJson(JsonReader reader, JcrPackage jcrPackage)
            throws RepositoryException, IOException {
        reader.beginObject();
        while (reader.hasNext() && (reader.peek()) == JsonToken.NAME) {
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
            throws IOException {
        reader.beginObject();
        while (reader.hasNext() && (reader.peek()) == JsonToken.NAME) {
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
                            boolean boolVal = reader.nextBoolean();
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

    @ObjectClassDefinition(name = "Composum Nodes Package Servlet")
    public @interface Configuration {

        @AttributeDefinition(name = "package job timeout", description =
                "Time in milliseconds a package job can be idle")
        long package_job_timeout() default 60L * 1000L;

    }

}
