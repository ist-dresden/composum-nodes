package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface NodesConfiguration {

    boolean checkConsoleAccess();

    @Nonnull
    String[] getConsoleCategories();

    long getQueryResultLimit();

    boolean isEnabled(Servlet servlet);

    @Nonnull
    ResourceFilter getPageNodeFilter();

    @Nonnull
    ResourceFilter getDefaultNodeFilter();

    @Nonnull
    ResourceFilter getTreeIntermediateFilter();

    @Nonnull
    ResourceFilter getReferenceableNodesFilter();

    @Nonnull
    ResourceFilter getOrderableNodesFilter();

    @Nonnull
    ResourceFilter getSourceNodesFilter();

    /**
     * The (readonly) properties useable for extensions. E.g. introduce a new property in a newer nodes version, and use
     * it if accessible already when depending on an older nodes version.
     */
    @Nonnull
    Dictionary<String, Object> getProperties();
}
