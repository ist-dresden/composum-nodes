package com.composum.sling.nodes.mount.remote;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * a service interface for extending the building of the remote HTTP client
 */
public interface RemoteClientBuilder {

    String ASPECT_KEY = "aspect.key";

    /**
     * add settings to the HTTP client context configuration
     *
     * @param context the context object to configure
     */
    void configure(@NotNull HttpClientContext context);

    /**
     * Extends the configuration of the HTTP client builder for remote clients
     *
     * @param builder the builder instance to extend
     */
    void configure(@NotNull HttpClientBuilder builder);
}
