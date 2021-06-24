package com.composum.nodes.debugutil;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Renders a resource with a given resource type. The servlet is activated by selector {@value #SELECTOR}
 * and renders the resource with a resourcetype given as parameter {@value #PARAM_FORCE_RESOURCE_TYPE}.
 *
 * E.g.
 * http://localhost:9090/var/composum/platform/security/credentials/composum/platform/workflow/test/mail/servers/hpssendgrid.debugReplaceResourceType.edit.dialog.html?forceResourceType=composum/platform/workflow/components/mail/mailserverconfig
 *
 * @deprecated doesn't work right yet
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Render With ResourceType Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + RenderWithResourceTypeServlet.SELECTOR,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = RenderWithResourceTypeServlet.Config.class)
@Deprecated
public class RenderWithResourceTypeServlet extends SlingSafeMethodsServlet {

    protected Config config;

    /**
     * Parameter with which {@link RequestDispatcherOptions} can be given - meant to be a forced resource type to render
     * the resource with.
     */
    public static final String PARAM_FORCE_RESOURCE_TYPE = "forceResourceType";

    /**
     * The selector which activates this servlet.
     */
    public static final String SELECTOR = "debugReplaceResourceType";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String resourceType = request.getParameter(PARAM_FORCE_RESOURCE_TYPE);
        if (!config.enabled()) {
            resourceType = null;
        }
        RequestDispatcherOptions opts = new RequestDispatcherOptions(resourceType);
        String newSelectors = Arrays.stream(request.getRequestPathInfo().getSelectors())
                .filter(s -> !SELECTOR.equals(s))
                .collect(Collectors.joining("."));
        opts.setReplaceSelectors(newSelectors);
        RequestDispatcher dispatcher = request.getRequestDispatcher(request.getRequestPathInfo().getResourcePath(), opts);
        dispatcher.include(request, response);
    }

    @Activate
    @Modified
    @Deactivate
    protected void setConfig(Config config) {
        this.config = config;
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Render With ResourceType Servlet",
            description = "Renders a resource with a given resource type. The servlet is activated by selector debugReplaceResourceType\n" +
                    " and renders the resource with a resourcetype given as parameter forceResourceType. Probably doesn't work right yet."
    )
    @interface Config {

        @AttributeDefinition(
                description = "Enables the servlet. Please consider deleting the configuration to deactivate."
        )
        boolean enabled() default false;

    }

}
