package com.composum.sling.core.pckgmgr;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.pckgmgr.jcrpckg.PackageServlet;
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
 * The general hook (servlet) for the Package Manager feature provides the path '/bin/packages.html/...'.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Packages Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + PackagesServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                "sling.auth.requirements=" + PackagesServlet.SERVLET_PATH
        },
        immediate = true
)
@Restricted(key = PackageServlet.SERVICE_KEY)
public class PackagesServlet extends AbstractConsoleServlet {

    public static final String SERVLET_PATH = "/bin/packages";

    public static final String RESOURCE_TYPE = "composum/nodes/pckgmgr";

    public static final String CONSOLE_PATH = RESOURCE_TYPE + "/content/pckgmgr";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(" + SERVLET_PATH + "(\\.[^/]+)?\\.html)(/.*)?$");

    @Reference
    protected CoreConfiguration coreConfig;

    @Reference
    protected NodesConfiguration nodesConfig;

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
        return nodesConfig.checkConsoleAccess() ? coreConfig.getComposumBase() + CONSOLE_PATH : null;
    }
}
