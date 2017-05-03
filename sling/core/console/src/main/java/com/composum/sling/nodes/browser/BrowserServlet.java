package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import org.apache.felix.scr.annotations.sling.SlingServlet;

import java.util.regex.Pattern;

/**
 * The general hook (servlet) for the Browser feature provides the path '/bin/browser.html/...'.
 */
@SlingServlet(
        paths = "/bin/browser",
        methods = {"GET"}
)
public class BrowserServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/browser.html";

    public static final String RESOURCE_TYPE = "composum/nodes/console/browser";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/browser(\\.[^/]+)?\\.html)(/.*)?$");

    @Override
    protected Pattern getPathPattern(BeanContext context) {
        return PATH_PATTERN;
    }

    @Override
    protected String getResourceType(BeanContext context) {
        return RESOURCE_TYPE;
    }
}