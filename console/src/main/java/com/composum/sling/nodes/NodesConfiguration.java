package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface NodesConfiguration {

    String getApplicationPath();

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

    /** Determines for the SourceServlet whether the attributes are sorted by importance. */
    boolean isSourceAdvancedSortAttributes();

    /** Determines when a resource should be rendered as folder in the SourceServlet. */
    ResourceFilter getSourceFolderNodesFilter();

    /** Determines when a resource should be rendered as separate XML file ("vlt:FullCoverage") */
    ResourceFilter getSourceXmlNodesFilter();

    @Nonnull
    String getScenesContentRoot();

    /**
     * The (readonly) properties useable for extensions. E.g. introduce a new property in a newer nodes version, and use
     * it if accessible already when depending on an older nodes version.
     */
    @Nonnull
    Dictionary<String, Object> getProperties();
}
