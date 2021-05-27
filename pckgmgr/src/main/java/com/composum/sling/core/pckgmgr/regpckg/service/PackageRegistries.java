package com.composum.sling.core.pckgmgr.regpckg.service;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

/**
 * Lookup service for all package registries - normally a registry with {@code jcr} namespace and, if configured, a registry with {@code fs} namespace.
 */
public interface PackageRegistries {

    /**
     * Collects the available registries bound to a resolver. Since the JcrPackageRegistry is instantiated in a resolver context,
     * the set of registries must be an object that also depends on a resolver (on the current request).
     */
    interface Registries {

        @Nonnull
        Collection<PackageRegistry> iterable();

        @Nonnull
        Collection<String> getNamespaces();

        /**
         * Gives the {@link PackageRegistry} for a namespace (e.g. @fs) or a path starting with a namespace ( e.g. /@fs/some/thing).
         * If the path does not have an explicit namespace (e.g. mixed view) but matches a package / package version in one of the registries,
         * we return that registry; if there are more than one, we return the registry with the highest version, as this one is probably most
         * interesting.
         */
        @Nullable
        PackageRegistry getRegistry(@Nonnull String namespaceOrPath);

        /**
         * Finds the namespace and package that matches the given path. If the path doesn't encode the version,
         * we return the package with the highest version. If there is no namespace given (merged mode),
         * we search the registries.
         */
        @Nullable
        Pair<String, PackageId> resolve(@Nullable String path) throws IOException;

        /** Finds the best {@link PackageId} that matches the dependency filter, if there is one. */
        @Nullable
        Pair<PackageRegistry, PackageId> resolve(@Nonnull Dependency dependency, boolean onlyInstalled) throws IOException;

        /**
         * Opens a {@link RegisteredPackage} - remember to close it (AutoCloseable).
         */
        @Nullable
        RegisteredPackage open(@Nonnull PackageId id) throws IOException;

    }

    @Nonnull
    Registries getRegistries(@Nonnull ResourceResolver resolver);
}
