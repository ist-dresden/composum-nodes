package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;

import javax.servlet.Servlet;
import java.util.Dictionary;

/**
 * The configuration service for all servlets in the core bundle.
 */
public interface NodesConfiguration {

    String ERRORPAGE_STATUS = "errorpage.status";
    String ERRORPAGES_PATH = "errorpages.path";

    String CONSOLE_CATEGORIES_KEY = "console.categories";

    long QUERY_RESULT_LIMIT_DEFAULT = 500L;
    String QUERY_RESULT_LIMIT_KEY = "query.result.limit";
    String QUERY_TEMPLATES_KEY = "query.templates";

    String PAGE_NODE_FILTER_KEY = "node.page.filter";
    String DEFAULT_NODE_FILTER_KEY = "node.default.filter";
    String TREE_INTERMEDIATE_FILTER_KEY = "tree.intermediate.filter";
    String REFERENCEABLE_NODES_FILTER_KEY = "node.referenceable.filter";
    String ORDERABLE_NODES_FILTER_KEY = "node.orderable.filter";
    String SOURCE_NODES_FILTER_KEY = "node.source.filter";

    String SOURCE_SERVLET_ENABLED = "source.servlet.enabled";
    String PACKAGE_SERVLET_ENABLED = "package.servlet.enabled";
    String SECURITY_SERVLET_ENABLED = "security.servlet.enabled";
    String NODE_SERVLET_ENABLED = "node.servlet.enabled";
    String PROPERTY_SERVLET_ENABLED = "property.servlet.enabled";
    String VERSION_SERVLET_ENABLED = "version.servlet.enabled";
    String USER_MANAGEMENT_SERVLET_ENABLED = "usermanagement.servlet.enabled";

    String[] getConsoleCategories();

    long getQueryResultLimit();

    String[] getQueryTemplates();

    boolean isEnabled(Servlet servlet);

    ResourceFilter getPageNodeFilter();

    ResourceFilter getDefaultNodeFilter();

    ResourceFilter getTreeIntermediateFilter();

    ResourceFilter getReferenceableNodesFilter();

    ResourceFilter getOrderableNodesFilter();

    ResourceFilter getSourceNodesFilter();

    Dictionary getProperties();
}
