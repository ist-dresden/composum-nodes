package com.composum.sling.core.proxy;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * the proxy request service interface
 */
public interface GenericProxyService {

    /**
     * return the key of the service
     */
    @NotNull
    String getName();

    /**
     * Handles the proxy request if appropriate (target pattern matches and access allowed)
     *
     * @param request   the proxy request
     * @param response  the response for the answer
     * @param targetUrl the url of the request which is addressing the target
     * @return 'true' if the request is supported by the service, allowed for the user and handle by the service
     */
    boolean doProxy(@NotNull SlingHttpServletRequest request,
                    @NotNull SlingHttpServletResponse response,
                    @NotNull String targetUrl)
            throws IOException;
}
