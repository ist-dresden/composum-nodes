package com.composum.sling.core.pckgmgr.registry.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.registry.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.registry.util.RegistryUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class RegistryNode extends AbstractNode {

    public RegistryNode(String namespace, PackageRegistry registry) {
        String name = registry.getClass().getSimpleName();
        put(KEY_PATH, "/@" + namespace);
        put(KEY_NAME, namespace);
        put(KEY_TEXT, name);
        put(KEY_TYPE, "registry");
    }

    @Override
    @Nullable
    public RegistryItem getItem(@Nonnull final String name) {
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        RegistryItem result = items.get("1_" + name);
        return result != null ? result : items.get("0_" + name);
    }

    @Override
    public void load(@Nonnull final BeanContext context) throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        PackageRegistry registry = service.getRegistries(context.getResolver()).getRegistry(getName());
        Map<String, RegistryItem> items = new TreeMap<>();
        put(KEY_ITEMS, items);
        if (registry != null) {
            for (PackageId pckgId : registry.packages()) {
                GroupNode group = getGroup(this, getPath(), pckgId.getGroup());
                group.addPackage(pckgId);
            }
        }
        setLoaded(true);
    }

    protected static GroupNode getGroup(AbstractNode holder, String parentPath, String name) {
        Map<String, RegistryItem> items = Objects.requireNonNull(holder.getItemsMap());
        String[] groupPath;
        String groupName;
        String groupKey;
        if (StringUtils.isNotBlank(name)) {
            groupPath = name.split("/");
            groupName = groupPath[0];
            groupKey = "0_" + groupName;
        } else {
            groupPath = new String[]{RegistryUtil.NO_GROUP};
            groupName = RegistryUtil.NO_GROUP;
            groupKey = "1_" + groupName;

        }
        GroupNode group = (GroupNode) items.get(groupKey);
        if (group == null) {
            group = new GroupNode(holder, parentPath, groupName);
            items.put(groupKey, group);
        }
        for (int i = 1; i < groupPath.length; i++) {
            group = group.getGroup(groupPath[i]);
        }
        return group;
    }
}
