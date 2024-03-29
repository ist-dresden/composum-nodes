package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.SlingResourceUtil;

import org.apache.jackrabbit.vault.packaging.PackageId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class GroupNode extends AbstractNode {


    public GroupNode(@Nullable final RegistryItem parent, @Nullable String parentPath, @Nonnull final String name) {
        super(parent);
        parentPath = parentPath != null ? parentPath : parent.getPath();
        String path = SlingResourceUtil.appendPaths(parentPath, name);
        put(KEY_PATH, path);
        put(KEY_NAME, name);
        put(KEY_TEXT, name);
        put(KEY_TYPE, "folder");
        Map<String, RegistryItem> items = new TreeMap<>();
        put(KEY_ITEMS, items);
    }

    @Override
    @Nullable
    public RegistryItem getItem(@Nonnull final String name) {
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        RegistryItem result = items.get("1_" + name);
        return result != null ? result : items.get("0_" + name);
    }

    @Override
    public void load(@Nonnull final BeanContext context) {
        setLoaded(true);
    }

    @Override
    public void loadForItems(@Nonnull BeanContext context) throws IOException {
        // empty - for getItems() to work we don't need a load() here
    }

    @Nonnull
    public GroupNode getGroup(String name) {
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        GroupNode group = (GroupNode) items.get("0_" + name);
        if (group == null) {
            group = new GroupNode(this, null, name);
            items.put("0_" + group.getName(), group);
        }
        return group;
    }

    public PackageNode addPackage(@Nonnull String registryNamespace, @Nonnull PackageId id) {
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        PackageNode pckg = (PackageNode) items.get("1_" + id.getName());
        if (pckg == null) {
            pckg = new PackageNode(this, id);
            items.put("1_" + id.getName(), pckg);
        }
        pckg.addVersion(registryNamespace, id);
        return pckg;
    }

    /** This shouldn't be called, but we make sure it isn't, because the super implementation is wrong here. */
    @Override
    protected void combineChildren(GroupNode otherNode) {
        throw new UnsupportedOperationException("BUG: this shouldn't be called.");
    }
}
