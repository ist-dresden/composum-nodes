package com.composum.sling.core.servlet;

import com.composum.sling.core.BeanContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A base class for a general hook (servlet) for a console view.
 */
public abstract class AbstractConsoleServlet extends SlingSafeMethodsServlet {

    protected abstract Pattern getPathPattern(BeanContext context);

    protected abstract String getResourceType(BeanContext context);

    protected String getRequestPath(SlingHttpServletRequest request) {
        RequestPathInfo reqPathInfo = request.getRequestPathInfo();
        String suffix = reqPathInfo.getSuffix();
        if (StringUtils.isBlank(suffix)) {
            suffix = "/";
        }
        return suffix;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        BeanContext context = new BeanContext.Servlet(getServletContext(), request, response);

        // the pattern matching is not necessary but it prevents from errors thrown during unwanted requests
        final String pathInfo = request.getPathInfo();
        final Matcher matcher = getPathPattern(context).matcher(pathInfo);

        if (matcher.matches()) {

            // the options for the delegation to the browser component implementation
            RequestDispatcherOptions options = new RequestDispatcherOptions();
            String path = getRequestPath(request);
            if (StringUtils.isNotBlank(path)) {
                options.setReplaceSuffix(path);
            }

            // set the viewa component resource type for each request received by this servlet
            String resourceType = getResourceType(context);
            options.setForceResourceType(resourceType);

            RequestDispatcher dispatcher = request.getRequestDispatcher(request.getResource(), options);
            dispatcher.include(request, response);
        }
    }
}