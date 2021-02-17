package com.composum.sling.core.pckgmgr.registry.service.impl;

import com.composum.sling.core.pckgmgr.registry.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.registry.util.RegistryUtil;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;

@Component(service = PackageRegistries.class, immediate = true)
public class PackageRegistriesImpl implements PackageRegistries {

    private static final Logger LOG = LoggerFactory.getLogger(PackageRegistriesImpl.class);

    public class RegistriesImpl implements Registries {

        final protected ResourceResolver resolver;
        final protected Map<String, PackageRegistry> registries;

        public RegistriesImpl(ResourceResolver resolver) {
            this.resolver = resolver;
            this.registries = new TreeMap<>();
            for (PackageRegistry registry : registryServices) {
                add(registry);
            }
            add(getJcrPackageRegistry(resolver));
        }

        @Override
        @Nonnull
        public Iterable<PackageRegistry> iterable() {
            return registries.values();
        }

        @Override
        @Nonnull
        public Collection<String> getNamespaces() {
            return registries.keySet();
        }

        @Override
        @Nullable
        public PackageRegistry getRegistry(@Nonnull final String namespaceOrPath) {
            String namespace = namespaceOrPath;
            if (namespaceOrPath.startsWith("/")) {
                Matcher matcher = RegistryUtil.REGISTRY_PATH_PATTERN.matcher(namespaceOrPath);
                namespace = matcher.matches() ? matcher.group("namespace") : "";
            }
            return registries.get(namespace);
        }

        @Override
        @Nullable
        public RegisteredPackage open(@Nonnull final PackageId id) throws IOException {
            RegisteredPackage pckg = null;
            for (PackageRegistry registry : iterable()) {
                pckg = registry.open(id);
                if (pckg != null) {
                    break;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("open({}): {}", id, pckg);
            }
            return pckg;
        }

        protected void add(@Nullable final PackageRegistry registry) {
            if (registry != null) {
                registries.put(RegistryUtil.namespace(registry), registry);
            }
        }
    }

    final protected List<PackageRegistry> registryServices = new ArrayList<>();

    @Reference(
            service = PackageRegistry.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void bindPackageRegistry(PackageRegistry registry) {
        synchronized (registryServices) {
            registryServices.add(registry);
        }
    }

    protected void unbindPackageRegistry(PackageRegistry registry) {
        synchronized (registryServices) {
            registryServices.remove(registry);
        }
    }

    @Reference
    protected Packaging packaging;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Nonnull
    public Registries getRegistries(@Nonnull final ResourceResolver resolver) {
        return new RegistriesImpl(resolver);
    }

    /**
     * Interim method to determine the 'hidden' JcrPackageRegistry.
     */
    protected PackageRegistry getJcrPackageRegistry(@Nonnull final ResourceResolver resolver) {
        PackageRegistry registry = null;
        try {
            JcrPackageManager manager = packaging.getPackageManager(
                    Objects.requireNonNull(resolver.adaptTo(Session.class)));
            Class<?> managerType = manager.getClass();
            Method getRegistry = managerType.getMethod("getRegistry");
            registry = (PackageRegistry) getRegistry.invoke(manager);
        } catch (NoSuchMethodException ex) {
            LOG.warn(ex.toString());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return registry;
    }
}
