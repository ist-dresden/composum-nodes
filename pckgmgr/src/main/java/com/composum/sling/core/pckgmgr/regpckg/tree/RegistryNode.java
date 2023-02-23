package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
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
        super(null);
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
        if (!isLoaded()) {
            PackageRegistries service = context.getService(PackageRegistries.class);
            PackageRegistry registry = service.getRegistries(context.getResolver()).getRegistry(getName());
            Map<String, RegistryItem> items = new TreeMap<>();
            put(KEY_ITEMS, items);
            if (registry != null) {
                for (PackageId pckgId : registry.packages()) {
                    GroupNode group = getGroup(this, pckgId.getGroup());
                    group.addPackage(getName(), pckgId);
                }
            }
            setLoaded(true);
        }
    }

    protected static GroupNode getGroup(@Nonnull AbstractNode parent, String name) {
        Map<String, RegistryItem> items = Objects.requireNonNull(parent.getItemsMap());
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
            group = new GroupNode(parent, null, groupName);
            items.put(groupKey, group);
        }
        for (int i = 1; i < groupPath.length; i++) {
            group = group.getGroup(groupPath[i]);
        }
        return group;
    }
}
