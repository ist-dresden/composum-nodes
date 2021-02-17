package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeItem;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    @Override
    public boolean equals(Object other) {
        return other instanceof TreeItem && getPath().equals(((TreeItem) other).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }
}
