package com.composum.sling.core;

import com.composum.sling.core.servlet.AbstractServiceServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface CoreConfiguration {

    String ERRORPAGE_STATUS = "errorpage.status";
    String ERRORPAGES_PATH = "errorpages.path";
    String DEFAULT_ERRORPAGES = "errorpages.default";

    String TREE_INTERMEDIATE_FILTER_KEY = "tree.intermediate.filter";

    String SYSTEM_SERVLET_ENABLED = "system.servlet.enabled";

    String TRANSLATION_SERVLET_ENABLED = "validation.servlet.enabled";

    String FORWARDED_SSL_PORT = "network.forward.ssl.port";

    String LOGOUT_URL = "logouturl";

    int getForwardedSslPort();

    boolean isEnabled(AbstractServiceServlet servlet);

    Resource getErrorpage(SlingHttpServletRequest request, int status);

    boolean forwardToErrorpage(SlingHttpServletRequest request,
                               SlingHttpServletResponse response, int status)
            throws IOException, ServletException;

    /**
     * The (relative) logout URL (including parameters) to use instead of the default /system/sling/logout
     * .html?logout=true&GLO=true . (The GLO=true parameter is relevant for singlesignon logout if Keycloak
     * authentication is installed.)
     */
    String getLogoutUrl();

    Dictionary getProperties();
}
