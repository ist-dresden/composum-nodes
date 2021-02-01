package com.composum.sling.core.pckgmgr.service.impl;

import com.composum.sling.core.pckgmgr.service.PackageRegistries;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component(service = PackageRegistries.class)
public class PackageRegistriesImpl implements PackageRegistries {

    private static final Logger LOG = LoggerFactory.getLogger(PackageRegistriesImpl.class);

    final protected List<PackageRegistry> registries = new ArrayList<>();

    @Reference(
            service = PackageRegistry.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void bindPackageRegistry(PackageRegistry registry) {
        synchronized (registries) {
            registries.add(registry);
        }
    }

    protected void unbindPackageRegistry(PackageRegistry registry) {
        synchronized (registries) {
            registries.remove(registry);
        }
    }

    @Deactivate
    protected void deactivate() {
        synchronized (registries) {
            registries.clear();
        }
    }

    @Nonnull
    public List<PackageRegistry> getRegistries() {
        return Collections.unmodifiableList(registries);
    }

    @Nullable
    public RegisteredPackage getPackage(@Nonnull final PackageId id) {
        RegisteredPackage pckg = null;
        for (PackageRegistry registry : registries) {
            try {
                pckg = registry.open(id);
                if (pckg != null) {
                    break;
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("getPackage({}): {}", id, pckg);
        }
        return pckg;
    }
}
