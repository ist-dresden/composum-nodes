package com.composum.sling.core.pckgmgr.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import org.apache.felix.scr.annotations.sling.SlingServlet;

import java.util.regex.Pattern;

/**
 * The general hook (servlet) for the Package Manager feature provides the path '/bin/packages.html/...'.
 */
@SlingServlet(
        paths = "/bin/packages",
        methods = {"GET"}
)
public class PackagesServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/packages.html";

    public static final String RESOURCE_TYPE = "composum/nodes/pckgmgr";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/packages(\\.[^/]+)?\\.html)(/.*)?$");

    protected Pattern getPathPattern(BeanContext context) {
        return PATH_PATTERN;
    }

    protected String getResourceType(BeanContext context) {
        return RESOURCE_TYPE;
    }
}