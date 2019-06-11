package com.composum.sling.clientlibs.service;

import com.composum.sling.core.filter.ResourceFilter;

import javax.annotation.Nonnull;

/**
 * Plugin for the clientlib service that can limit extendability.
 */
public interface ClientlibPermissionPlugin {

    /**
     * Returns limits for the client libraries that are used for the given category. This can be used to avoid
     * security issues where a malicious user can add client libraries to a category another user uses, and thus
     * compromise the site of the other user. To avoid this, there has to be a {@link ClientlibPermissionPlugin}
     * that returns a {@link ResourceFilter} that matches only the areas in the JCR tree the legitimate users
     * for the site can write to, but not the areas potentially malicious users can write to (e.g. other tenants).
     * <p>
     * Caution: If there are several {@link ClientlibPermissionPlugin}s, the {@link ResourceFilter} of all plugins have to be matched.
     * Thus, if a particular plugin doesn't care about a category, it must return {@link ResourceFilter#ALL}!
     * We also assume that if the filter matches one path, it should also match all subpaths.
     *
     * @param category the name of a category
     * @return a filter that restricts client libraries that should be included into a category.
     * not null - return {@link ResourceFilter#ALL} if this {@link ClientlibPermissionPlugin} does not pose a restriction
     * for a category.
     */
    @Nonnull
    ResourceFilter categoryFilter(@Nonnull String category);

}
