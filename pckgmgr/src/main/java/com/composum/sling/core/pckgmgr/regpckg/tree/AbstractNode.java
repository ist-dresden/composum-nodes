package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeItem;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractNode extends LinkedHashMap<String, Object> implements RegistryItem {

    private static final Logger LOG = LoggerFactory.getLogger(PackageNode.class);

    public static final String KEY_NAME = "name";
    public static final String KEY_PATH = "path";
    public static final String KEY_TEXT = "text";
    public static final String KEY_TYPE = "type";
    public static final String KEY_STATE = "state";
    public static final String KEY_LOADED = "loaded";
    public static final String KEY_ITEMS = "items";

    private boolean loaded = false;

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    protected void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    @Nonnull
    public Iterable<RegistryItem> getItems() {
        Map<String, RegistryItem> items = getItemsMap();
        return items != null ? items.values() : Collections.emptyList();
    }

    @Override
    @Nullable
    public RegistryItem getItem(@Nonnull final String name) {
        Map<String, RegistryItem> items = getItemsMap();
        return items != null ? items.get(name) : null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected Map<String, RegistryItem> getItemsMap() {
        return ((Map<String, RegistryItem>) get(KEY_ITEMS));
    }

    @Override
    public String getName() {
        return (String) get(KEY_NAME);
    }

    @Override
    public String getPath() {
        return (String) get(KEY_PATH);
    }

    @Override
    public String getText() {
        return (String) get(KEY_TEXT);
    }

    @Override
    public String getType() {
        return (String) get(KEY_TYPE);
    }

    @Override
    public void toTree(@Nonnull final JsonWriter writer, boolean children, boolean showRoot) throws IOException {
        if (showRoot) {
            writer.beginObject();
            toTreeProperties(writer);
            writer.name(KEY_STATE).beginObject();
            toTreeState(writer);
            writer.endObject();
            if (children) {
                writer.name("children");
                toTreeChildren(writer);
            }
            writer.endObject();
        } else {
            toTreeChildren(writer);
        }
    }

    protected void toTreeChildren(@Nonnull final JsonWriter writer) throws IOException {
        writer.beginArray();
        Map<String, RegistryItem> items = getItemsMap();
        if (items != null) {
            for (RegistryItem item : items.values()) {
                item.toTree(writer, false, true);
            }
        }
        writer.endArray();
    }

    protected void toTreeProperties(@Nonnull final JsonWriter writer) throws IOException {
        writer.name(KEY_NAME).value(getName());
        writer.name(KEY_PATH).value(getPath());
        writer.name(KEY_TEXT).value(getText());
        writer.name(KEY_TYPE).value(getType());
    }

    protected void toTreeState(@Nonnull final JsonWriter writer) throws IOException {
        writer.name(KEY_LOADED).value(isLoaded());
    }

    @Override
    public void toJson(@Nonnull final JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtil.jsonMapEntries(writer, this);
        Map<String, RegistryItem> items = getItemsMap();
        if (items != null) {
            writer.name("children").beginArray();
            for (RegistryItem item : items.values()) {
                try {
                    item.toJson(writer);
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    throw new IOException(ex.getMessage());
                }
            }
        }
        writer.endArray();
        writer.endObject();
    }

    /**
     * We clear up the rare case that there are two items with the same {@link TreeItem#getName()}
     * (can happen if there is, e.g., a group that has the same name as a package),
     * which would lead to trouble with tree display since the same id was used twice.
     * We take what's more specific (that is, not GroupNode) and add the children there.
     */
    @Override
    public void compactSubTree() {
        Map<String, RegistryItem> children = getItemsMap();
        if (children == null) {
            return;
        }
        Map<String, String> nameToKey = new HashMap<>();
        List<String> keysToRemove = new ArrayList<>();
        for(Iterator<Map.Entry<String, RegistryItem>> it = children.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, RegistryItem> entry = it.next();
            entry.getValue().compactSubTree();
            String name = entry.getValue().getName();
            String otherEntryKey = nameToKey.get(name);
            if (otherEntryKey != null) {
                RegistryItem other = children.get(otherEntryKey);
                if (entry.getValue() instanceof AbstractNode && other instanceof AbstractNode) {
                    AbstractNode entryNode = (AbstractNode) entry.getValue();
                    AbstractNode otherNode = (AbstractNode) other;
                    if (entryNode instanceof GroupNode) {
                        otherNode.combineChildren((GroupNode) entryNode);
                        it.remove();
                    } else if (otherNode instanceof GroupNode) {
                        entryNode.combineChildren((GroupNode) otherNode);
                        keysToRemove.add(otherEntryKey);
                    } else {
                        LOG.warn("Found two not combineable items with the same name: {} and {}", entry.getValue(), other);
                    }
                }
            }
            nameToKey.put(name, entry.getKey());
        }
        keysToRemove.forEach(children::remove); // were combined into other nodes
    }

    /** Within {@link #compactSubTree()}: we add the children of the otherNode (which has the same name) to our children,
     * so that the otherNode can be removed. */
    protected void combineChildren(GroupNode otherNode) {
        Collection duplicatedKeys = CollectionUtils.intersection(getItemsMap().keySet(), otherNode.getItemsMap().keySet());
        if (!duplicatedKeys.isEmpty()) { // shouldn't happen since the children's names correspond to the path.
            // we'd be lost if it somehow happens anyway, so we just log it.
            LOG.error("Found duplicated keys in children of {}: {}", getPath(), duplicatedKeys);
        }
        getItemsMap().putAll(otherNode.getItemsMap());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TreeItem && getPath().equals(((TreeItem) other).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }
}
