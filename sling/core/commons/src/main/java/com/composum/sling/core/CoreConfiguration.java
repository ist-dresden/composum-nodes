package com.composum.sling.core;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.servlet.AbstractServiceServlet;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface CoreConfiguration {

    long getQueryResultLimit();

    boolean isEnabled(AbstractServiceServlet servlet);

    ResourceFilter getPageNodeFilter();

    ResourceFilter getDefaultNodeFilter();

    ResourceFilter getTreeIntermediateFilter();

    ResourceFilter getReferenceableNodesFilter();

    ResourceFilter getOrderableNodesFilter();
}
