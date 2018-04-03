package com.composum.sling.core.usermanagement;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import com.composum.sling.nodes.NodesConfiguration;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;

import java.util.regex.Pattern;

/**
 * The general hook (servlet) for the User Manager feature provides the path '/bin/users.html/...'.
 */
@SlingServlet(
        paths = "/bin/users",
        methods = {"GET"}
)
public class UserManagerServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/users.html";

    public static final String RESOURCE_TYPE = "composum/nodes/usermgnt";

    public static final String CONSOLE_PATH = "/libs/composum/nodes/usermgnt/content/usermanagement";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/users(\\.[^/]+)?\\.html)(/.*)?$");

    @Reference
    protected NodesConfiguration config;

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