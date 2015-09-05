package com.composum.sling.core.util;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rw on 26.02.15.
 */
public class PropertyUtil {

    public static final String FORBIDDEN_NAME_CHARS = "/";

    public static <T> Class<T> getType(T defaultValue) {
        Class<T> type;
        if (defaultValue != null) {
            type = (Class<T>) defaultValue.getClass();
        } else {
            type = (Class<T>) String.class;
        }
        return type;
    }

    /**
     * FIXME(rw,2015-04-22) not useful in the core layer
     *
     * @param name
     * @return
     */
    public static String manglePropertyName(String name) {
        if (name != null && name.length() > 0) {
            StringBuilder builder = new StringBuilder();
            int length = name.length();
            char c = name.charAt(0);
            if (c >= '0' && c <= '9') {
                builder.append('_'); // don't start with a digit
            }
            for (int i = 0; i < length; i++) {
                c = name.charAt(i);
                if (c > ' ' && FORBIDDEN_NAME_CHARS.indexOf(c) < 0) {
                    builder.append(c);
                } else {
                    builder.append('_');
                }
            }
            name = builder.toString();
            name = StringEscapeUtils.escapeEcmaScript(name); // prevent from scripting in names
        } else {
            name = "_";
        }
        return name;
    }

    public static Binary getBinaryData(Node node) {
        Binary result = null;
        if (node != null) {
            try {
                Property property = null;
                try {
                    property = node.getProperty(ResourceUtil.PROP_DATA);
                    result = property.getBinary();
                } catch (PathNotFoundException pnfex) {
                    Node contentNode = node.getNode(ResourceUtil.CONTENT_NODE);
                    property = contentNode.getProperty(ResourceUtil.PROP_DATA);
                    result = property.getBinary();
                }
            } catch (RepositoryException rex) {
                // ok, property doesn't exist
            }
        }
        return result;
    }

    // some 'property' strategies

    /** some property names with special functions or filters */
    public static final String PROP_PRIMARY_TYPE = "jcr:primaryType";
    public static final String PROP_MIXIN_TYPES = "jcr:mixinTypes";

    /**  */
    public static final SetPropertyStrategy DEFAULT_PROPERTY_STRATEGY = new SetPropertyStrategy.Property();

    /**  */
    public static final Map<String, SetPropertyStrategy> SET_PROPERTY_STRATEGY_MAP;

    static {
        SET_PROPERTY_STRATEGY_MAP = new HashMap<String, SetPropertyStrategy>();
        SET_PROPERTY_STRATEGY_MAP.put(PROP_PRIMARY_TYPE, new SetPropertyStrategy.PrimaryType());
        SET_PROPERTY_STRATEGY_MAP.put(PROP_MIXIN_TYPES, new SetPropertyStrategy.MixinTypes());
    }

    /**
     * @param name
     * @return
     */
    public static SetPropertyStrategy getSetPropertyStrategy(String name) {
        SetPropertyStrategy strategy = SET_PROPERTY_STRATEGY_MAP.get(name);
        return strategy != null ? strategy : DEFAULT_PROPERTY_STRATEGY;
    }

    /**
     * @param node
     * @param name
     * @param value
     * @param type
     * @return
     * @throws RepositoryException
     */
    public static Property setProperty(Node node, String name, Value value, int type)
            throws RepositoryException {
        return getSetPropertyStrategy(name).setProperty(node, name, value, type);
    }

    /**
     * @param node
     * @param name
     * @param values
     * @param type
     * @return
     * @throws RepositoryException
     */
    public static Property setProperty(Node node, String name, Value[] values, int type)
            throws RepositoryException {
        return getSetPropertyStrategy(name).setProperty(node, name, values, type);
    }

    /**
     * @param node
     * @param name
     * @param input
     * @return
     * @throws RepositoryException
     */
    public static Property setProperty(Node node, String name, InputStream input) throws RepositoryException {
        Property property = null;
        if (input != null) {
            Session session = node.getSession();
            ValueFactory valueFactory = session.getValueFactory();
            Binary binary = valueFactory.createBinary(input);
            try {
                property = node.setProperty(name, valueFactory.createValue(binary));
            } finally {
                binary.dispose();
            }
        }
        return property;
    }

    /**
     * @param node
     * @param name
     * @param values
     * @param type
     * @throws RepositoryException
     */
    public static Property setProperty(Node node, String name, Iterable<?> values, int type)
            throws RepositoryException {
        Session session = node.getSession();
        ValueFactory factory = session.getValueFactory();
        List<Value> valueList = new ArrayList<>();
        for (Object value : values) {
            Value jcrValue = createValue(factory, value, type);
            if (jcrValue != null) {
                valueList.add(jcrValue);
            }
        }
        Value[] valueArray = valueList.toArray(new Value[valueList.size()]);
        return setProperty(node, name, valueArray, type);
    }

    /**
     * @param node
     * @param name
     * @param value
     * @param type
     * @throws RepositoryException
     */
    public static Property setProperty(Node node, String name, Object value, int type)
            throws RepositoryException {
        Session session = node.getSession();
        ValueFactory factory = session.getValueFactory();
        return setProperty(node, name, createValue(factory, value, type), type);
    }

    public static Value createValue(ValueFactory factory, Object value, int type)
            throws ValueFormatException {
        Value jcrValue = null;
        if (value != null) {
            switch (type) {
                case PropertyType.BOOLEAN:
                    jcrValue = factory.createValue((boolean) value);
                    break;
                case PropertyType.DATE:
                    jcrValue = factory.createValue((Calendar) value);
                    break;
                case PropertyType.DECIMAL:
                    jcrValue = factory.createValue((BigDecimal) value);
                    break;
                case PropertyType.DOUBLE:
                    jcrValue = factory.createValue((double) value);
                    break;
                case PropertyType.LONG:
                    jcrValue = factory.createValue((long) value);
                    break;
                default:
                    jcrValue = factory.createValue((String) value, type);
                    break;
            }
        }
        return jcrValue;
    }
}
