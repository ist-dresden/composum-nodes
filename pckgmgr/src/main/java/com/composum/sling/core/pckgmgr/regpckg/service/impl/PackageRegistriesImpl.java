package com.composum.sling.core.pckgmgr.regpckg.service.impl;

import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.jackrabbit.vault.packaging.VersionRange;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.composum.sling.core.pckgmgr.Packages.REGISTRY_BASED_PATH;

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
        public Collection<PackageRegistry> iterable() {
            return Collections.unmodifiableCollection(registries.values());
        }

        @Override
        @Nonnull
        public Collection<String> getNamespaces() {
            return Collections.unmodifiableCollection(registries.keySet());
        }

        @Override
        @Nullable
        public PackageRegistry getRegistry(@Nonnull final String namespaceOrPath) {
            String namespace = namespaceOrPath;
            if (namespaceOrPath.startsWith("/")) {
                Matcher matcher = REGISTRY_BASED_PATH.matcher(namespaceOrPath);
                if (matcher.matches()) {
                    namespace = matcher.group("ns");
                } else { // try to find a package / package version matching the path, for mixed mode
                    Pair<String, PackageId> resolved = null;
                    try {
                        resolved = resolve(namespaceOrPath);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                    if (resolved != null) {
                        namespace = resolved.getLeft();
                    }
                }
            }
            return registries.get(namespace);
        }

        @Nullable
        @Override
        public Pair<String, PackageId> resolve(@Nullable String rawPath) throws IOException {
            Pair<String, PackageId> result = null;
            if (StringUtils.startsWith(rawPath, "/")) {
                Matcher matcher = REGISTRY_BASED_PATH.matcher(rawPath);
                String path = rawPath;
                List<PackageRegistry> searchRegistries = new ArrayList<>();
                if (matcher.matches()) {
                    String namespace = matcher.group("ns");
                    path = matcher.group("path");
                    if (getRegistry(namespace) != null) {
                        searchRegistries.add(getRegistry(namespace));
                    }
                } else {
                    searchRegistries.addAll(iterable());
                }

                // now try to find a package / package version matching the path
                for (Map.Entry<String, PackageRegistry> entry : registries.entrySet()) {
                    for (PackageId id : entry.getValue().packages()) {
                        if (path.equals(RegistryUtil.toPath((String) null, id))) {
                            result = Pair.of(entry.getKey(), id);
                        } else if (path.equals(RegistryUtil.toPackagePath(null, id))) {
                            if (result == null || new VersionComparator().compare(result.getRight().getVersionString(), id.getVersionString()) < 0) {
                                result = Pair.of(entry.getKey(), id);
                            }
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Creates a matcher from either {@code group/package} or {@code group/package/version}.
         */
        @Nullable
        protected Dependency pathToDependency(@Nonnull String path) {
            String lastSegment = ResourceUtil.getName(path);
            String parent = ResourceUtil.getParent(path);
            if (lastSegment == null || parent == null) {
                return null;
            }
            Version version = null;
            if (lastSegment.matches("^[0-9].*")) {
                // we assume that's the version, which is not 100% accurate, but we cannot really tell from the path.
                version = Version.create(lastSegment);
                lastSegment = ResourceUtil.getName(parent);
                parent = ResourceUtil.getParent(path);
            }
            if (lastSegment == null || parent == null) {
                return null;
            }
            return new Dependency(parent, lastSegment, new VersionRange(version));
        }

        @Nullable
        @Override
        public Pair<PackageRegistry, PackageId> resolve(@Nonnull Dependency dependency, boolean onlyInstalled) throws IOException {
            PackageRegistry candidateRegistry = null;
            PackageId candidate = null;
            for (PackageRegistry registry : iterable()) {
                PackageId pckg = registry.resolve(dependency, onlyInstalled);
                if (pckg != null) {
                    if (candidate == null || new VersionComparator().compare(candidate.getVersionString(), pckg.getVersionString()) < 0) {
                        candidate = pckg;
                        candidateRegistry = registry;
                    }
                }
            }
            return candidate != null ? Pair.of(candidateRegistry, candidate) : null;
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
