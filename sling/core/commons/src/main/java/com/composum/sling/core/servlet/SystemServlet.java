package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.collections.map.LRUMap;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The service servlet to retrieve all general system settings.
 */
@SlingServlet(
        paths = "/bin/core/system",
        methods = {"GET", "PUT"}
)
public class SystemServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SystemServlet.class);

    public static final String NODE_TYPES_PATH = "/jcr:system/jcr:nodeTypes";

    public static final String PROP_IS_MIXIN = "jcr:isMixin";

    public static final String ALL_QUERY_KEY = "-- all --";

    @Reference
    private CoreConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {json}

    public enum Operation {propertyTypes, primaryTypes, mixinTypes, profile}

    protected ServletOperationSet operations = new ServletOperationSet(Extension.json);

    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.propertyTypes, new GetPropertyTypes());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.primaryTypes, new GetPrimaryTypes());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.mixinTypes, new GetMixinTypes());
    }

    //
    // Property types
    //

    public static final String[] PROPERTY_TYPES = new String[]{
            "String",
            "Boolean",
            "Long",
            "Date",
            "Binary",
            "Decimal",
            "Double",
            "Name",
            "Path",
            "URI",
            "Reference",
            "WeakReference"
    };

    public class GetPropertyTypes implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
            response.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.writeJsonArray(jsonWriter, PROPERTY_TYPES);
        }
    }

    //
    // Node types
    //

    /** the general filter for mixin types */
    public static class MixinTypesFilter implements ResourceFilter {

        @Override
        public boolean accept(Resource resource) {
            ResourceHandle handle = ResourceHandle.use(resource);
            Boolean isMixin = handle.getProperty(PROP_IS_MIXIN, false);
            return isMixin;
        }

        @Override
        public boolean isRestriction() {
            return false;
        }

        @Override
        public void toString(StringBuilder builder) {
            builder.append("MixinTypes");
        }
    }

    /** the general filter for primary types is a inversion ot the mixin types filter */
    public static class PrimaryTypesFilter extends MixinTypesFilter {

        @Override
        public boolean accept(Resource resource) {
            return !super.accept(resource);
        }

        @Override
        public void toString(StringBuilder builder) {
            builder.append("PrimaryTypes");
        }
    }

    /** the general filter for primary types */
    protected ResourceFilter PRIMARY_TYPE_FILTER = new PrimaryTypesFilter();

    /** the cache for primary type queries and their results */
    protected LRUMap PRIMARY_TYPE_CACHE = new LRUMap();

    /**
     * the 'system.primaryTypes.json' operation send a JSON array with available primary types
     * a 'query' parameter is used to restrict the result to type names with the query string
     */
    public class GetPrimaryTypes extends GetNodeTypes {

        public GetPrimaryTypes() {
            super(PRIMARY_TYPE_FILTER, PRIMARY_TYPE_CACHE);
        }
    }

    /** the general filter for mixin types */
    protected ResourceFilter MIXIN_TYPE_FILTER = new MixinTypesFilter();

    /** the static cache for mixin type queries and their results */
    protected LRUMap MIXIN_TYPE_CACHE = new LRUMap();

    /**
     * the 'system.mixinTypes.json' operation send a JSON array with available mixin types
     * a 'query' parameter is used to restrict the result to type names with the query string
     */
    public class GetMixinTypes extends GetNodeTypes {

        public GetMixinTypes() {
            super(MIXIN_TYPE_FILTER, MIXIN_TYPE_CACHE);
        }
    }

    /**
     * the general node types Query and Caching implementation
     */
    public class GetNodeTypes implements ServletOperation {

        private ResourceFilter typeFilter;
        private LRUMap queryCache;

        public GetNodeTypes(ResourceFilter nameFilter, LRUMap cache) {
            this.typeFilter = nameFilter;
            this.queryCache = cache;
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                String query = request.getParameter(PARAM_QUERY);

                ResourceResolver resolver = request.getResourceResolver();
                List<String> nodeTypes = (List<String>) this.queryCache.get(query == null ? ALL_QUERY_KEY : query);
                if (nodeTypes == null) {
                    nodeTypes = getNodeTypes(resolver, query != null ? query.toLowerCase() : null);
                    this.queryCache.put(query == null ? ALL_QUERY_KEY : query, nodeTypes);
                }

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                response.setStatus(HttpServletResponse.SC_OK);

                JsonUtil.writeJsonArray(jsonWriter, nodeTypes.iterator());

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

        public List<String> getNodeTypes(ResourceResolver resolver, String query) throws RepositoryException {

            List<String> nodeTypes = new ArrayList<String>();
            Resource typesResource = resolver.getResource(NODE_TYPES_PATH);
            for (Resource type : typesResource.getChildren()) {
                String name = type.getName();
                if (this.typeFilter.accept(type) &&
                        (query == null || name.toLowerCase().contains(query))) {
                    nodeTypes.add(name);
                }
            }
            Collections.sort(nodeTypes);
            return nodeTypes;
        }
    }
}