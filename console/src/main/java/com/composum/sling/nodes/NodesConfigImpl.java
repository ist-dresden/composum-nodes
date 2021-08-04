package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.nodes.servlet.ComponentsServlet;
import com.composum.sling.nodes.servlet.NodeServlet;
import com.composum.sling.nodes.servlet.PropertyServlet;
import com.composum.sling.nodes.servlet.SceneServlet;
import com.composum.sling.nodes.servlet.SecurityServlet;
import com.composum.sling.nodes.servlet.SourceServlet;
import com.composum.sling.nodes.servlet.SourceUpdateServlet;
import com.composum.sling.nodes.servlet.VersionServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The configuration service for all servlets in the core bundle.
 */
@Component(
        service = NodesConfiguration.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes (Console) Configuration"
        },
        immediate = true
)
@Designate(ocd = NodesConfigImpl.Configuration.class)
public class NodesConfigImpl implements NodesConfiguration {

    /**
     * Configuration object for {@link NodesConfiguration}. The method naming is due to backwards compatibility.
     */
    @ObjectClassDefinition(name = "Composum Nodes (Console) Configuration", description = "the configuration service for all servlets in the nodes bundles")
    public @interface Configuration {

        @AttributeDefinition(
                name = "Composum Path",
                description = "the path to the Composum Nodes application"
        )
        String composum_nodes_path() default "/libs/composum/nodes";

        @AttributeDefinition(
                name = "Check Console Access",
                description = "if 'true' (checked) the access to the console pages is checked on servlet access"
        )
        boolean console_access_check() default true;

        @AttributeDefinition(
                name = "Console Categories",
                description = "the list of categories to determine the views in the core console"
        )
        String[] console_categories() default {"core", "nodes"};

        @AttributeDefinition(
                name = "Query Result Limit",
                description = "the maximum node count for query results (default: 500)"
        )
        long query_result_limit() default 500L;

        // FIXME(hps,25.05.20) should that be used somehow? It isn't now. Relation to CoreConfig-errorpages?
        @AttributeDefinition(
                name = "Errorpages",
                description = "the path to the errorpages; e.g. 'meta/errorpages' for searching errorpages along the requested path"
        )
        String errorpages_Path() default "meta/errorpages";

        @AttributeDefinition(
                name = "Content Page Filter",
                description = "the filter configuration to set the scope to the content pages"
        )
        String node_page_filter() default "or{ResourceType(+'^[a-z]+:.*([Ss]ite|[Pp]age)$'),and{PrimaryType(+'^nt:file$'),MimeType(+'^text/html$')}}";

        @AttributeDefinition(
                name = "The default Node Filter",
                description = "the filter configuration to filter out system nodes"
        )
        String node_default_filter() default "and{Name(-'^rep:(repo)?[Pp]olicy$'),Path(-'^/bin(/.*)?$,^/services(/.*)?$,^/servlet(/.*)?$,^/(jcr:)?system(/.*)?$')}";

        @AttributeDefinition(
                name = "Tree Intermediate (Folder) Filter",
                description = "the filter configuration to determine all intermediate nodes in the tree view"
        )
        String tree_intermediate_filter() default "or{Folder(),PrimaryType(+'^dam:Asset(Content)?$')}";

        @AttributeDefinition(
                name = "Referenceable Nodes Filter",
                description = "the filter configuration to select reference target nodes"
        )
        String node_referenceable_filter() default "Type(mix:referenceable)";

        @AttributeDefinition(
                name = "Orderable Nodes Filter",
                description = "the filter configuration to detect ordered nodes (prevent from sorting in the tree)"
        )
        String node_orderable_filter() default "or{Type(node:orderable),PrimaryType(+'^.*([Oo]rdered|[Pp]age).*$,^sling:(Mapping)$,^nt:(unstructured|frozenNode)$,^rep:(ACL|Members|system)$')}";

        @AttributeDefinition(
                name = "XML Source Nodes Filter",
                description = "the filter configuration for the source export of the repository content (Source Servlet) that limits which nodes are exported"
        )
        String node_source_filter() default "PrimaryType(-'^cpp:(Statistics)$,^rep:(.+)$')";

        @AttributeDefinition(
                name = "XML Source Folder Filter",
                description = "the filter configuration for the source export of the repository content (Source Servlet) that determines which nodes are exported as folder"
        )
        String node_source_folder_filter() default "or{NodeType(+'^nt:hierarchyNode$,^vlt:HierarchyNode$'),Name(+'^cq:dialog$,^cq:htmlTag$,^cq:template$,^cq:design_dialog$,^cq:childEditConfig$')}";

        @AttributeDefinition(
                name = "XML Source Folder Filter",
                description = "the filter configuration for the source export of the repository content (Source Servlet) that determines which nodes are exported as separate XML file (aka vlt:FullCoverage)"
        )
        // The default is done mostly like
        // https://github.com/apache/jackrabbit-filevault/blob/trunk/vault-core/src/main/resources/org/apache/jackrabbit/vault/fs/config/defaultConfig-1.1.xml
        String node_source_xml_filter() default "NodeType(+'^vlt:FullCoverage$,^mix:language$,^rep:AccessControl$,^rep:Policy$,^cq:EditConfig$,^cq:WorkflowModel$,^sling:OsgiConfig$')";

        @AttributeDefinition(
                name = "XML Source Advanced Attribute Sort",
                description = "the filter configuration for the source export of the repository content (Source Servlet) that determines whether the attributes are sorted by importance (jcr:primaryType, jcr:mixins, sling:* and then the rest"
        )
        boolean node_source_advanced_attributesort() default true;

        @AttributeDefinition(
                name = "Scenes Content Root",
                description = "the root path of the scenes content nodes"
        )
        String scene_content_root() default "/var/composum/nodes/scenes";

        @AttributeDefinition(
                name = "Package Servlet",
                description = "the general on/off switch for the services of the Package Servlet"
        )
        boolean package_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Security Servlet",
                description = "the general on/off switch for the services of the Security Servlet"
        )
        boolean security_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Node Servlet",
                description = "the general on/off switch for the services of the Node Servlet"
        )
        boolean node_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Property Servlet",
                description = "the general on/off switch for the services of the Property Servlet"
        )
        boolean property_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Version Servlet",
                description = "the general on/off switch for the services of the Version Servlet"
        )
        boolean version_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Scene Servlet",
                description = "the general on/off switch for the services of the Scene Servlet"
        )
        boolean scene_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Components Servlet",
                description = "the general on/off switch for the services of the Components Servlet"
        )
        boolean components_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Source Servlet",
                description = "the general on/off switch for the services of the Source Servlet"
        )
        boolean source_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Source Update Servlet",
                description = "the general on/off switch for the services of the Source Update Servlet"
        )
        boolean sourceupdate_servlet_enabled() default true;

        @AttributeDefinition(
                name = "User Management Servlet",
                description = "the general on/off switch for the services of the User Management Servlet"
        )
        boolean usermanagement_servlet_enabled() default true;

    }

    private volatile Configuration config;

    private volatile ResourceFilter pageNodeFilter;

    private volatile ResourceFilter defaultNodeFilter;

    private volatile ResourceFilter treeIntermediateFilter;

    private volatile ResourceFilter referenceableNodesFilter;

    private volatile ResourceFilter orderableNodesFilter;

    private volatile ResourceFilter sourceNodesFilter;

    private volatile ResourceFilter sourceFolderFilter;

    private volatile ResourceFilter sourceXmlFilter;

    private volatile boolean sourceAdvancedSortAttributes;

    private volatile Map<String, Boolean> enabledServlets;

    @Nonnull
    private Configuration getConfig() {
        return Objects.requireNonNull(config, "NodesConfigImpl is not active");
    }

    @Override
    public boolean isEnabled(Servlet servlet) {
        return enabledServlets != null && Boolean.TRUE.equals(enabledServlets.get(servlet.getClass().getSimpleName()));
    }

    @Override
    public String getApplicationPath() {
        return getConfig().composum_nodes_path();
    }

    @Override
    public boolean checkConsoleAccess() {
        return getConfig().console_access_check();
    }

    @Override
    public String[] getConsoleCategories() {
        return getConfig().console_categories();
    }

    @Override
    public long getQueryResultLimit() {
        return getConfig().query_result_limit();
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

    @Override
    public ResourceFilter getSourceNodesFilter() {
        return sourceNodesFilter;
    }

    @Override
    public ResourceFilter getSourceFolderNodesFilter(){
        return sourceFolderFilter;
    }

    @Override
    public ResourceFilter getSourceXmlNodesFilter() {
        return sourceXmlFilter;
    }

    @Override
    public boolean isSourceAdvancedSortAttributes() {
        return sourceAdvancedSortAttributes;
    }

    @Override
    @Nonnull
    public String getScenesContentRoot() {
        return getConfig().scene_content_root();
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    protected volatile Dictionary<String, Object> properties;

    @Activate
    @Modified
    public void activate(ComponentContext context, Configuration configuration) {
        this.config = configuration;
        this.properties = context.getProperties();
        pageNodeFilter = ResourceFilterMapping.fromString(configuration.node_page_filter());
        defaultNodeFilter = ResourceFilterMapping.fromString(configuration.node_default_filter());
        treeIntermediateFilter = ResourceFilterMapping.fromString(configuration.tree_intermediate_filter());
        referenceableNodesFilter = ResourceFilterMapping.fromString(configuration.node_referenceable_filter());
        orderableNodesFilter = ResourceFilterMapping.fromString(configuration.node_orderable_filter());
        sourceNodesFilter = ResourceFilterMapping.fromString(configuration.node_source_filter());
        sourceFolderFilter = ResourceFilterMapping.fromString(configuration.node_source_folder_filter());
        sourceXmlFilter = ResourceFilterMapping.fromString(configuration.node_source_xml_filter());
        sourceAdvancedSortAttributes = configuration.node_source_advanced_attributesort();
        Map<String, Boolean> theEnabledServlets = new HashMap<>();
        theEnabledServlets.put("PackageServlet", configuration.package_servlet_enabled());
        theEnabledServlets.put(SecurityServlet.class.getSimpleName(), configuration.security_servlet_enabled());
        theEnabledServlets.put(NodeServlet.class.getSimpleName(), configuration.node_servlet_enabled());
        theEnabledServlets.put(PropertyServlet.class.getSimpleName(), configuration.property_servlet_enabled());
        theEnabledServlets.put(VersionServlet.class.getSimpleName(), configuration.version_servlet_enabled());
        theEnabledServlets.put(SceneServlet.class.getSimpleName(), configuration.scene_servlet_enabled());
        theEnabledServlets.put(ComponentsServlet.class.getSimpleName(), configuration.components_servlet_enabled());
        theEnabledServlets.put(SourceServlet.class.getSimpleName(), configuration.source_servlet_enabled());
        theEnabledServlets.put(SourceUpdateServlet.class.getSimpleName(), configuration.sourceupdate_servlet_enabled());
        theEnabledServlets.put("UserManagementServlet", configuration.usermanagement_servlet_enabled());
        this.enabledServlets = theEnabledServlets;
    }

    @Deactivate
    protected void deactivate() {
        properties = null;
        config = null;
        enabledServlets = null;
        pageNodeFilter = null;
        defaultNodeFilter = null;
        treeIntermediateFilter = null;
        referenceableNodesFilter = null;
        orderableNodesFilter = null;
        sourceNodesFilter = null;
    }
}
