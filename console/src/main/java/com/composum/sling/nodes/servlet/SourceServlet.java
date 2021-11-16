package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Source Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SourceServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=xml",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=zip",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=pkg",
                "sling.auth.requirements=" + SourceServlet.SERVLET_PATH
        }
)
@Restricted(key = SourceServlet.SERVICE_KEY)
public class SourceServlet extends SlingSafeMethodsServlet implements RestrictedService {

    public static final String SERVLET_PATH = "/bin/cpm/nodes/source";

    public static final String SERVICE_KEY = "nodes/repository/source";

    @Reference
    private ServiceRestrictions restrictions;

    @Reference
    protected NodesConfiguration nodesConfig;

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public ServiceRestrictions.Key getServiceKey() {
        return new ServiceRestrictions.Key(SERVICE_KEY);
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {

        if (restrictions.isPermissible(request, getServiceKey(), ServiceRestrictions.Permission.read)) {

            Resource resource = null;
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            String resourcePath = XSS.filter(pathInfo.getSuffix());
            if (StringUtils.isNotBlank(resourcePath)) {
                ResourceResolver resolver = request.getResourceResolver();
                resource = resolver.getResource(resourcePath);
            }

            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {

                try {
                    SourceModel sourceModel = new SourceModel(nodesConfig,
                            new BeanContext.Servlet(getServletContext(), bundleContext, request, response),
                            resource);

                    String name = resource.getName();
                    switch (StringUtils.defaultIfBlank(pathInfo.getExtension(), "")) {

                        // a single page or a node in its XML source representation
                        case "xml":

                            response.setCharacterEncoding("UTF-8");
                            //response.setContentType("text/xml;charset=UTF-8");
                            response.setContentType("text/plain;charset=UTF-8"); // best to avoid any conversion by the client
                            //response.setContentType("application/octet-stream");
                            response.setHeader("Content-Disposition", "inline; filename=.content.xml");

                            sourceModel.writeXmlFile(response.getWriter(), true);
                            break;

                        // a content hierarchy in a zipped structure with '.content.xml' for the content within
                        case "zip":

                            if (!name.endsWith(".zip")) {
                                name += ".zip";
                            }

                            response.setContentType("application/octet-stream");
                            response.setHeader("Content-Disposition", "inline; filename=" + name);

                            sourceModel.writeArchive(response.getOutputStream());
                            break;

                        // a content hierarchy in a zipped Vault package for installation by the Package Manager
                        case "pkg":

                            String group = "source";
                            if (name.endsWith(".zip")) {
                                name = name.substring(0, name.length() - 4);
                            }
                            name += "-source-package";
                            String version = "current";

                            response.setContentType("application/octet-stream");
                            response.setHeader("Content-Disposition", "inline; filename=" + name + "-" + version + ".zip");

                            sourceModel.writePackage(response.getOutputStream(), group, name, version);
                            break;

                        default:
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                            break;
                    }

                } catch (RepositoryException ex) {
                    throw new ServletException(ex);
                }

            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }
}
