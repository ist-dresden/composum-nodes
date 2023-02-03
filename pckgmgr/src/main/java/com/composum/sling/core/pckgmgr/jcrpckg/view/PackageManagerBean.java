package com.composum.sling.core.pckgmgr.jcrpckg.view;

import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.Packages.Mode;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PackageManagerBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PackageManagerBean.class);

    private transient String path;
    private transient PackageUtil.ViewType type;

    private transient List<String> pathsToVersionsOfThisPackage;

    private transient List<String> pathsToHighestVersionOfEachPackage;

    private transient Map<String, String> registriesMap;

    @Override
    public String getPath() {
        if (path == null) {
            path = PackageUtil.getPath(getRequest());
        }
        return path;
    }

    /**
     * The mode {@link Mode} - {@value Mode#jcrpckg} or {@value Mode#regpckg}.
     */
    public Mode getMode() {
        return Packages.getMode(getRequest());
    }

    /**
     * The kind of view - {@link com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil.ViewType}.
     */
    public String getViewType() {
        if (type == null) {
            type = PackageUtil.getViewType(context, getRequest(), getPath());
        }
        return type != null ? type.name() : "";
    }

    public String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "general";
    }


    /**
     * If this is the pseudo-resource denominating a package (without version, /groupname/packagename), this
     * gives the package versions corresponding to it.
     */
    public List<String> getPathsToVersionsOfThisPackage() {
        if (pathsToVersionsOfThisPackage == null) {
            try {
                JcrPackageManager manager = PackageUtil.getPackageManager(context.getService(Packaging.class), getRequest());
                String group = StringUtils.removeStart(ResourceUtil.getParent(getPath()), "/");
                String name = ResourceUtil.getName(getPath());
                pathsToVersionsOfThisPackage = manager.listPackages(group, false).stream()
                        .filter(p -> name.equals(PackageUtil.getPackageId(p).getName()))
                        .map(p -> PackageUtil.getPackagePath(manager, p))
                        .sorted(Comparator.<String>naturalOrder().reversed())
                        .collect(Collectors.toList());
            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return pathsToVersionsOfThisPackage;
    }

    public List<String> getPathsToHighestVersionOfEachPackage() {
        if (pathsToHighestVersionOfEachPackage == null) {
            try {
                JcrPackageManager manager = PackageUtil.getPackageManager(context.getService(Packaging.class), getRequest());
                Map<Pair<String, String>, List<JcrPackage>> packageGrouped = manager.listPackages().stream()
                        .filter(p -> PackageUtil.getPackagePath(manager, p).startsWith(getPath() + "/"))
                        .collect(Collectors.groupingBy(
                                pckg -> Pair.of(PackageUtil.getPackageId(pckg).getGroup(), PackageUtil.getPackageId(pckg).getName())
                        ));
                List<List<JcrPackage>> packageGroupList = packageGrouped.entrySet().stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .map(Map.Entry::getValue)
                        .map(l -> l.stream()
                                .filter(p -> PackageUtil.getPackageId(p) != null)
                                .sorted(
                                        Comparator.comparing(PackageUtil::getPackageId, new VersionComparator.PackageIdComparator(true))
                                ).collect(Collectors.toList()))
                        .collect(Collectors.toList());
                pathsToHighestVersionOfEachPackage = packageGroupList.stream()
                        .map(l -> l.get(0))
                        .map(p -> PackageUtil.getPackagePath(manager, p))
                        .collect(Collectors.toList());
            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return pathsToHighestVersionOfEachPackage;
    }

    public boolean isRegistriesAvailable() {
        return getRegistries().size() > 0;
    }

    /**
     * Returns the list of package registry keys to their names if there are registries; empty if that service isn't available.
     */
    public Map<String, String> getRegistries() {
        if (registriesMap == null) {
            registriesMap = new TreeMap<>();
            final PackageRegistries packageRegistries = context.getService(PackageRegistries.class);
            if (packageRegistries != null) {
                final PackageRegistries.Registries registries = packageRegistries.getRegistries(getResolver());
                for (final String namespace : registries.getNamespaces()) {
                    final PackageRegistry registry = registries.getRegistry(namespace);
                    if (registry != null) {
                        registriesMap.put(namespace, registry.getClass().getSimpleName());
                    }
                }
            }
        }
        return registriesMap;
    }
}
