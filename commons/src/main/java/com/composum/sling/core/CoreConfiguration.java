package com.composum.sling.core;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface CoreConfiguration {

    String ERRORPAGE_STATUS = "errorpage.status";

    /**
     * Parameter that can be appended to {@link #getLoginUrl()} to save the current rendered resource, to allow
     * re-rendering it after the user logged in.
     */
    String RESOURCE_PARAMETER = "resource";

    Resource getErrorpage(SlingHttpServletRequest request, int status);

    boolean forwardToErrorpage(SlingHttpServletRequest request,
                               SlingHttpServletResponse response, int status)
            throws IOException, ServletException;

    /**
     * The (relative) logout URL (including parameters) to use instead of the default /system/sling/logout
     * .html?logout=true&GLO=true (The GLO=true parameter is relevant for singlesignon logout if Keycloak
     * authentication is installed). A parameter {@link #RESOURCE_PARAMETER} - the targetUri - can be appended
     * if after user logout in the user should redirect to rendering that resource.
     */
    @NotNull
    String getLogoutUrl(@Nullable String targetUri);

    @Deprecated
    @NotNull
    default String getLogoutUrl() {
        return getLogoutUrl(null);
    }

    /**
     * The URL to redirect to after the user was logged out successfully.
     */
    @NotNull
    String getLoggedoutUrl();

    /**
     * The URL to redirect to when the user should login. A parameter {@link #RESOURCE_PARAMETER} - the targetUri -
     * can be appended if after user login in the user should redirect to rendering that resource.
     */
    @NotNull
    String getLoginUrl(@Nullable String targetUri);

    @Deprecated
    @NotNull
    default String getLoginUrl() {
        return getLoginUrl(null);
    }

    /**
     * The (readonly) properties useable for extensions. E.g. introduce a new property in a newer nodes version, and use
     * it if accessible already when depending on an older nodes version.
     */
    @NotNull
    Dictionary<String, Object> getProperties();
}
