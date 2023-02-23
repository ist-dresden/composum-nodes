package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public interface RegistryItem extends Map<String, Object>, Serializable {

    @Nonnull String getName();

    @Nonnull String getPath();

    @Nonnull String getText();

    @Nonnull String getType();

    @Nullable RegistryItem getParent();

    /** Loads all details of this node from the package(s) and makes sure all children ({@link #getItems()}) are present,
     * though not necessarily loaded. */
    void load(@Nonnull BeanContext context) throws IOException;

    /** Makes sure {@link #getItems()} are present, though not necessarily loaded.
     * might or might not trigger a {@link #load(BeanContext)} if that's necessary for that.
     * (Optimized version of {@link #load(BeanContext)} if we just need the {@link #getItems()}). */
    void loadForItems(@Nonnull BeanContext context) throws IOException;

    boolean isLoaded();

    @Nonnull
    Iterable<RegistryItem> getItems();

    @Nullable
    RegistryItem getItem(@Nonnull String name);

    void toTree(@Nonnull final JsonWriter writer, boolean children, boolean showRoot) throws IOException;

    void toJson(@Nonnull JsonWriter writer) throws RepositoryException, IOException;

    /**
     * In some cases the tree can contain e.g. a group and a package with the same name. This compacts it and it's subnodes.
     */
    void compactSubTree();

    /**
     * Calls {@link #compactSubTree()} (see there for the why) on the {@link #getParent()}, since the compaction might
     * join same named siblings and replace this node for it to have all children.
     * @return the new node to use instead of this one (might or might not be the same instance)
     */
    RegistryItem compactTree();

}
