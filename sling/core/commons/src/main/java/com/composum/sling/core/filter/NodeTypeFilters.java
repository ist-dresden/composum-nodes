package com.composum.sling.core.filter;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.HashMap;
import java.util.Map;

public class NodeTypeFilters {

    private static final Logger LOG = LoggerFactory.getLogger(NodeTypeFilters.class);

    public static final String NODE_TYPE_PREFIX = "node:";

    public static final String TYPE_ORDERABLE = NODE_TYPE_PREFIX + "orderable";

    /**
     * Provides a set of predefined filters whose name start with {@value #NODE_TYPE_PREFIX}
     * - e.g. {@value TYPE_ORDERABLE} for {@link OrderableTypeFilter} that work on the {@link NodeType}s of a resource.
     */
    public static boolean accept(Resource resource, String type) {
        try {
            if (resource != null) {
                NodeTypeFilter filter = TYPE_FILTER_SET.get(type);
                if (filter != null) {
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        if (accept(filter, node.getPrimaryNodeType())) {
                            return true;
                        }
                        NodeType[] mixins = node.getMixinNodeTypes();
                        for (NodeType mixinType : mixins) {
                            if (accept(filter, mixinType)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return false;
    }

    protected static boolean accept(NodeTypeFilter filter, NodeType nodeType) {
        if (nodeType != null) {
            if (filter.accept(nodeType)) {
                return true;
            }
            NodeType[] supertypes = nodeType.getDeclaredSupertypes();
            for (NodeType superType : supertypes) {
                if (accept(filter, superType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<String, NodeTypeFilter> TYPE_FILTER_SET;

    static {
        TYPE_FILTER_SET = new HashMap<>();
        TYPE_FILTER_SET.put(TYPE_ORDERABLE, new OrderableTypeFilter());
    }

    public interface NodeTypeFilter {
        boolean accept(NodeType type);
    }

    public static class OrderableTypeFilter implements NodeTypeFilter {
        @Override
        public boolean accept(NodeType type) {
            return type.hasOrderableChildNodes();
        }
    }
}
