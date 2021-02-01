package com.composum.sling.core.pckgmgr.service;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface PackageRegistries {

    @Nonnull
    List<PackageRegistry> getRegistries();

    @Nullable
    RegisteredPackage getPackage(@Nonnull PackageId id);
}
