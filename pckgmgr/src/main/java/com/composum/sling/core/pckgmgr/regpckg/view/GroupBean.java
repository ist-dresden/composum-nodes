package com.composum.sling.core.pckgmgr.regpckg.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.SlingResourceUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;

/**
 * Model for a group of packages - packages below a path.
 */
public class GroupBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(GroupBean.class);

    protected transient PackageRegistries.Registries registries;

    protected String namespace;
    protected List<String> packages = new ArrayList<>();


    @Override
    public void initialize(BeanContext context, Resource resource) {
        SlingHttpServletRequest request = context.getRequest();
        String path = RegistryUtil.requestPath(request);
        this.namespace = RegistryUtil.namespace(path);
        super.initialize(context, new SyntheticResource(context.getResolver(), path, ""));
        try {
            load(context);
        } catch (IOException ex) {
            LOG.error("Error loading {}", resource.getPath(), ex);
        }
    }

    protected void load(BeanContext context) throws IOException {
        if (registries == null) {
            PackageRegistries service = context.getService(PackageRegistries.class);
            registries = service.getRegistries(context.getResolver());
        }
        PackageRegistry singleRegistry = StringUtils.isNotBlank(namespace) ? registries.getRegistry(namespace) : null;
        Collection<PackageRegistry> regs = registries.iterable();
        if (singleRegistry != null) {
            regs = Collections.singletonList(singleRegistry);
        }
        List<PackageId> registeredpackages = new ArrayList<>();
        for (PackageRegistry reg : regs) {
            registeredpackages.addAll(reg.packages());
        }
        Map<Pair<String, String>, List<PackageId>> grouped = registeredpackages.stream()
                .collect(Collectors.groupingBy(pkg -> Pair.of(pkg.getGroup(), pkg.getName())));
        Comparator<PackageId> comparator = new VersionComparator.PackageIdComparator(false);
        packages = grouped.entrySet().stream()
                .map(entry -> entry.getValue().stream().max(comparator))
                .filter(Optional::isPresent)
                .map(Optional::get)
                // link without the version if merged mode <-> singleRegistry
                .map(pkg -> RegistryUtil.toPath(singleRegistry, pkg))
                .filter(pkgpath -> SlingResourceUtil.isSameOrDescendant(getPath(), pkgpath))
                .collect(Collectors.toList());
    }

    /** Sorted collection of paths to the packages (without version) contained below this path. */
    public List<String> getPackagePaths() {
        return packages;
    }
}
