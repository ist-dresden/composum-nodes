package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.List;

/**
 * the mapping interface and their implementations to change some protected 'properties'
 * like mixin types and the primary node type
 */
public interface SetPropertyStrategy {

    javax.jcr.Property setProperty(Node node, String name, Value value, int type)
            throws RepositoryException;

    javax.jcr.Property setProperty(Node node, String name, Value[] values, int type)
            throws RepositoryException;

    class Property implements SetPropertyStrategy {

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value value, int type)
                throws RepositoryException {
            return node.setProperty(name, value, type);
        }

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value[] values, int type)
                throws RepositoryException {
            return node.setProperty(name, values, type);
        }
    }

    class PrimaryType implements SetPropertyStrategy {

        private static final Logger LOG = LoggerFactory.getLogger(PrimaryType.class);

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value value, int type)
                throws RepositoryException {
            if (type != PropertyType.NAME) {
                LOG.warn("primary type must be of property type NAME, was '"
                        + PropertyType.nameFromValue(type) + "'");
            }
            node.setPrimaryType(value.getString());
            return null;
        }

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value[] values, int type)
                throws RepositoryException {
            throw new RepositoryException("invalid multi value for primary type");
        }
    }

    class MixinTypes implements SetPropertyStrategy {

        private static final Logger LOG = LoggerFactory.getLogger(MixinTypes.class);

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value value, int type)
                throws RepositoryException {
            throw new RepositoryException("invalid single value for multi value mixin types");
        }

        @Override
        public javax.jcr.Property setProperty(Node node, String name, Value[] values, int type)
                throws RepositoryException {
            if (type != PropertyType.NAME) {
                LOG.warn("mixin types must be of property type NAME, was '"
                        + PropertyType.nameFromValue(type) + "'");
            }
            List<String> valueList = new ArrayList<>();
            if (values != null) {
                for (Value val : values) {
                    String mixin = val.getString();
                    if (StringUtils.isNotBlank(mixin)) {
                        valueList.add(mixin);
                    }
                }
            }
            NodeType[] mixins = node.getMixinNodeTypes();
            for (NodeType mixin : mixins) {
                String key = mixin.getName();
                if (valueList.contains(key)) {
                    valueList.remove(key);
                } else {
                    node.removeMixin(key);
                }
            }
            for (String key : valueList) {
                node.addMixin(key);
            }
            return null;
        }
    }
}
