package com.composum.sling.core.usermanagement;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
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
 * The general hook (servlet) for the Browser feature provides the path '/bin/browser.html/...'.
 * This is the analogous absolute path implementation for the '.browser' selector feature.
 */
@SlingServlet(
        paths = "/bin/users",
        methods = {"GET"}
)
public class UserManagerServlet extends SlingSafeMethodsServlet {

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/users(\\.[^/]+)?\\.html)(/.*)?$");

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // the pattern matching is not necessary but it prevents from errors thrown during unwanted requests
        final String pathInfo = request.getPathInfo();
        final Matcher matcher = PATH_PATTERN.matcher(pathInfo);

        if (matcher.matches()) {
            RequestPathInfo reqPathInfo = request.getRequestPathInfo();

            // the options for the delegation to the browser component implementation
            RequestDispatcherOptions options = new RequestDispatcherOptions();
            String suffix = reqPathInfo.getSuffix();
            if (StringUtils.isBlank(suffix)) {
                // ensure the a target resource is always used
                options.setReplaceSuffix("/");
            }

            // set the browsers component resource type for each request received by this servlet
            String resourceType = "composum/sling/usermgnt";
            options.setForceResourceType(resourceType);

            RequestDispatcher dispatcher = request.getRequestDispatcher(request.getResource(), options);
            dispatcher.include(request, response);
        }
    }
}