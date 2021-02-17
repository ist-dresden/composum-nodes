package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class RegistryTree extends AbstractNode {

    protected final boolean merged;

    public RegistryTree(boolean merged) {
        this.merged = merged;
        put(KEY_PATH, "/");
        put(KEY_NAME, "/");
        put(KEY_TEXT, "Packages");
        put(KEY_TYPE, "root");
    }

    @Nullable
    public RegistryItem getItem(@Nonnull final BeanContext context, @Nonnull final String path)
            throws IOException {
        RegistryItem item = this;
        if (!"/".equals(path)) {
            String[] segments = (path.startsWith("/") ? path.substring(1) : path).split("/");
            int i = 0;
            if (merged) {
                if (segments.length > 0) {
                    if (!item.isLoaded()) {
                        item.load(context);
                    }
                    RegistryItem found = item.getItem("0_" + segments[i]);
                    if (found == null) {
                        found = item.getItem("1_" + segments[i]);
                    }
                    item = found;
                }
                i++;
            }
            for (; item != null && i < segments.length; i++) {
                if (!item.isLoaded()) {
                    item.load(context);
                }
                item = item.getItem(segments[i]);
            }
        }
        return item;
    }

    @Override
    public void load(@Nonnull BeanContext context) throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        Map<String, RegistryItem> items = new TreeMap<>();
        put(KEY_ITEMS, items);
        PackageRegistries.Registries registries = service.getRegistries(context.getResolver());
        for (String namespace : registries.getNamespaces()) {
            PackageRegistry registry = Objects.requireNonNull(registries.getRegistry(namespace));
            if (merged) {
                for (PackageId pckgId : registry.packages()) {
                    GroupNode group = RegistryNode.getGroup(this, "", pckgId.getGroup());
                    group.addPackage(pckgId);
                }
            } else {
                RegistryNode node = new RegistryNode(namespace, registry);
                items.put("@" + namespace, node);
            }
        }
        setLoaded(true);
    }

    @Override
    public void toTree(@Nonnull final JsonWriter writer, boolean children, boolean showRoot) throws IOException {
        if (showRoot) {
            super.toTree(writer, children, true);
        } else {
            writer.beginArray();
            Map<String, RegistryItem> items = getItemsMap();
            if (items != null) {
                for (RegistryItem item : items.values()) {
                    item.toTree(writer, false, true);
                }
            }
            writer.endArray();
        }
    }
}
