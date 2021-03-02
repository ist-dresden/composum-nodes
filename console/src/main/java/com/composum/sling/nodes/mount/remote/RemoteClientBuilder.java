package com.composum.sling.nodes.mount.remote;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.annotation.Nonnull;

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
    void configure(@Nonnull HttpClientContext context);

    /**
     * Extends the configuration of the HTTP client builder for remote clients
     *
     * @param builder the builder instance to extend
     */
    void configure(@Nonnull HttpClientBuilder builder);
}
