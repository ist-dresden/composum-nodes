package com.composum.sling.core;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.NodeServlet;
import com.composum.sling.core.servlet.PackageServlet;
import com.composum.sling.core.servlet.PropertyServlet;
import com.composum.sling.core.servlet.SecurityServlet;
import com.composum.sling.core.servlet.SystemServlet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * The configuration service for all servlets in the core bundle.
 */
@Component(
        name = "ComposumCoreConfiguration",
        label = "Composum Core Configuration",
        description = "the configuration service for all servlets in the core bundle",
        immediate = true,
        metatype = true
)
@Service
public class CoreConfigImpl implements CoreConfiguration {

    public static final long QUERY_RESULT_LIMIT_DEFAULT = 500L;
    public static final String QUERY_RESULT_LIMIT_KEY = "query.result.limit";
    @Property(
            name = QUERY_RESULT_LIMIT_KEY,
            label = "Query Result Limit",
            description = "the maximum node count for query results (default: 500)",
            longValue = QUERY_RESULT_LIMIT_DEFAULT
    )
    private long queryResultLimit;

    public static final String PAGE_NODE_FILTER_KEY = "node.page.filter";
    @Property(
            name = PAGE_NODE_FILTER_KEY,
            label = "Content Page Filter",
            description = "the filter configuration to set the scope to the content pages",
            value = "or{ResourceType(+'^[a-z]+:.*[Pp]age$'),and{PrimaryType(+'^nt:file$'),MimeType(+'^text/html$')}}"
    )
    private ResourceFilter pageNodeFilter;

    public static final String DEFAULT_NODE_FILTER_KEY = "node.default.filter";
    @Property(
            name = DEFAULT_NODE_FILTER_KEY,
            label = "The default Node Filter",
            description = "the filter configuration to filter out system nodes",
            value = "and{Name(-'^rep:(repo)?[Pp]olicy$'),Path(-'^/system(/.*)?$,^/services(/.*)?$,^/bin(/.*)?$')}"
    )
    private ResourceFilter defaultNodeFilter;

    public static final String TREE_INTERMEDIATE_FILTER_KEY = "tree.intermediate.filter";
    @Property(
            name = TREE_INTERMEDIATE_FILTER_KEY,
            label = "Tree Intermediate (Folder) Filter",
            description = "the filter configuration to determine all intermediate nodes in the tree view",
            value = "or{Folder(),PrimaryType(+'^dam:Asset(Content)?$')}"
    )
    private ResourceFilter treeIntermediateFilter;

    public static final String REFERENCEABLE_NODES_FILTER_KEY = "node.referenceable.filter";
    @Property(
            name = REFERENCEABLE_NODES_FILTER_KEY,
            label = "Referenceable Nodes Filter",
            description = "the filter configuration to select reference target nodes",
            value = "MixinType(+'^mix:referenceable$')"
    )
    private ResourceFilter referenceableNodesFilter;

    public static final String ORDERABLE_NODES_FILTER_KEY = "node.orderable.filter";
    @Property(
            name = ORDERABLE_NODES_FILTER_KEY,
            label = "Orderable Nodes Filter",
            description = "the filter configuration to detect ordered nodes (prevent from sorting in the tree)",
            value = "or{MixinType(+'^mix:orderable$'),PrimaryType(+'^.*([Oo]rdered|[Pp]age).*$,^sling:(Mapping)$,^nt:(unstructured|frozenNode)$,^rep:(ACL|Members|system)$')}"
    )
    private ResourceFilter orderableNodesFilter;

    public static final String SYSTEM_SERVLET_ENABLED = "system.servlet.enabled";
    @Property(
            name = SYSTEM_SERVLET_ENABLED,
            label = "System Servlet",
            description = "the general on/off switch for the services of the System Servlet",
            boolValue = true
    )
    private boolean systemServletEnabled;

    public static final String PACKAGE_SERVLET_ENABLED = "package.servlet.enabled";
    @Property(
            name = PACKAGE_SERVLET_ENABLED,
            label = "Package Servlet",
            description = "the general on/off switch for the services of the Package Servlet",
            boolValue = true
    )
    private boolean packageServletEnabled;

    public static final String SECURITY_SERVLET_ENABLED = "security.servlet.enabled";
    @Property(
            name = SECURITY_SERVLET_ENABLED,
            label = "Security Servlet",
            description = "the general on/off switch for the services of the Security Servlet",
            boolValue = true
    )
    private boolean securityServletEnabled;

    public static final String NODE_SERVLET_ENABLED = "node.servlet.enabled";
    @Property(
            name = "node.servlet.enabled",
            label = "Node Servlet",
            description = "the general on/off switch for the services of the Node Servlet",
            boolValue = true
    )
    private boolean nodeServletEnabled;

    public static final String PROPERTY_SERVLET_ENABLED = "property.servlet.enabled";
    @Property(
            name = "property.servlet.enabled",
            label = "Property Servlet",
            description = "the general on/off switch for the services of the Property Servlet",
            boolValue = true
    )
    private boolean propertyServletEnabled;

    private Map<Class<? extends AbstractServiceServlet>, Boolean> enabledServlets;

    @Override
    public boolean isEnabled(AbstractServiceServlet servlet) {
        Boolean result = enabledServlets.get(servlet.getClass());
        return result != null ? result : false;
    }

    @Override
    public long getQueryResultLimit() {
        return queryResultLimit;
    }

    @Override
    public ResourceFilter getPageNodeFilter() {
        return pageNodeFilter;
    }

    @Override
    public ResourceFilter getDefaultNodeFilter() {
        return defaultNodeFilter;
    }

    @Override
    public ResourceFilter getTreeIntermediateFilter() {
        return treeIntermediateFilter;
    }

    @Override
    public ResourceFilter getReferenceableNodesFilter() {
        return referenceableNodesFilter;
    }

    @Override
    public ResourceFilter getOrderableNodesFilter() {
        return orderableNodesFilter;
    }

    protected Dictionary properties;

    protected void activate(ComponentContext context) {
        this.properties = context.getProperties();
        queryResultLimit = PropertiesUtil.toLong(QUERY_RESULT_LIMIT_KEY, QUERY_RESULT_LIMIT_DEFAULT);
        pageNodeFilter = ResourceFilterMapping.fromString(
                (String) properties.get(PAGE_NODE_FILTER_KEY));
        defaultNodeFilter = ResourceFilterMapping.fromString(
                (String) properties.get(DEFAULT_NODE_FILTER_KEY));
        treeIntermediateFilter = ResourceFilterMapping.fromString(
                (String) properties.get(TREE_INTERMEDIATE_FILTER_KEY));
        referenceableNodesFilter = ResourceFilterMapping.fromString(
                (String) properties.get(REFERENCEABLE_NODES_FILTER_KEY));
        orderableNodesFilter = ResourceFilterMapping.fromString(
                (String) properties.get(ORDERABLE_NODES_FILTER_KEY));
        enabledServlets = new HashMap<>();
        enabledServlets.put(SystemServlet.class, systemServletEnabled =
                (Boolean) properties.get(SYSTEM_SERVLET_ENABLED));
        enabledServlets.put(PackageServlet.class, packageServletEnabled =
                (Boolean) properties.get(PACKAGE_SERVLET_ENABLED));
        enabledServlets.put(SecurityServlet.class, securityServletEnabled =
                (Boolean) properties.get(SECURITY_SERVLET_ENABLED));
        enabledServlets.put(NodeServlet.class, nodeServletEnabled =
                (Boolean) properties.get(NODE_SERVLET_ENABLED));
        enabledServlets.put(PropertyServlet.class, propertyServletEnabled =
                (Boolean) properties.get(PROPERTY_SERVLET_ENABLED));
    }

    protected void deactivate(ComponentContext context) {
        this.properties = null;
    }
}
