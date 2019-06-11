package com.composum.sling.core.util;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.jcr.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

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

    public static String getProperty(Node node, String name, String defaultValue)
            throws RepositoryException {
        String value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getString();
        }
        return value != null ? value : defaultValue;
    }

    public static Boolean getProperty(Node node, String name, Boolean defaultValue)
            throws RepositoryException {
        Boolean value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getBoolean();
        }
        return value != null ? value : defaultValue;
    }

    public static Long getProperty(Node node, String name, Long defaultValue)
            throws RepositoryException {
        Long value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getLong();
        }
        return value != null ? value : defaultValue;
    }

    public static Calendar getProperty(Node node, String name, Calendar defaultValue)
            throws RepositoryException {
        Calendar value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getDate();
        }
        return value != null ? value : defaultValue;
    }

    public static BigDecimal getProperty(Node node, String name, BigDecimal defaultValue)
            throws RepositoryException {
        BigDecimal value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getDecimal();
        }
        return value != null ? value : defaultValue;
    }

    public static Double getProperty(Node node, String name, Double defaultValue)
            throws RepositoryException {
        Double value = null;
        if (node.hasProperty(name)) {
            Property property = node.getProperty(name);
            value = property.getDouble();
        }
        return value != null ? value : defaultValue;
    }

    /**
     * FIXME(rw,2015-04-22) not useful in the core layer
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

    /** The most appropriate Java type for a {@link PropertyType}. */
    protected static final Map<Integer, Class> DEFAULT_PROPERTY_TYPES = new HashMap<Integer, Class>() {{
        put(PropertyType.STRING, String.class);
        put(PropertyType.BINARY, Binary.class);
        put(PropertyType.LONG, Long.class);
        put(PropertyType.DOUBLE, Double.class);
        put(PropertyType.DATE, Calendar.class);
        put(PropertyType.BOOLEAN, Boolean.class);
        put(PropertyType.NAME, String.class);
        put(PropertyType.PATH, String.class);
        put(PropertyType.REFERENCE, String.class);
        put(PropertyType.WEAKREFERENCE, String.class);
        put(PropertyType.URI, java.net.URI.class);
        put(PropertyType.DECIMAL, BigDecimal.class);
    }};

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

    /** Reads the value of a property as the given type. */
    public static <T> T readValue(Value value, Class<T> type) throws RepositoryException {
        try {
            if (null == value) return null;
            if (type.isAssignableFrom(value.getClass())) return type.cast(value);

            if (Long.class.equals(type)) return type.cast(value.getLong());
            if (Integer.class.equals(type)) return type.cast(value.getLong());
            if (Short.class.equals(type)) return type.cast((short) value.getLong());
            if (Byte.class.equals(type)) return type.cast((byte) value.getLong());
            if (Float.class.equals(type)) return type.cast((float) value.getLong());
            if (Double.class.equals(type)) return type.cast(value.getDouble());
            if (String.class.equals(type)) return type.cast(value.getString());
            if (Boolean.class.equals(type)) return type.cast(value.getBoolean());
            if (java.net.URI.class.equals(type)) return type.cast(new URI(value.getString()));
            if (java.net.URL.class.equals(type)) return type.cast(new URL(value.getString()));

            if (Date.class.equals(type)) return type.cast(value.getDate().getTime());
            if (Calendar.class.equals(type)) return type.cast(value.getDate());
            if (BigDecimal.class.equals(type)) return type.cast(value.getDecimal());
            if (Binary.class.equals(type)) return type.cast(value.getBinary());
            if (InputStream.class.equals(type)) return type.cast(value.getBinary().getStream());

            Class defaultType = DEFAULT_PROPERTY_TYPES.get(value.getType());
            if (null != defaultType && type.isAssignableFrom(defaultType))
                return type.cast((T) readValue(value, defaultType));

            throw new IllegalArgumentException("Type " + type + " not supported yet.");
        } catch (URISyntaxException | MalformedURLException | RuntimeException e) {
            throw new ValueFormatException("Can't convert to " + type, e);
        }
    }
}
