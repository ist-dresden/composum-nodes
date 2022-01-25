package com.composum.sling.clientlibs.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import static org.slf4j.LoggerFactory.getLogger;

@Component(service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=map",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_HEAD,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
public class DropMapServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = getLogger(DropMapServlet.class);

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response) {
        drop(request, response);
    }

    @Override
    protected void doHead(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response) {
        drop(request, response);
    }

    protected static void drop(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("map file request dropped (empty response): '{}'", request.getRequestURI());
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setContentLength(0);
    }
}
