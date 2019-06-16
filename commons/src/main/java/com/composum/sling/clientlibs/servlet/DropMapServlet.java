package com.composum.sling.clientlibs.servlet;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;

import static org.slf4j.LoggerFactory.getLogger;

@SlingServlet(
        extensions = {"map"},
        resourceTypes = "sling/servlet/default",
        methods = {HttpConstants.METHOD_GET, HttpConstants.METHOD_HEAD}
)
public class DropMapServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = getLogger(DropMapServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        drop(request, response);
    }

    @Override
    protected void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) {
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
