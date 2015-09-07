package com.composum.sling.core;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletException;
import java.io.IOException;

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

    Resource getErrorpage(SlingHttpServletRequest request, int status);

    boolean forwardToErrorpage(SlingHttpServletRequest request,
                               SlingHttpServletResponse response, int status)
            throws IOException, ServletException;
}
