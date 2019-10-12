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

    /**
     * Parameter that is appended to {@link #getLoginUrl()} to save the current rendered resource, to allow
     * re-rendering it after the user logged in.
     */
    public static final String RESOURCE_PARAMETER = "resource";

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

    /** The URL to redirect to after the user was logged out successfully. */
    String getLoggedoutUrl();

    /**
     * The URL to redirect to when the user should login. A parameter {@link #RESOURCE_PARAMETER} can be appended
     * if after user loging in the user should redirect to rendering that resource.
     */
    String getLoginUrl();

    Dictionary getProperties();
}
