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

    String getName();

    String getPath();

    String getText();

    String getType();

    /** Loads all details of this node from the package(s) and makes sure all children ({@link #getItems()}) are present,
     * though not necessarily loaded. */
    void load(@Nonnull BeanContext context) throws IOException;

    boolean isLoaded();

    @Nonnull
    Iterable<RegistryItem> getItems();

    @Nullable
    RegistryItem getItem(@Nonnull String name);

    void toTree(@Nonnull final JsonWriter writer, boolean children, boolean showRoot) throws IOException;

    void toJson(@Nonnull JsonWriter writer) throws RepositoryException, IOException;

    /** In some cases the tree can contain e.g. a group and a package with the same name. This compacts it and it's subnodes. */
    void compactSubTree();

}
