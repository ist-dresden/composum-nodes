package com.composum.sling.core.pckgmgr.regpckg.service;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

public interface PackageRegistries {

    /**
     * The JcrPackageRegistry is instantiated in a resolver context;
     * therefore the set of registries must be an object that also depends on a resolver (on the current request).
     */
    interface Registries {

        @Nonnull
        Iterable<PackageRegistry> iterable();

        @Nonnull
        Collection<String> getNamespaces();

        @Nullable
        PackageRegistry getRegistry(@Nonnull String namespaceOrPath);

        @Nullable
        RegisteredPackage open(@Nonnull PackageId id) throws IOException;
    }

    @Nonnull
    Registries getRegistries(@Nonnull ResourceResolver resolver);
}
