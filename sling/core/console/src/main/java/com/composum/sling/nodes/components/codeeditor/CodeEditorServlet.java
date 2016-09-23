package com.composum.sling.nodes.components.codeeditor;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import org.apache.felix.scr.annotations.sling.SlingServlet;

import java.util.regex.Pattern;


/**
 * The general hook (servlet) for the Editor feature to edit code in a separate view.
 */
@SlingServlet(
        paths = CodeEditorServlet.SERVLET_PATH,
        methods = {"GET"}
)
public class CodeEditorServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/cpm/edit/code";

    public static final String RESOURCE_TYPE = "composum/nodes/console/components/codeeditor/editpage";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(" + SERVLET_PATH + "(\\.[^/]+)?\\.html)(/.*)?$");

    protected Pattern getPathPattern(BeanContext context) {
        return PATH_PATTERN;
    }

    protected String getResourceType(BeanContext context) {
        return RESOURCE_TYPE;
    }
}