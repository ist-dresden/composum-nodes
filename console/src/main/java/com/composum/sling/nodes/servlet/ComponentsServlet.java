package com.composum.sling.nodes.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.service.ComponentsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

import static com.composum.sling.nodes.servlet.ComponentsServlet.SERVICE_KEY;

@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Components Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + ComponentsServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        }
)
@Restricted(key = SERVICE_KEY)
public class ComponentsServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentsServlet.class);

    public static final String SERVICE_KEY = "nodes/components/operations";

    public static final String SERVLET_PATH = "/bin/cpm/nodes/components";

    public static final String PARAM_PATH = "path";
    public static final String PARAM_TYPE = "type";

    @Reference
    protected ComponentsService componentsService;

    @Reference
    protected NodesConfiguration nodesConfig;

    protected BundleContext bundleContext;

    //
    // Servlet operations
    //

    public enum Extension {json}

    public enum Operation {createOverlay, removeOverlay}

    protected ServletOperationSet<ComponentsServlet.Extension, ComponentsServlet.Operation> operations =
            new ServletOperationSet<>(ComponentsServlet.Extension.json);

    @NotNull
    protected ServletOperationSet<ComponentsServlet.Extension, ComponentsServlet.Operation> getOperations() {
        return operations;
    }

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, ComponentsServlet.Extension.json,
                Operation.createOverlay, new CreateOverlayOperation());
        operations.setOperation(ServletOperationSet.Method.POST, ComponentsServlet.Extension.json,
                Operation.removeOverlay, new RemoveOverlayOperation());
    }

    @Nullable
    protected static String getComponentType(@NotNull final SlingHttpServletRequest request,
                                             @Nullable final ResourceHandle resource) {
        String parameter = request.getParameter(PARAM_TYPE);
        if (StringUtils.isBlank(parameter)) {
            parameter = request.getParameter(PARAM_PATH);
            if (StringUtils.isBlank(parameter)) {
                return resource != null ? resource.getPath() : null;
            }
        }
        return parameter;
    }

    protected class CreateOverlayOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException {
            final Status status = new Status(request, response, LOG);
            final String type = getComponentType(request, resource);
            if (StringUtils.isNotBlank(type)) {
                ResourceResolver resolver = request.getResourceResolver();
                try {
                    componentsService.createOverlay(resolver, type);
                    resolver.commit();
                } catch (PersistenceException ex) {
                    status.error(ex.getMessage(), ex);
                }
            } else {
                status.error("type expected");
            }
            status.sendJson();
        }
    }

    protected class RemoveOverlayOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException {
            final Status status = new Status(request, response, LOG);
            final String type = getComponentType(request, resource);
            if (StringUtils.isNotBlank(type)) {
                ResourceResolver resolver = request.getResourceResolver();
                try {
                    componentsService.removeOverlay(resolver, type);
                    resolver.commit();
                } catch (PersistenceException ex) {
                    status.error(ex.getMessage(), ex);
                }
            } else {
                status.error("type expected");
            }
            status.sendJson();
        }
    }
}
