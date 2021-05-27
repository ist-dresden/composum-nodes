package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RegistryBean extends ConsoleSlingBean {

    public static final String RESOURCE_TYPE = "";

    private static final Logger LOG = LoggerFactory.getLogger(RegistryBean.class);

    protected String namespace;
    private transient String title;

    protected boolean loaded = false;
    protected Map<String, List<PackageBean>> groups;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        this.namespace = RegistryUtil.namespace(resource.getPath());
        super.initialize(context, resource);
    }

    @Nonnull
    public String getNamespace() {
        return namespace;
    }

    @Override
    @Nonnull
    public String getName() {
        return getNamespace();
    }

    @Override
    @Nonnull
    public String getPath() {
        return "/@" + getNamespace();
    }

    @Override
    @Nonnull
    public String getTitle() {
        if (title == null) {
            title = getName();
            PackageRegistry registry = getPackageRegistry(context);
            if (registry != null) {
                title = registry.getClass().getSimpleName();
            }
        }
        return title;
    }

    @Nonnull
    protected Map<String, List<PackageBean>> getGroups() {
        if (groups == null) {
            groups = new TreeMap<>();
            PackageRegistry registry = getPackageRegistry(context);
            if (registry != null) {
                try {
                    for (PackageId packageId : registry.packages()) {
                        addPackage(packageId);
                    }
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return groups;
    }

    protected void addPackage(PackageId packageId) {
        List<PackageBean> group = groups.computeIfAbsent(packageId.getGroup(), k -> new ArrayList<>());
        group.add(new PackageBean(context, getNamespace(), packageId));
        Collections.sort(group);
    }

    @Nullable
    protected PackageRegistry getPackageRegistry(BeanContext context) {
        PackageRegistries service = context.getService(PackageRegistries.class);
        PackageRegistries.Registries registries = service.getRegistries(context.getResolver());
        return registries.getRegistry(getNamespace());
    }
}
