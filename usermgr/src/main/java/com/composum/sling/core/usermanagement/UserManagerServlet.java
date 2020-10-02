package com.composum.sling.core.usermanagement;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import com.composum.sling.nodes.NodesConfiguration;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.util.regex.Pattern;

/**
 * The general hook (servlet) for the User Manager feature provides the path '/bin/users.html/...'.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Browser Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + UserManagerServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                "sling.auth.requirements=" + UserManagerServlet.SERVLET_PATH
        },
        immediate = true
)
public class UserManagerServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/users";

    public static final String RESOURCE_TYPE = "composum/nodes/usermgnt";

    public static final String CONSOLE_PATH = "/libs/composum/nodes/usermgnt/content/usermanagement";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(" + SERVLET_PATH + "(\\.[^/]+)?\\.html)(/.*)?$");

    @Reference
    protected NodesConfiguration config;

    @Override
    protected String getServletPath(BeanContext context) {
        return SERVLET_PATH;
    }

    @Override
    protected Pattern getPathPattern(BeanContext context) {
        return PATH_PATTERN;
    }

    @Override
    protected String getResourceType(BeanContext context) {
        return RESOURCE_TYPE;
    }

    @Override
    protected String getConsolePath(BeanContext context) {
        return config.checkConsoleAccess() ? CONSOLE_PATH : null;
    }
}
