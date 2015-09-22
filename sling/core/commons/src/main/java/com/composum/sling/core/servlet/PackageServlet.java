package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.mapping.Package;
import com.composum.sling.core.mapping.PackageManager;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The servlet to provide download and upload of content packages and package definitions.
 * <p/>
 * Examples for testing using 'curl':
 * - download package via POST with a 'package.json' package definition file as form parameter
 * curl -u admin:admin --form package=@package.json http://localhost:4502/bin/core/package.download.zip >test.zip
 * - upload a package definition file 'package.json' into the repository
 * curl -u admin:admin --form package=@package.json http://localhost:4502/bin/core/package.json
 * - download a package via GET using a package definition resource in the repository
 * curl -u admin:admin http://localhost:4502/bin/core/package.zip/test/group/test-1.1.5-SNAPSHOT >test.zip
 * - install a package file 'test.zip' via PUT
 * curl -u admin:admin -X PUT --data-binary "@test.zip" http://localhost:4502/bin/core/package.zip >test.log
 */
@SlingServlet(
        paths = "/bin/core/package",
        methods = {"GET", "POST", "PUT", "DELETE"}
)
public class PackageServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PackageServlet.class);

    public static final String PARAM_GROUP = "group";
    public static final String PARAM_PACKAGE = "package";

    public static final String ZIP_CONTENT_TYPE = "application/zip";

    // service references

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private PackageManager packageManager;

    //
    // Servlet operations
    //

    public enum Extension {html, json, zip, txt}

    public enum Operation {download, upload, setup, delete, tree, view, map}

    protected PackagesOperationSet operations = new PackagesOperationSet();

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

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.view, new ViewPackageOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.tree, new PackageTreeOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.zip,
                Operation.download, new DownloadOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.setup, new SetupOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.delete, new DeleteOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.zip,
                Operation.upload, new UploadOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.zip,
                Operation.download, new DownloadOperation());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.setup, new PutSetupOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.zip,
                Operation.upload, new PutUploadOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.delete, new DeleteOperation());
    }

    /**
     * extended resource retrieval in the PackageManagers context
     */
    protected class PackagesOperationSet extends ServletOperationSet<Extension, Operation> {

        public PackagesOperationSet() {
            super(Extension.zip);
        }

        /**
         * extended resource retrieval in the PackageManagers context
         */
        protected ResourceHandle getResource(SlingHttpServletRequest request) {

            ResourceResolver resolver = request.getResourceResolver();
            RequestPathInfo requestPathInfo = request.getRequestPathInfo();
            String suffix = requestPathInfo.getSuffix();

            Resource resource = packageManager.getResource(suffix, resolver);

            return resource != null ? ResourceHandle.use(resource) : super.getResource(request);
        }
    }

    //
    // operation implementations
    //

    protected class DownloadOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            ResourceResolver resolver = request.getResourceResolver();
            if (!resource.isValid()) {
                resource = ResourceHandle.use(
                        packageManager.retrievePackage(request.getRequestPathInfo().getSuffix(), resolver));
            }

            Package requestedPackage = null;

            RequestParameterMap parameters = request.getRequestParameterMap();

            RequestParameter file = parameters.getValue(PARAM_PACKAGE);
            if (file != null) {
                InputStream input = file.getInputStream();
                InputStreamReader reader = new InputStreamReader(input, MappingRules.CHARSET);
                JsonReader jsonReader = new JsonReader(reader);
                requestedPackage = Package.GSON.fromJson(jsonReader, Package.class);
            }

            response.setContentType(ZIP_CONTENT_TYPE);
            response.setStatus(HttpServletResponse.SC_OK);

            if (requestedPackage != null) {
                packageManager.exportPackage(response.getOutputStream(), requestedPackage, resolver);
            } else {
                // if the resource is a package definition than the resource is used to control the package generation
                packageManager.exportPackage(response.getOutputStream(), resource);
            }
        }
    }

    /**
     * the 'setup' declares a package object and stores it in the repository
     */
    protected class SetupOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            Package pckg = getPackage(request);

            if (pckg != null) {
                ResourceResolver resolver = request.getResourceResolver();

                packageManager.setupPackage(pckg, resolver);

                Session session = resolver.adaptTo(Session.class);
                session.save();

                response.setContentLength(0);
                response.setStatus(HttpServletResponse.SC_OK);

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "no valid package definition found");
            }
        }

        protected Package getPackage(SlingHttpServletRequest request) throws IOException {

            Package pckg = null;
            RequestParameterMap parameters = request.getRequestParameterMap();
            RequestParameter file = parameters.getValue(PARAM_PACKAGE);

            if (file != null) {

                InputStream input = file.getInputStream();
                InputStreamReader reader = new InputStreamReader(input, MappingRules.CHARSET);
                JsonReader jsonReader = new JsonReader(reader);

                pckg = Package.GSON.fromJson(jsonReader, Package.class);

            } else {

                String name = RequestUtil.getParameter(parameters, PARAM_NAME, "");
                String group = RequestUtil.getParameter(parameters, PARAM_GROUP, "");
                String version = RequestUtil.getParameter(parameters, PARAM_VERSION, "1.0-SNAPSHOT");

                if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version)) {

                    RequestParameter[] pathParams = parameters.getValues(PARAM_PATH);
                    RequestParameter[] filterParams = parameters.getValues(PARAM_FILTER);

                    List<Package.PackagePath> pathList = new ArrayList<>();
                    for (int i = 0; i < pathParams.length; i++) {
                        String path = pathParams[i].getString();
                        if (StringUtils.isNotBlank(path)) {
                            pathList.add(new Package.PackagePath(path,
                                    ResourceFilterMapping.fromString(filterParams[i].getString())));
                        }
                    }

                    pckg = new Package(group, name, version, new Package.PackageOptions(), pathList);
                }
            }

            return pckg;
        }
    }

    protected class PutSetupOperation extends SetupOperation {

        @Override
        protected Package getPackage(SlingHttpServletRequest request) throws IOException {

            Package pckg = null;

            InputStream input = request.getInputStream();
            InputStreamReader reader = new InputStreamReader(input, MappingRules.CHARSET);
            JsonReader jsonReader = new JsonReader(reader);

            pckg = Package.GSON.fromJson(jsonReader, Package.class);

            return pckg;
        }
    }

    /**
     * the 'setup' declares a package object and stores it in the repository
     */
    protected class DeleteOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            if (resource.isValid() &&
                    Package.RESOURCE_TYPE_PACKAGE.equals(resource.getResourceType())) {

                packageManager.deletePackage(resource);

                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                session.save();

                response.setContentLength(0);
                response.setStatus(HttpServletResponse.SC_OK);

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "no valid package definition found");
            }
        }
    }

    /**
     * the 'upload' operation imports the content of a package in the repository; no
     * package object is created in the repository
     */
    protected class UploadOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws IOException {

            InputStream input = getInputStream(request);
            if (input != null) {

                OutputStream log = response.getOutputStream();

                try {
                    ResourceResolver resolver = request.getResourceResolver();
                    packageManager.importPackage(input, log, resolver);

                    Session session = resolver.adaptTo(Session.class);
                    session.save();

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    if (response.isCommitted()) {
                        log.write(ex.toString().getBytes(MappingRules.CHARSET));
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "package file not available");
            }
        }

        protected InputStream getInputStream(SlingHttpServletRequest request) throws IOException {

            InputStream input = null;

            RequestParameterMap parameters = request.getRequestParameterMap();
            RequestParameter file = parameters.getValue(PARAM_PACKAGE);

            if (file != null) {
                input = file.getInputStream();
            }
            return input;
        }
    }

    protected class PutUploadOperation extends UploadOperation {

        @Override
        protected InputStream getInputStream(SlingHttpServletRequest request) throws IOException {
            InputStream input = request.getInputStream();
            return input;
        }
    }

    /**
     * The 'tree' operation generates al list of package nodes for a package tree view.
     */
    protected class PackageTreeOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            if (resource.isValid()) {

                ResourceFilter filter = packageManager.getListFilter();
                NodeServlet.LabelType labelType =
                        RequestUtil.getSelector(request, NodeServlet.LabelType.name);

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);

                response.setStatus(HttpServletResponse.SC_OK);

                NodeServlet.writeJsonNode(jsonWriter, filter, resource, labelType, coreConfig, false);

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "resource not found '" + request.getRequestPathInfo().getSuffix() + "'");
            }
        }
    }

    /**
     * The 'view' operation delegates all requests to a component which renders a HTML view
     * of the requested resource using a 'dispatcher.forward()'.
     *
     * @example for request forwarding from servlet to a component
     */
    protected class ViewPackageOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {

            RequestDispatcher dispatcher;

            if (Package.RESOURCE_TYPE_PACKAGE.equals(resource.getResourceType())) {
                // packages can render itself using the component specified by the resource type
                dispatcher = request.getRequestDispatcher(resource);
            } else {
                // folders are rendered using the 'list' component in the packages implementation
                RequestDispatcherOptions options = new RequestDispatcherOptions();
                options.setForceResourceType("composum/sling/console/package/list");
                dispatcher = request.getRequestDispatcher(resource, options);
            }

            dispatcher.forward(request, response);
        }
    }
}
