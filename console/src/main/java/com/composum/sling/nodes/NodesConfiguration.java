package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface NodesConfiguration {

    String getApplicationPath();

    boolean checkConsoleAccess();

    @NotNull
    String[] getConsoleCategories();

    long getQueryResultLimit();

    @NotNull
    ResourceFilter getPageNodeFilter();

    @NotNull
    ResourceFilter getDefaultNodeFilter();

    @NotNull
    ResourceFilter getTreeIntermediateFilter();

    @NotNull
    ResourceFilter getReferenceableNodesFilter();

    @NotNull
    ResourceFilter getOrderableNodesFilter();

    @NotNull
    ResourceFilter getSourceNodesFilter();

    /** Determines for the SourceServlet whether the attributes are sorted by importance. */
    boolean isSourceAdvancedSortAttributes();

    /** Determines when a resource should be rendered as folder in the SourceServlet. */
    @NotNull
    ResourceFilter getSourceFolderNodesFilter();

    /** Determines when a resource should be rendered as separate XML file ("vlt:FullCoverage") */
    @NotNull
    ResourceFilter getSourceXmlNodesFilter();

    @NotNull
    String getScenesContentRoot();

    /**
     * The (readonly) properties useable for extensions. E.g. introduce a new property in a newer nodes version, and use
     * it if accessible already when depending on an older nodes version.
     */
    @NotNull
    Dictionary<String, Object> getProperties();
}
