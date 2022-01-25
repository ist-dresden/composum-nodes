package com.composum.sling.nodes.mount.remote;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public interface RemoteClientSetup {

    /**
     * @param aspectKeys the set of builder aspect keys referenced by the remote client
     * @return the set of registered services implementing the requested aspects
     */
    @NotNull
    Set<RemoteClientBuilder> getBuilders(@NotNull final Collection<String> aspectKeys);
}
