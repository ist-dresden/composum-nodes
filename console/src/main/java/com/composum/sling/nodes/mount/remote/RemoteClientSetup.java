package com.composum.sling.nodes.mount.remote;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

public interface RemoteClientSetup {

    /**
     * @param aspectKeys the set of builder aspect keys referenced by the remote client
     * @return the set of registered services implementing the requested aspects
     */
    @Nonnull
    Set<RemoteClientBuilder> getBuilders(@Nonnull final Collection<String> aspectKeys);
}
