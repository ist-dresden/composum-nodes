package com.composum.sling.core;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface CoreConfiguration {

    String ERRORPAGE_STATUS = "errorpage.status";
    String ERRORPAGES_PATH = "errorpages.path";

    long QUERY_RESULT_LIMIT_DEFAULT = 500L;
    String QUERY_RESULT_LIMIT_KEY = "query.result.limit";
    String QUERY_TEMPLATES_KEY = "query.templates";

    String GROOVY_SETUP_SCRIPT = "groovy.setup.script";

    String PAGE_NODE_FILTER_KEY = "node.page.filter";
    String DEFAULT_NODE_FILTER_KEY = "node.default.filter";
    String TREE_INTERMEDIATE_FILTER_KEY = "tree.intermediate.filter";
    String REFERENCEABLE_NODES_FILTER_KEY = "node.referenceable.filter";
    String ORDERABLE_NODES_FILTER_KEY = "node.orderable.filter";

    String SYSTEM_SERVLET_ENABLED = "system.servlet.enabled";
    String PACKAGE_SERVLET_ENABLED = "package.servlet.enabled";
    String SECURITY_SERVLET_ENABLED = "security.servlet.enabled";
    String NODE_SERVLET_ENABLED = "node.servlet.enabled";
    String PROPERTY_SERVLET_ENABLED = "property.servlet.enabled";
    String VERSION_SERVLET_ENABLED = "version.servlet.enabled";

    long getQueryResultLimit();

    String[] getQueryTemplates();

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

    Dictionary getProperties();
}
