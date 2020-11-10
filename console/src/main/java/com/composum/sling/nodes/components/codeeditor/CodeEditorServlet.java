package com.composum.sling.nodes.components.codeeditor;

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

import static com.composum.sling.nodes.browser.BrowserServlet.CONSOLE_PATH;

/**
 * The general hook (servlet) for the Editor feature to edit code in a separate view.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Code Editor Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + CodeEditorServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                "sling.auth.requirements=" + CodeEditorServlet.SERVLET_PATH
        },
        immediate = true
)
public class CodeEditorServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/cpm/edit/code";

    public static final String RESOURCE_TYPE = "composum/nodes/console/components/codeeditor/editpage";

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
