package com.composum.sling.nodes;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Dictionary;
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
                name = "Check Console Access",
                description = "if 'true' (checked) the access to the console pages is checked on servlet access"
        )
        boolean console_access_check() default true;

        @AttributeDefinition(
                name = "Console Categories",
                description = "the list of categories to determine the views in the core console"
        )
        String[] console_categories() default {"core", "system", "nodes", "users"};

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

    @NotNull
    private Configuration getConfig() {
        return Objects.requireNonNull(config, "NodesConfigImpl is not active");
    }

    @Override
    public boolean checkConsoleAccess() {
        return getConfig().console_access_check();
    }

    @Override
    @NotNull
    public String[] getConsoleCategories() {
        return getConfig().console_categories();
    }

    @Override
    public long getQueryResultLimit() {
        return getConfig().query_result_limit();
    }

    @Override
    @NotNull
    public ResourceFilter getPageNodeFilter() {
        return pageNodeFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getDefaultNodeFilter() {
        return defaultNodeFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getTreeIntermediateFilter() {
        return treeIntermediateFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getReferenceableNodesFilter() {
        return referenceableNodesFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getOrderableNodesFilter() {
        return orderableNodesFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getSourceNodesFilter() {
        return sourceNodesFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getSourceFolderNodesFilter(){
        return sourceFolderFilter;
    }

    @Override
    @NotNull
    public ResourceFilter getSourceXmlNodesFilter() {
        return sourceXmlFilter;
    }

    @Override
    public boolean isSourceAdvancedSortAttributes() {
        return sourceAdvancedSortAttributes;
    }

    @Override
    @NotNull
    public String getScenesContentRoot() {
        return getConfig().scene_content_root();
    }

    @Override
    @NotNull
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
    }

    @Deactivate
    protected void deactivate() {
        properties = null;
        config = null;
        pageNodeFilter = null;
        defaultNodeFilter = null;
        treeIntermediateFilter = null;
        referenceableNodesFilter = null;
        orderableNodesFilter = null;
        sourceNodesFilter = null;
    }
}
