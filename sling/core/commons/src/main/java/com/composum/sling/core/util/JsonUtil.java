package com.composum.sling.core.util;

import com.composum.sling.core.exception.PropertyValueFormatException;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * The utility class to transform JCR object and Sling resources into JSON and update such objects using JSON data.
 */
public class JsonUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    /** the declared builder for JSON POJO mapping */
    public static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    /**
     * the structure for parsing property values from JSON using Gson
     */
    public static class JsonProperty {

        public String name;
        public Object value; // can be a 'String' or a list of 'String' (multi value)
        public String type = PropertyType.nameFromValue(PropertyType.STRING);
        public boolean multi = false;
    }

    //
    // ==== the Sling resource level ===========================================
    //
    // - wrapping for structured objects stored in the repository
    //

    //
    // export resource structures as JSON objects
    //

    /**
     * Writes a resources JSON view to a writer using the default application rules for filtering.
     *
     * @param writer   the writer for the JSON transformation
     * @param resource the resource to transform
     * @throws RepositoryException
     * @throws IOException
     */
    public static void exportJson(JsonWriter writer, Resource resource)
            throws RepositoryException, IOException {
        exportJson(writer, resource, MappingRules.DEFAULT_MAPPING_RULES);
    }

    /**
     * @param writer
     * @param resource
     * @param rules
     * @throws RepositoryException
     * @throws IOException
     */
    public static void exportJson(JsonWriter writer, Resource resource, MappingRules rules)
            throws RepositoryException, IOException {
        exportJson(writer, resource, rules, 1);
    }

    /**
     * @param writer
     * @param resource
     * @param rules
     * @param depth
     * @throws RepositoryException
     * @throws IOException
     */
    public static void exportJson(JsonWriter writer, Resource resource, MappingRules rules, int depth)
            throws RepositoryException, IOException {

        if (resource != null) {

            writer.beginObject();

            exportProperties(writer, resource, rules);

            // export children after the properties(!) if depth is not reached or not restricted
            if (rules.maxDepth == 0 || depth < rules.maxDepth) {
                depth++;
                for (Resource child : resource.getChildren()) {
                    if (rules.resourceFilter.accept(child)) {
                        writer.name(child.getName());
                        exportJson(writer, child, rules, depth);
                    }
                }
            }

            writer.endObject();
        }
    }

    /**
     * @param writer
     * @param resource
     * @param rules
     * @throws RepositoryException
     * @throws IOException
     */
    public static void exportProperties(JsonWriter writer, Resource resource, MappingRules rules)
            throws RepositoryException, IOException {

        Node node = resource.adaptTo(Node.class);

        // property collection for a sorted output
        TreeMap<String, Object> propertiesSet = new TreeMap<>();
        if (node != null) {
            // retrieve properties from the resources repository node
            PropertyIterator iterator = node.getProperties();
            while (iterator.hasNext()) {
                Property property = iterator.nextProperty();
                propertiesSet.put(property.getName(), property);
            }
        } else {
            // for synthetic resources use the synthetic values from the resources value map
            ValueMap properties = ResourceUtil.getValueMap(resource);
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                propertiesSet.put(entry.getKey(), entry.getValue());
            }
        }

        // write properties first to ensure that types are read first on import
        if (rules.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
            writer.name(MappingRules.PROPERTIES_NAME);
            writer.beginArray();
        }
        for (Map.Entry<String, Object> entry : propertiesSet.entrySet()) {
            String name = entry.getKey();
            if (rules.exportPropertyFilter.accept(name)) {
                Object value = entry.getValue();
                if (value instanceof Property) {
                    Property property = (Property) value;
                    writeJsonProperty(writer, node, property, rules.propertyFormat);
                } else {
                    // if no node exists (synthetic resource) the properties are simple values
                    writeJsonProperty(writer, name, value, rules.propertyFormat);
                }
            }
        }
        if (rules.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
            writer.endArray();
        }
    }

    /**
     * Writes the names of all children (except the 'jcr:content' child) into one array value
     * named '_child_order_' as a hint to recalculate the original order of the children.
     *
     * @param writer
     * @param resource
     * @throws IOException
     */
    public static void exportChildOrderProperty(JsonWriter writer, Resource resource)
            throws IOException {
        List<String> names = new ArrayList<>();
        Iterable<Resource> children = resource.getChildren();
        for (Resource child : children) {
            String name = child.getName();
            if (!ResourceUtil.CONTENT_NODE.equals(name)) {
                names.add(name);
            }
        }
        if (names.size() > 0) {
            writer.name(MappingRules.CHILD_ORDER_NAME);
            writer.beginArray();
            for (String name : names) {
                if (!ResourceUtil.CONTENT_NODE.equals(name)) {
                    writer.value(name);
                }
            }
            writer.endArray();
        }
    }

    //
    // import JSON mapped (exported) resource structures
    //

    protected static final Gson GSON = JsonUtil.GSON_BUILDER.create();

    /**
     * @param reader
     * @param resolver
     * @param path
     * @throws RepositoryException
     * @throws IOException
     */
    public static Resource importJson(JsonReader reader, ResourceResolver resolver, String path)
            throws RepositoryException, IOException {
        return importJson(reader, resolver, path, MappingRules.DEFAULT_MAPPING_RULES);
    }

    /**
     * @param reader
     * @param resolver
     * @param path
     * @param rules
     * @throws RepositoryException
     * @throws IOException
     */
    public static Resource importJson(JsonReader reader, ResourceResolver resolver,
                                      String path, MappingRules rules)
            throws RepositoryException, IOException {

        Session session = resolver.adaptTo(Session.class);
        ValueFactory factory = session.getValueFactory();

        Resource resource = resolver.getResource(path);

        ArrayList<String> childrenSet = new ArrayList<>();
        HashMap<String, JsonProperty> propertiesSet = new HashMap<>();

        reader.beginObject();

        JsonToken token;
        while (reader.hasNext()) {

            String name = reader.nextName();

            if (MappingRules.PROPERTIES_NAME.equals(name)) {
                reader.beginArray();
                while ((token = reader.peek()) != JsonToken.END_ARRAY) {
                    switch (token) {
                        case BEGIN_OBJECT:
                            JsonProperty property = GSON.fromJson(reader, JsonProperty.class);
                            if (resource != null) {
                                if (importJsonProperty(factory, resource, property, rules)) {
                                    // remember all properties found in JSON object for finalizing
                                    propertiesSet.put(property.name, property);
                                }
                            } else {
                                // remember all properties found in JSON object for later store
                                propertiesSet.put(property.name, property);
                            }
                            break;
                    }
                }
                reader.endArray();

            } else {

                JsonProperty property = null;

                token = reader.peek();
                switch (token) {
                    case BEGIN_OBJECT:
                        // child resource
                        try {
                            if (resource == null) {
                                // to avoid expensive memory consumption the resource is created before
                                // the first child is imported; that can be a problem if the types are
                                // not available (primary type, mixin types) at this time
                                resource = createResource(resolver, path, propertiesSet, factory, rules);
                            }
                            importJson(reader, resolver, path + "/" + name, rules);
                        } catch (ConstraintViolationException cvex) {
                            LOG.error(cvex.getMessage() + " (" + path + "/" + name + ")", cvex);
                        }
                        childrenSet.add(name);
                        break;
                    case BEGIN_ARRAY:
                        // multi value property (short format)
                        reader.beginArray();
                        if (reader.peek() != JsonToken.END_ARRAY) {
                            // ignore the additional child order array
                            // FIXME: child reorder implementation needed
                            if (!MappingRules.CHILD_ORDER_NAME.equals(name)) {
                                property = parseJsonProperty(reader, name);
                                ArrayList<Object> values = new ArrayList<>();
                                values.add(property.value);
                                while ((token = reader.peek()) != JsonToken.END_ARRAY) {
                                    switch (token) {
                                        case BOOLEAN:
                                            values.add(reader.nextBoolean());
                                            break;
                                        case NUMBER:
                                            values.add(reader.nextLong());
                                            break;
                                        case STRING:
                                            values.add(parseJsonString(reader, property));
                                            break;
                                        case NULL:
                                            reader.nextNull();
                                            break;
                                    }
                                }
                                property.value = values.toArray();
                                property.multi = true;
                            } else {
                                while ((token = reader.peek()) != JsonToken.END_ARRAY) {
                                    reader.nextString();
                                }
                            }
                        }
                        reader.endArray();
                        break;
                    default:
                        // single value property (short format)
                        property = parseJsonProperty(reader, name);
                        break;
                }
                if (property != null) {
                    if (resource != null) {
                        if (importJsonProperty(factory, resource, property, rules)) {
                            // remember all properties found in JSON object for finalizing
                            propertiesSet.put(property.name, property);
                        }
                    } else {
                        // remember all properties found in JSON object for later store
                        propertiesSet.put(property.name, property);
                    }
                }
            }
        }

        reader.endObject();

        if (resource == null) {
            resource = createResource(resolver, path, propertiesSet, factory, rules);
        }

        if (rules.changeRule == MappingRules.ChangeRule.update) {
            Node node = resource.adaptTo(Node.class);
            if (node != null && !node.isNew()) {
                // remove all properties not included in JSON object if 'update' rule is specified
                if (node != null) {
                    PropertyIterator iterator = node.getProperties();
                    while (iterator.hasNext()) {
                        Property property = iterator.nextProperty();
                        String propertyName = property.getName();
                        if (propertiesSet.get(propertyName) == null
                                && rules.importPropertyFilter.accept(propertyName)) {
                            try {
                                node.setProperty(propertyName, (Value) null);
                            } catch (ValueFormatException vfex) {
                                node.setProperty(propertyName, (Value[]) null);
                            }
                        }
                    }
                }
                // remove all children not included in JSON object if the 'update' rule is specified
                for (Resource child : resource.getChildren()) {
                    String childName = child.getName();
                    if (!childrenSet.contains(childName) && rules.resourceFilter.accept(child)) {
                        Node childNode = child.adaptTo(Node.class);
                        if (childNode != null) {
                            childNode.remove();
                        }
                    }
                }
            }
        }
        return resource;
    }

    /**
     * Creates the resource parsed from JSON.
     * Uses the 'jcr:primaryType' property from the propertySet to determine
     * the right primary type for the new node.
     * Uses the 'jcr:mixinTypes' property to set up the mixin types of the node
     * before the other properties are set (it's important that all main type settings
     * are done before the properties are set to avoid constraint violations).
     *
     * @param resolver
     * @param path
     * @param propertiesSet
     * @param factory
     * @param rules
     * @return
     * @throws RepositoryException
     */
    public static Resource createResource(ResourceResolver resolver, String path,
                                          Map<String, JsonProperty> propertiesSet,
                                          ValueFactory factory, MappingRules rules)
            throws RepositoryException {
        // determine the new nodes primary type from the properties
        JsonProperty primaryType = propertiesSet.get(PropertyUtil.PROP_PRIMARY_TYPE);
        Resource resource = ResourceUtil.getOrCreateResource(resolver, path,
                primaryType != null ? (String) primaryType.value : null);
        if (resource != null) {
            JsonProperty mixinTypes = propertiesSet.get(PropertyUtil.PROP_MIXIN_TYPES);
            if (mixinTypes != null) {
                // import mixin types property first(!)
                if (!importJsonProperty(factory, resource, mixinTypes, rules)) {
                    propertiesSet.remove(PropertyUtil.PROP_MIXIN_TYPES);
                }
            }
            for (Map.Entry<String, JsonProperty> entry : propertiesSet.entrySet()) {
                JsonProperty property = entry.getValue();
                // import all the other properties - not the primary and mixin types
                if (!PropertyUtil.PROP_PRIMARY_TYPE.equals(property.name)
                        && !PropertyUtil.PROP_MIXIN_TYPES.equals(property.name)) {
                    if (!importJsonProperty(factory, resource, property, rules)) {
                        entry.setValue(null);
                    }
                }
            }
        }
        return resource;
    }

    /**
     * Parses a single property of the first element of an array and returns the result as a JSON POJO object.
     *
     * @param reader the reader with the JSON stream
     * @param name   the already parsed name of the property
     * @return a new JsonProperty object with the name, type, and value set
     * @throws RepositoryException
     * @throws IOException
     */
    public static JsonProperty parseJsonProperty(JsonReader reader, String name)
            throws RepositoryException, IOException {
        JsonToken token = reader.peek();
        JsonProperty property = new JsonProperty();
        property.name = name;
        switch (token) {
            case BOOLEAN:
                // map boolean values directly if not a string
                property.type = PropertyType.nameFromValue(PropertyType.BOOLEAN);
                property.value = reader.nextBoolean();
                break;
            case NUMBER:
                // map numver values to LONG directly if not a string
                property.type = PropertyType.nameFromValue(PropertyType.LONG);
                property.value = reader.nextLong();
                break;
            case STRING:
                // parse the string with an option type hint within
                parseJsonString(reader, property);
                break;
            case NULL:
                reader.nextNull();
                break;
        }
        return property;
    }

    /**
     * The parser for a JSON string value with an optional type hint ('{type}...') as the values prefix.
     * Sets the value and type of the property POJO and returns the string value without the type.
     * This is used for single property values and also for multiple values (in the array loop).
     *
     * @param reader
     * @param property
     * @return
     * @throws IOException
     */
    public static String parseJsonString(JsonReader reader, JsonProperty property) throws IOException {
        if (property.type == null) {
            // set property type to the default value if not set otherwise
            // (e.g. on by the value before in an array loop)
            property.type = PropertyType.nameFromValue(PropertyType.STRING);
        }
        String string = reader.nextString();
        // check for a type hint string pattern and extract the parts if matching
        Matcher matcher = MappingRules.TYPED_PROPERTY_STRING.matcher(string);
        if (matcher.matches()) {
            try {
                // ensure that the type ist known in the JCR repository
                property.type = PropertyType.nameFromValue(
                        PropertyType.valueFromName(matcher.group(1)));
                string = matcher.group(2);
            } catch (IllegalArgumentException iaex) {
                // if not a known type let the string unchanged
            }
        }
        property.value = string;
        return string;
    }

    /**
     * Changes the property specified by the JSON POJO of the given node if the change rule is appropriate to
     * the current state of the node (changes are made only if not only 'extend' is specified as rule or if
     * the property is new) and if the property filter of the rules accepts the property.
     * ConstraintViolationExceptions are catched if thrown and logged with 'info' level.
     *
     * @param factory  the ValueFactory to create the JCR value from  the JSON value
     * @param resource the resource which stores the property
     * @param property the JSON POJO for the property
     * @param rules    the change rules for merging with existing values
     * @throws RepositoryException
     */
    public static boolean importJsonProperty(ValueFactory factory, Resource resource,
                                             JsonProperty property, MappingRules rules)
            throws RepositoryException {
        Node node;
        if (resource != null && (node = resource.adaptTo(Node.class)) != null) {
            // change property if new or not only 'extend' is specified
            if ((rules.changeRule != MappingRules.ChangeRule.extend
                    || node.getProperty(property.name) == null)
                    && rules.importPropertyFilter.accept(property.name)) {
                try {
                    setJsonProperty(factory, node, property, rules.propertyFormat);
                    return true;
                } catch (ConstraintViolationException cvex) {
                    LOG.info(cvex.toString() + " (" + node.getPath() + "@" + property.name + ")");
                }
            }
        }
        return false;
    }

    //
    // ==== the JCR level ======================================================
    //
    // - wrapping for nodes and properties to JSON
    //

    /**
     * Write all properties of an node accepted by the filter into an JSON array.
     *
     * @param writer the JSON writer object (with the JSON state)
     * @param filter the property name filter
     * @param node   the JCR node to write
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonProperties(JsonWriter writer, StringFilter filter,
                                           Node node, MappingRules.PropertyFormat format)
            throws RepositoryException, IOException {
        if (node != null) {
            TreeMap<String, Property> sortedProperties = new TreeMap<>();
            PropertyIterator iterator = node.getProperties();
            while (iterator.hasNext()) {
                Property property = iterator.nextProperty();
                String name = property.getName();
                if (filter.accept(name)) {
                    sortedProperties.put(name, property);
                }
            }
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            } else {
                writer.beginArray();
            }
            for (Map.Entry<String, Property> entry : sortedProperties.entrySet()) {
                writeJsonProperty(writer, node, entry.getValue(), format);
            }
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.endObject();
            } else {
                writer.endArray();
            }
        }
    }

    /**
     * Write all properties of an node accepted by the filter into an JSON array.
     *
     * @param writer the JSON writer object (with the JSON state)
     * @param filter the property name filter
     * @param values the Sling ValueMap to write
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonValueMap(JsonWriter writer, StringFilter filter,
                                         ValueMap values, MappingRules.PropertyFormat format)
            throws RepositoryException, IOException {
        if (values != null) {
            TreeMap<String, Object> sortedProperties = new TreeMap<>();
            Iterator<Map.Entry<String, Object>> iterator = values.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                if (filter.accept(key)) {
                    sortedProperties.put(key, entry.getValue());
                }
            }
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            } else {
                writer.beginArray();
            }
            for (Map.Entry<String, Object> entry : sortedProperties.entrySet()) {
                writeJsonProperty(writer, entry.getKey(), entry.getValue(), format);
            }
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.endObject();
            } else {
                writer.endArray();
            }
        }
    }

    /**
     * Writes a JCR property as an JSON object: { name: ..., value: ..., type: ..., multi: ...}.
     *
     * @param writer   the JSON writer object (with the JSON state)
     * @param property the JCR property to write
     * @param format   the format in the JSON output
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonProperty(JsonWriter writer, Node node,
                                         Property property, MappingRules.PropertyFormat format)
            throws RepositoryException, IOException {
        if (property != null &&
                (PropertyType.BINARY != property.getType() ||
                        format.binary != MappingRules.PropertyFormat.Binary.skip)) {
            String name = property.getName();
            int type = property.getType();
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.name(name);
            } else {
                writer.beginObject();
                writer.name("name").value(name);
                writer.name("value");
            }
            if (property.isMultiple()) {
                writer.beginArray();
                for (Value value : property.getValues()) {
                    JsonUtil.writeJsonValue(writer, node, name, value, type, format);
                }
                writer.endArray();
            } else {
                JsonUtil.writeJsonValue(writer, node, name, property.getValue(), type, format);
            }
            if (format.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.name("type").value(PropertyType.nameFromValue(type));
                writer.name("multi").value(property.isMultiple());
                if (format.scope == MappingRules.PropertyFormat.Scope.definition) {
                    PropertyDefinition definition = property.getDefinition();
                    writer.name("auto").value(definition.isAutoCreated());
                    writer.name("protected").value(definition.isProtected());
                }
                writer.endObject();
            }
        }
    }

    /**
     * Writes a resource property (without node - probably synthetic resource)
     *
     * @param writer
     * @param name
     * @param value
     * @param format
     * @throws RepositoryException
     * @throws IOException
     */
    public static void writeJsonProperty(JsonWriter writer, String name,
                                         Object value, MappingRules.PropertyFormat format)
            throws RepositoryException, IOException {
        if (name != null && value != null) {
            int type = PropertyType.STRING;
            if (value instanceof Boolean) {
                type = PropertyType.BOOLEAN;
            } else if (value instanceof Long || value instanceof Integer) {
                type = PropertyType.LONG;
            } else if (value instanceof Double || value instanceof Float) {
                type = PropertyType.DOUBLE;
            } else if (value instanceof BigDecimal) {
                type = PropertyType.DECIMAL;
            } else if (value instanceof Calendar) {
                type = PropertyType.DATE;
            }
            if (format.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            }
            if (format.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.name(name);
            } else {
                writer.name("name").value(name);
                writer.name("value");
            }
            if (value instanceof Object[]) {
                writer.beginArray();
                for (Object val : (Object[]) value) {
                    JsonUtil.writeJsonValue(writer, null, name, val, type, format);
                }
                writer.endArray();
            } else {
                JsonUtil.writeJsonValue(writer, null, name, value, type, format);
            }
            if (format.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.name("type").value(PropertyType.nameFromValue(type));
                writer.name("multi").value(value instanceof Object[]);
                writer.endObject();
            }
        }
    }

    // Java type transformations

    /**
     * Write a JCR value to the JSON writer.
     *
     * @param writer the JSON writer object (with the JSON state)
     * @param type   the JCR type of the value (see PropertyType)
     * @param value  the value itself, should be a JCR property otherwise a java scalar object or array of scalars
     * @param type   the type of the value
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonValue(JsonWriter writer, Node node, String name, Object value,
                                      Integer type, MappingRules.PropertyFormat format)
            throws RepositoryException, IOException {
        Value jcrValue = value instanceof Value ? (Value) value : null;
        switch (type) {
            case PropertyType.BINARY:
                if (node != null && jcrValue != null) {
                    if (format.binary == MappingRules.PropertyFormat.Binary.link) {
                        String uri = "/bin/core/property.bin"
                                + LinkUtil.encodePath(node.getPath())
                                + "?name=" + LinkUtil.encodePath(name);
                        boolean htmlSafe = writer.isHtmlSafe();
                        writer.setHtmlSafe(false);
                        writer.value(uri);
                        writer.setHtmlSafe(htmlSafe);
                    } else if (format.binary == MappingRules.PropertyFormat.Binary.base64) {
                        Binary binary = jcrValue.getBinary();
                        byte[] buffer = IOUtils.toByteArray(binary.getStream());
                        String encoded = Base64.encodeBase64String(buffer);
                        writer.value(getValueString(encoded, type, format));
                    } else {
                        writer.nullValue();
                    }
                } else {
                    writer.nullValue();
                }
                break;
            case PropertyType.BOOLEAN:
                writer.value(jcrValue != null ? jcrValue.getBoolean() : (Boolean) value);
                break;
            case PropertyType.DATE:
                Calendar cal = jcrValue != null ? jcrValue.getDate() : (Calendar) value;
                if (cal != null) {
                    SimpleDateFormat dateFormat = MappingRules.MAP_DATE_FORMAT;
                    dateFormat.setTimeZone(cal.getTimeZone());
                    writer.value(getValueString(dateFormat.format(cal.getTime()), type, format));
                }
                break;
            case PropertyType.DECIMAL:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getDecimal() : (BigDecimal) value, type, format));
                break;
            case PropertyType.DOUBLE:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getDouble() : (Double) value, type, format));
                break;
            case PropertyType.LONG:
                writer.value(jcrValue != null ? jcrValue.getLong() : (Long) value);
                break;
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getString() : value.toString(), type, format));
                break;
            case PropertyType.UNDEFINED:
                writer.nullValue();
                break;
        }
    }

    // helper methods to decouple value write from value get (can probably throw an exception)

    public static void writeValue (JsonWriter writer, String name, String value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue (JsonWriter writer, String name, boolean value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue (JsonWriter writer, String name, double value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue (JsonWriter writer, String name, long value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue (JsonWriter writer, String name, Number value) throws IOException {
        writer.name(name).value(value);
    }

    /**
     * Embeds the property type in the string value if the formats scope is 'value'.
     *
     * @param value
     * @param type
     * @param format
     * @return
     */
    public static String getValueString(Object value, int type, MappingRules.PropertyFormat format) {
        String string = value.toString();
        if (format.scope == MappingRules.PropertyFormat.Scope.value && type != PropertyType.STRING) {
            string = "{" + PropertyType.nameFromValue(type) + "}" + string;
        }
        return string;
    }

    // helper methos for general use

    /**
     * @param writer
     * @param values
     * @throws IOException
     */
    public static void writeJsonArray(JsonWriter writer, String[] values) throws IOException {
        if (values != null) {
            writer.beginArray();
            for (String value : values) {
                writer.value(value);
            }
            writer.endArray();
        }
    }

    /**
     * @param writer
     * @param values
     * @throws IOException
     */
    public static void writeJsonArray(JsonWriter writer, Iterator<String> values) throws IOException {
        if (values != null) {
            writer.beginArray();
            while (values.hasNext()) {
                writer.value(values.next());
            }
            writer.endArray();
        }
    }

    // receiving JSON ...

    /**
     * Creates or updates one property at a JCR node.
     *
     * @param factory  the value factory to create the properties value
     * @param node     the node which holds the property
     * @param property the property object transformed from JSON
     * @return <code>true</code> if the property is set and available
     * @throws RepositoryException if storing was not possible (some reasons)
     */
    public static boolean setJsonProperty(ValueFactory factory, Node node,
                                          JsonProperty property, MappingRules.PropertyFormat format)
            throws RepositoryException {

        if (property != null) {

            int type = StringUtils.isNotBlank(property.type)
                    ? PropertyType.valueFromName(property.type) : PropertyType.STRING;

            String name = property.name;

            if (property.multi || property.value instanceof Object[]) {
                // make or store a multi value property

                Object[] jsonValues = property.value instanceof Object[] ? (Object[]) property.value : null;
                if (jsonValues == null) {
                    if (property.value instanceof List) {
                        // if the value is already a multi value use this directly
                        List<Object> list = (List<Object>) property.value;
                        jsonValues = list.toArray(new Object[list.size()]);
                    } else {
                        // make a multi value by splitting the string using a comma as delimiter
                        jsonValues = property.value != null
                                ? property.value.toString().split("\\s*,\\s*") : new String[0];
                    }
                }

                // make a JCR value for each string value
                Value[] values = new Value[jsonValues.length];
                try {
                    for (int i = 0; i < jsonValues.length; i++) {
                        values[i] = makeJcrValue(factory, type, jsonValues[i], format);
                    }
                } catch (PropertyValueFormatException pfex) {
                    return false;
                }

                Property jcrProperty = null;
                try {
                    jcrProperty = PropertyUtil.setProperty(node, name, values, type);

                } catch (ValueFormatException vfex) {
                    // if this exception occurs the property must be transformed to multi value
                    node.setProperty(name, (Value) null);
                    PropertyUtil.setProperty(node, name, values, type);
                }

                return jcrProperty != null;

            } else {
                // make or store a single value property

                String stringValue;
                if (property.value instanceof List) {
                    // if the value was a multi value before join this to one string
                    stringValue = StringUtils.join((List<String>) property.value, ',');
                } else {
                    stringValue = property.value != null ? property.value.toString() : null;
                }

                Value value = null;
                try {
                    value = makeJcrValue(factory, type, stringValue, format);
                } catch (PropertyValueFormatException pfex) {
                    return false;
                }

                Property jcrProperty = null;
                try {
                    jcrProperty = PropertyUtil.setProperty(node, name, value, type);

                } catch (ValueFormatException vfex) {
                    // if this exception occurs the property must be transformed to single value
                    node.setProperty(name, (Value[]) null);
                    PropertyUtil.setProperty(node, name, value, type);
                }

                return jcrProperty != null;
            }
        }

        return false;
    }

    /**
     * Create a JCR value from string value for the designated JCR type.
     *
     * @param factory the value factory to create the properties value
     * @param type    the JCR type according to the types declared in PropertyType
     * @param object  the value in the right type or a string representation of the value,
     *                for binary values a input stream can be used as parameter or a string
     *                with the base64 encoded data for the binary property
     * @return
     */
    public static Value makeJcrValue(ValueFactory factory, int type, Object object,
                                     MappingRules.PropertyFormat format)
            throws PropertyValueFormatException, RepositoryException {
        Value value = null;
        if (object != null) {
            switch (type) {
                case PropertyType.BINARY:
                    if (format.binary != MappingRules.PropertyFormat.Binary.skip) {
                        InputStream input = null;
                        if (object instanceof InputStream) {
                            input = (InputStream) object;
                        } else if (object instanceof String) {
                            if (format.binary == MappingRules.PropertyFormat.Binary.base64) {
                                byte[] decoded = Base64.decodeBase64((String) object);
                                input = new ByteArrayInputStream(decoded);
                            }
                        }
                        if (input != null) {
                            Binary binary = factory.createBinary(input);
                            value = factory.createValue(binary);
                        }
                    }
                    break;
                case PropertyType.BOOLEAN:
                    value = factory.createValue(object instanceof Boolean
                            ? (Boolean) object : Boolean.parseBoolean(object.toString()));
                    break;
                case PropertyType.DATE:
                    Date date = object instanceof Date ? (Date) object : null;
                    if (date == null) {
                        String string = object.toString();
                        // try some date patterns...
                        for (SimpleDateFormat dateFormat : MappingRules.DATE_PATTERNS) {
                            try {
                                date = dateFormat.parse(string);
                                // break after first usable pattern
                                break;
                            } catch (ParseException pex) {
                                // try next...
                            }
                        }
                    }
                    if (date != null) {
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTime(date);
                        value = factory.createValue(cal);
                    } else {
                        throw new PropertyValueFormatException("invalid date/time value: " + object);
                    }
                    break;
                case PropertyType.DECIMAL:
                    value = factory.createValue(object instanceof BigDecimal
                            ? (BigDecimal) object : new BigDecimal(object.toString()));
                    break;
                case PropertyType.DOUBLE:
                    value = factory.createValue(object instanceof Double
                            ? (Double) object : Double.parseDouble(object.toString()));
                    break;
                case PropertyType.LONG:
                    value = factory.createValue(object instanceof Long
                            ? (Long) object : Long.parseLong(object.toString()));
                    break;
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.STRING:
                case PropertyType.URI:
                case PropertyType.WEAKREFERENCE:
                    value = factory.createValue(object.toString(), type);
                    break;
                case PropertyType.UNDEFINED:
                    break;
            }
        }
        return value;
    }
}
