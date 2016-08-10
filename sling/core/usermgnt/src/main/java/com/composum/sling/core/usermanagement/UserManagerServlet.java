package com.composum.sling.core.usermanagement;

import com.composum.sling.core.servlet.AbstractConsoleServlet;
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

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/users(\\.[^/]+)?\\.html)(/.*)?$");

    protected Pattern getPathPattern() {
        return PATH_PATTERN;
            }

    protected String getResourceType() {
        return RESOURCE_TYPE;
    }
}