package com.composum.sling.core.pckgmgr.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import com.composum.sling.nodes.NodesConfiguration;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;

import java.util.regex.Pattern;

import static com.composum.sling.core.pckgmgr.view.PackagesServlet.SERVLET_PATH;

/**
 * The general hook (servlet) for the Package Manager feature provides the path '/bin/packages.html/...'.
 */
@SlingServlet(
        paths = SERVLET_PATH,
        methods = {"GET"}
)
public class PackagesServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/packages";

    public static final String RESOURCE_TYPE = "composum/nodes/pckgmgr";

    public static final String CONSOLE_PATH = "/libs/composum/nodes/pckgmgr/content/pckgmgr";

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