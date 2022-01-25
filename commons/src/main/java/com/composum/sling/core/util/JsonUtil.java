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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;

import static com.composum.sling.core.util.CoreConstants.PROPO_FROZEN_UUID;
import static com.composum.sling.core.util.CoreConstants.PROP_BASE_VERSION;
import static com.composum.sling.core.util.CoreConstants.PROP_PREDECESSORS;
import static com.composum.sling.core.util.CoreConstants.PROP_RESOURCE_SUPER_TYPE;
import static com.composum.sling.core.util.CoreConstants.PROP_RESOURCE_TYPE;
import static com.composum.sling.core.util.CoreConstants.PROP_ROOT_VERSION;
import static com.composum.sling.core.util.CoreConstants.PROP_SUCCESSORS;
import static com.composum.sling.core.util.CoreConstants.PROP_UUID;
import static com.composum.sling.core.util.CoreConstants.PROP_VERSION_HISTORY;

/**
 * The utility class to transform JCR object and Sling resources into JSON and update such objects using JSON data.
 */
public class JsonUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    /**
     * the declared builder for JSON POJO mapping
     */
    public static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    /**
     * the structure for parsing property values from JSON using Gson
     */
    public static class JsonProperty {

        public String name;
        public String oldname;
        public Object value; // can be a 'String' or a list of 'String' (multi value)
        public String type = PropertyType.nameFromValue(PropertyType.STRING);
        public boolean multi = false;
    }

    //
    // mapping of objects and maps
    //

    /**
     * Transforms a JSON object (stream) into a Map object.
     */
    public static Map<String, Object> jsonMap(JsonReader reader) throws IOException {
        Map<String, Object> map = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext() && reader.peek() == JsonToken.NAME) {
            String name = reader.nextName();
            Object value = jsonValue(reader);
            if (value != null) {
                map.put(name, value);
            }
        }
        reader.endObject();
        return map;
    }

    /**
     * Transforms a JSON object (stream) into an object.
     */
    public static Object jsonValue(JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case STRING:
                return reader.nextString();
            case BOOLEAN:
                return reader.nextBoolean();
            case NUMBER:
                try {
                    return reader.nextLong();
                } catch (NumberFormatException nfex) {
                    return reader.nextDouble();
                }
            case BEGIN_ARRAY:
                ArrayList<Object> list = new ArrayList<>();
                reader.beginArray();
                while (reader.peek() != JsonToken.END_ARRAY) {
                    list.add(jsonValue(reader));
                }
                reader.endArray();
                return list;
            case BEGIN_OBJECT:
                return jsonMap(reader);
            default:
                reader.skipValue();
        }
        return null;
    }

    /**
     * Transforms a Map object into a JSON object (stream).
     */
    public static void jsonMap(@NotNull final JsonWriter writer, @Nullable final Map<String, ?> map)
            throws IOException {
        if (map != null) {
            writer.beginObject();
            jsonMapEntries(writer, map);
            writer.endObject();
        }
    }

    public static void jsonMapEntries(@NotNull final JsonWriter writer, @Nullable final Map<String, ?> map)
            throws IOException {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                writer.name(entry.getKey());
                jsonValue(writer, entry.getValue());
            }
        }
    }

    /**
     * Transforms an object into a JSON object (stream).
     */
    @SuppressWarnings("unchecked")
    public static void jsonValue(@NotNull final JsonWriter writer, @Nullable final Object value)
            throws IOException {
        if (value == null) {
            writer.nullValue();
        } else {
            if (value instanceof Map) {
                jsonMap(writer, (Map<String, ?>) value);
            } else if (value instanceof Collection) {
                writer.beginArray();
                for (Object val : ((Collection<?>) value)) {
                    jsonValue(writer, val);
                }
                writer.endArray();
            } else if (value instanceof Object[]) {
                writer.beginArray();
                for (Object val : ((Object[]) value)) {
                    jsonValue(writer, val);
                }
                writer.endArray();
            } else if (value instanceof Boolean) {
                writer.value((Boolean) value);
            } else if (value instanceof Long) {
                writer.value((Long) value);
            } else if (value instanceof Double) {
                writer.value((Double) value);
            } else if (value instanceof Number) {
                writer.value((Number) value);
            } else {
                writer.value(value.toString());
            }
        }
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
     */
    public static void exportJson(@NotNull final JsonWriter writer, @NotNull final Resource resource)
            throws RepositoryException, IOException {
        exportJson(writer, resource, MappingRules.getDefaultMappingRules());
    }

    /**
     * @param writer   the writer for the JSON transformation
     * @param resource the resource to transform
     * @param mapping  the mapping policy rule set
     */
    public static void exportJson(@NotNull final JsonWriter writer, @NotNull final Resource resource,
                                  MappingRules mapping)
            throws RepositoryException, IOException {
        exportJson(writer, resource, mapping, 1);
    }

    /**
     * @param writer   the writer for the JSON transformation
     * @param resource the resource to transform
     * @param mapping  the mapping policy rule set
     * @param depth    the max depth for the rendering
     */
    public static void exportJson(JsonWriter writer, Resource resource, MappingRules mapping, int depth)
            throws RepositoryException, IOException {

        if (resource != null) {

            writer.beginObject();

            exportProperties(writer, resource, mapping);

            // export children after the properties(!) if depth is not reached or not restricted
            if (mapping.maxDepth == 0 || depth < mapping.maxDepth) {
                depth++;
                for (Resource child : resource.getChildren()) {
                    if (mapping.resourceFilter.accept(child)) {
                        writer.name(child.getName());
                        exportJson(writer, child, mapping, depth);
                    }
                }
            }

            writer.endObject();
        }
    }

    /**
     * @param writer   the writer for the JSON transformation
     * @param resource the resource to transform
     * @param mapping  the mapping policy rule set
     */
    public static void exportProperties(@NotNull final JsonWriter writer,
                                        @NotNull final Resource resource, MappingRules mapping)
            throws RepositoryException, IOException {

        String path = resource.getPath();
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
        if (mapping.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
            writer.name(MappingRules.PROPERTIES_NAME);
            writer.beginArray();
        }
        for (Map.Entry<String, Object> entry : propertiesSet.entrySet()) {
            String name = entry.getKey();
            if (mapping.exportPropertyFilter.accept(name)) {
                Object value = entry.getValue();
                if (value instanceof Property) {
                    Property property = (Property) value;
                    writeJsonProperty(resource, writer, property, mapping);
                } else {
                    // if no node exists (synthetic resource) the properties are simple values
                    writeJsonProperty(resource, writer, name, value, mapping);
                }
            }
        }
        if (mapping.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
            writer.endArray();
        }
    }

    /**
     * Writes the names of all children (except the 'jcr:content' child) into one array value
     * named '_child_order_' as a hint to recalculate the original order of the children.
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
     *
     */
    public static Resource importJson(JsonReader reader, ResourceResolver resolver, String path)
            throws RepositoryException, IOException {
        return importJson(reader, resolver, path, MappingRules.getDefaultMappingRules());
    }

    /**
     *
     */
    public static Resource importJson(JsonReader reader, ResourceResolver resolver,
                                      String path, MappingRules mapping)
            throws RepositoryException, IOException {

        Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
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
                                if (importJsonProperty(factory, resource, property, mapping)) {
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
                                resource = createResource(resolver, path, propertiesSet, factory, mapping);
                            }
                            importJson(reader, resolver, path + "/" + name, mapping);
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
                                while (reader.peek() != JsonToken.END_ARRAY) {
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
                        if (importJsonProperty(factory, resource, property, mapping)) {
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
            resource = createResource(resolver, path, propertiesSet, factory, mapping);
        }

        if (mapping.changeRule == MappingRules.ChangeRule.update) {
            Node node = resource.adaptTo(Node.class);
            if (node != null && !node.isNew()) {
                // remove all properties not included in JSON object if 'update' rule is specified
                PropertyIterator iterator = node.getProperties();
                while (iterator.hasNext()) {
                    Property property = iterator.nextProperty();
                    String propertyName = property.getName();
                    if (propertiesSet.get(propertyName) == null
                            && mapping.importPropertyFilter.accept(propertyName)) {
                        try {
                            node.setProperty(propertyName, (Value) null);
                        } catch (ValueFormatException vfex) {
                            node.setProperty(propertyName, (Value[]) null);
                        }
                    }
                }
                // remove all children not included in JSON object if the 'update' rule is specified
                for (Resource child : resource.getChildren()) {
                    String childName = child.getName();
                    if (!childrenSet.contains(childName) && mapping.resourceFilter.accept(child)) {
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
     */
    public static Resource createResource(ResourceResolver resolver, String path,
                                          Map<String, JsonProperty> propertiesSet,
                                          ValueFactory factory, MappingRules mapping)
            throws RepositoryException {
        // determine the new nodes primary type from the properties
        JsonProperty primaryType = propertiesSet.get(PropertyUtil.PROP_PRIMARY_TYPE);
        Resource resource = ResourceUtil.getOrCreateResource(resolver, path,
                primaryType != null ? (String) primaryType.value : null);
        if (resource != null) {
            JsonProperty mixinTypes = propertiesSet.get(PropertyUtil.PROP_MIXIN_TYPES);
            if (mixinTypes != null) {
                // import mixin types property first(!)
                if (!importJsonProperty(factory, resource, mixinTypes, mapping)) {
                    propertiesSet.remove(PropertyUtil.PROP_MIXIN_TYPES);
                }
            }
            for (Map.Entry<String, JsonProperty> entry : propertiesSet.entrySet()) {
                JsonProperty property = entry.getValue();
                // import all the other properties - not the primary and mixin types
                if (!PropertyUtil.PROP_PRIMARY_TYPE.equals(property.name)
                        && !PropertyUtil.PROP_MIXIN_TYPES.equals(property.name)) {
                    if (!importJsonProperty(factory, resource, property, mapping)) {
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
     */
    public static JsonProperty parseJsonProperty(JsonReader reader, String name)
            throws IOException {
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
     * @param mapping  the change rules for merging with existing values
     */
    public static boolean importJsonProperty(ValueFactory factory, Resource resource,
                                             JsonProperty property, MappingRules mapping)
            throws RepositoryException {
        Node node;
        if (resource != null && (node = resource.adaptTo(Node.class)) != null) {
            // change property if new or not only 'extend' is specified
            if ((mapping.changeRule != MappingRules.ChangeRule.extend
                    || node.getProperty(property.name) == null)
                    && mapping.importPropertyFilter.accept(property.name)) {
                try {
                    setJsonProperty(node, property, mapping);
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
    public static void writeJsonProperties(@NotNull final Resource resource, @NotNull final JsonWriter writer,
                                           @NotNull final StringFilter filter, @Nullable final Node node,
                                           @NotNull final MappingRules mapping)
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
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            } else {
                writer.beginArray();
            }
            for (Map.Entry<String, Property> entry : sortedProperties.entrySet()) {
                writeJsonProperty(resource, writer, entry.getValue(), mapping);
            }
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
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
    public static void writeJsonValueMap(@NotNull final Resource resource, @NotNull final JsonWriter writer,
                                         @NotNull final StringFilter filter, @Nullable final ValueMap values,
                                         @NotNull final MappingRules mapping)
            throws RepositoryException, IOException {
        if (values != null) {
            TreeMap<String, Object> sortedProperties = new TreeMap<>();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                if (filter.accept(key)) {
                    sortedProperties.put(key, entry.getValue());
                }
            }
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            } else {
                writer.beginArray();
            }
            for (Map.Entry<String, Object> entry : sortedProperties.entrySet()) {
                writeJsonProperty(resource, writer, entry.getKey(), entry.getValue(), mapping);
            }
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
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
     * @param mapping  the format in the JSON output
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonProperty(@NotNull final Resource resource, @NotNull final JsonWriter writer,
                                         @Nullable final Property property, @NotNull final MappingRules mapping)
            throws RepositoryException, IOException {
        if (property != null &&
                (PropertyType.BINARY != property.getType() ||
                        mapping.propertyFormat.binary != MappingRules.PropertyFormat.Binary.skip)) {
            String name = property.getName();
            int type = property.getType();
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.name(name);
            } else {
                writer.beginObject();
                writer.name("name").value(name);
                writer.name("value");
            }
            StringBuilder valueString = PropertyType.STRING == type ? new StringBuilder() : null;
            if (property.isMultiple()) {
                writer.beginArray();
                for (Value value : property.getValues()) {
                    JsonUtil.writeJsonValue(resource, writer, name, value, type, mapping);
                    if (valueString != null) {
                        if (valueString.length() > 0) {
                            valueString.append(',');
                        }
                        valueString.append(value.getString());
                    }
                }
                writer.endArray();
            } else {
                Value value = property.getValue();
                JsonUtil.writeJsonValue(resource, writer, name, value, type, mapping);
                if (valueString != null) {
                    valueString.append(value.getString());
                }
            }
            if (mapping.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.name("type").value(PropertyType.nameFromValue(type));
                writer.name("multi").value(property.isMultiple());
                if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.definition) {
                    PropertyDefinition definition = property.getDefinition();
                    writer.name("auto").value(definition.isAutoCreated());
                    writer.name("protected").value(definition.isProtected());
                    if (valueString != null) {
                        writer.name("subtype").value(PropertyUtil.getStringSubtype(valueString.toString()).name());
                    }
                }
                if (!property.isMultiple()) {
                    String target = null;
                    switch (type) {
                        case PropertyType.STRING:
                        case PropertyType.PATH:
                        case PropertyType.REFERENCE:
                        case PropertyType.WEAKREFERENCE:
                            target = getValueTarget(resource, name, property.getString());
                            break;
                    }
                    if (StringUtils.isNotBlank(target)) {
                        writer.name("target").value(target);
                    }
                }
                writer.endObject();
            }
        }
    }

    /**
     * Writes a resource property (without node - probably synthetic resource)
     */
    public static void writeJsonProperty(@NotNull final Resource resource, @NotNull final JsonWriter writer,
                                         @Nullable final String name, @Nullable final Object value,
                                         @NotNull final MappingRules mapping)
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
            } else if (value instanceof InputStream) {
                type = PropertyType.BINARY;
            }
            if (mapping.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.beginObject();
            }
            if (mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
                writer.name(name);
            } else {
                writer.name("name").value(name);
                writer.name("value");
            }
            boolean isMultiple = value instanceof Object[];
            if (isMultiple) {
                writer.beginArray();
                for (Object val : (Object[]) value) {
                    JsonUtil.writeJsonValue(resource, writer, name, val, type, mapping);
                }
                writer.endArray();
            } else {
                JsonUtil.writeJsonValue(resource, writer, name, value, type, mapping);
            }
            if (mapping.propertyFormat.scope != MappingRules.PropertyFormat.Scope.value) {
                writer.name("type").value(PropertyType.nameFromValue(type));
                writer.name("multi").value(isMultiple);
                if (!isMultiple && type == PropertyType.STRING) {
                    String target = getValueTarget(resource, name, value.toString());
                    if (StringUtils.isNotBlank(target)) {
                        writer.name("target").value(target);
                    }
                }
                writer.endObject();
            }
        }
    }

    public static String getValueTarget(@Nullable final Resource resource,
                                        @Nullable final String name,
                                        @Nullable final String value) {
        if (resource != null && StringUtils.isNotBlank(value)) {
            ResourceResolver resolver = resource.getResourceResolver();
            Resource target;
            if (value.startsWith("/")) {
                target = resolver.getResource(value);
                if (target != null) {
                    return target.getPath();
                }
            }
            if (StringUtils.isNotBlank(name)) {
                switch (name) {
                    case PROP_RESOURCE_TYPE:
                    case PROP_RESOURCE_SUPER_TYPE:
                        if (!value.startsWith("/")) {
                            for (String root : resolver.getSearchPath()) {
                                target = resolver.getResource(root + value);
                                if (target != null) {
                                    return target.getPath();
                                }
                            }
                        }
                        break;
                    case PROP_UUID:
                        break;
                    case PROPO_FROZEN_UUID:
                    case PROP_ROOT_VERSION:
                    case PROP_BASE_VERSION:
                    case PROP_VERSION_HISTORY:
                    case PROP_PREDECESSORS:
                    case PROP_SUCCESSORS:
                    default:
                        Session session = resolver.adaptTo(Session.class);
                        if (session != null) {
                            try {
                                Node node = session.getNodeByIdentifier(value);
                                return node.getPath();
                            } catch (Exception ignore) {
                            }
                        }
                        break;
                }
            }
        }
        return null;
    }

    // Java type transformations

    /**
     * Write a JCR value to the JSON writer.
     *
     * @param writer the JSON writer object (with the JSON state)
     * @param name   the name of the value
     * @param value  the value itself, should be a JCR property otherwise a java scalar object or array of scalars
     * @param type   the type of the value
     * @throws javax.jcr.RepositoryException error on accessing JCR
     * @throws java.io.IOException           error on write JSON
     */
    public static void writeJsonValue(@NotNull final Resource resource, @NotNull final JsonWriter writer,
                                      @NotNull final String name, @NotNull final Object value,
                                      @NotNull final Integer type, @NotNull final MappingRules mapping)
            throws RepositoryException, IOException {
        Value jcrValue = value instanceof Value ? (Value) value : null;
        switch (type) {
            case PropertyType.BINARY:
                if (mapping.propertyFormat.binary == MappingRules.PropertyFormat.Binary.link) {
                    String uri = "/bin/cpm/nodes/property.bin"
                            + LinkUtil.encodePath(resource.getPath())
                            + "?name=" + UrlCodec.QUERYPART.encode(name);
                    boolean htmlSafe = writer.isHtmlSafe();
                    writer.setHtmlSafe(false);
                    writer.value(uri);
                    writer.setHtmlSafe(htmlSafe);
                } else if (
                        mapping.propertyFormat.binary == MappingRules.PropertyFormat.Binary.base64) {
                    byte[] buffer = null;
                    if (jcrValue != null) {
                        Binary binary = jcrValue.getBinary();
                        buffer = IOUtils.toByteArray(binary.getStream());
                    } else if (value instanceof InputStream) {
                        buffer = IOUtils.toByteArray((InputStream) value);
                    }
                    String encoded = Base64.encodeBase64String(buffer != null ? buffer : new byte[0]);
                    writer.value(getValueString(encoded, type, mapping));
                } else {
                    writer.nullValue();
                }
                break;
            case PropertyType.BOOLEAN:
                writer.value(jcrValue != null ? jcrValue.getBoolean()
                        : (value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString())));
                break;
            case PropertyType.DATE:
                Calendar cal = jcrValue != null ? jcrValue.getDate()
                        : (value instanceof Calendar ? (Calendar) value : null);
                if (cal != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(MappingRules.MAP_DATE_FORMAT);
                    dateFormat.setTimeZone(cal.getTimeZone());
                    writer.value(getValueString(dateFormat.format(cal.getTime()), type, mapping));
                }
                break;
            case PropertyType.DECIMAL:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getDecimal() : (value instanceof BigDecimal ? (BigDecimal) value
                        : new BigDecimal(value.toString())), type, mapping));
                break;
            case PropertyType.DOUBLE:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getDouble() : (value instanceof Double ? (Double) value
                        : Double.valueOf(value.toString())), type, mapping));
                break;
            case PropertyType.LONG:
                writer.value(jcrValue != null ? jcrValue.getLong()
                        : (value instanceof Long ? (Long) value : Long.valueOf(value.toString())));
                break;
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
                writer.value(getValueString(jcrValue != null
                        ? jcrValue.getString() : value.toString(), type, mapping));
                break;
            case PropertyType.UNDEFINED:
                writer.nullValue();
                break;
        }
    }

    // helper methods to decouple value write from value get (can probably throw an exception)

    public static void writeValue(JsonWriter writer, String name, String value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue(JsonWriter writer, String name, boolean value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue(JsonWriter writer, String name, double value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue(JsonWriter writer, String name, long value) throws IOException {
        writer.name(name).value(value);
    }

    public static void writeValue(JsonWriter writer, String name, Number value) throws IOException {
        writer.name(name).value(value);
    }

    /**
     * Embeds the property type in the string value if the formats scope is 'value'.
     */
    public static String getValueString(Object value, int type, MappingRules mapping) {
        String string = value.toString();
        if (type != PropertyType.STRING &&
                mapping.propertyFormat.embedType &&
                mapping.propertyFormat.scope == MappingRules.PropertyFormat.Scope.value) {
            string = "{" + PropertyType.nameFromValue(type) + "}" + string;
        }
        return string;
    }

    // helper methos for general use

    /**
     *
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
     *
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

    public interface ElementCallback<T> {
        String doWithElement(T element) throws Exception;
    }

    public static <T> void writeJsonArray(JsonWriter writer, Iterator<T> values, ElementCallback<T> elementCallback) throws IOException {
        if (values != null) {
            writer.beginArray();
            while (values.hasNext()) {
                try {
                    String extractedValue = elementCallback.doWithElement(values.next());
                    writer.value(extractedValue);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            writer.endArray();
        }
    }

    public static <T> void writeJsonArray(JsonWriter writer, String name, Iterator<T> values, ElementCallback<T> elementCallback) throws IOException {
        writer.name(name);
        writeJsonArray(writer, values, elementCallback);
    }

    // receiving JSON ...

    /**
     * Creates or updates one property at a JCR node.
     *
     * @param node     the node which holds the property
     * @param property the property object transformed from JSON
     * @return <code>true</code> if the property is set and available
     * @throws RepositoryException if storing was not possible (some reasons)
     */
    public static boolean setJsonProperty(Node node,
                                          JsonProperty property, MappingRules mapping)
            throws RepositoryException {

        if (property != null) {

            int type = StringUtils.isNotBlank(property.type)
                    ? PropertyType.valueFromName(property.type) : PropertyType.STRING;

            String name = property.name;
            String oldname = property.oldname;
            if (!StringUtils.isBlank(oldname) && !name.equals(oldname) && node.hasProperty(name)) {
                throw new RepositoryException("property '" + name + "' already exists");
            }

            if (property.multi || property.value instanceof Object[]) {
                // make or store a multi value property

                Object[] jsonValues = property.value instanceof Object[] ? (Object[]) property.value : null;
                if (jsonValues == null) {
                    if (property.value instanceof List) {
                        // if the value is already a multi value use this directly
                        List<?> list = (List<?>) property.value;
                        jsonValues = list.toArray(new Object[0]);
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
                        values[i] = makeJcrValue(node, type, jsonValues[i], mapping);
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
                if (!StringUtils.isBlank(oldname) && !name.equals(oldname)) {
                    node.setProperty(oldname, (Value) null);
                }

                return jcrProperty != null;

            } else {
                // make or store a single value property

                String stringValue;
                if (property.value instanceof List) {
                    // if the value was a multi value before join this to one string
                    //noinspection unchecked
                    stringValue = StringUtils.join((List<String>) property.value, ',');
                } else {
                    stringValue = property.value != null ? property.value.toString() : null;
                }

                Value value;
                try {
                    value = makeJcrValue(node, type, stringValue, mapping);
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
                if (!StringUtils.isBlank(oldname) && !name.equals(oldname)) {
                    node.setProperty(oldname, (Value) null);
                }

                return jcrProperty != null;
            }
        }

        return false;
    }

    /**
     * Create a JCR value from string value for the designated JCR type.
     *
     * @param node   the node of the property
     * @param type   the JCR type according to the types declared in PropertyType
     * @param object the value in the right type or a string representation of the value,
     *               for binary values a input stream can be used as parameter or a string
     *               with the base64 encoded data for the binary property
     */
    public static Value makeJcrValue(Node node, int type, Object object,
                                     MappingRules mapping)
            throws PropertyValueFormatException, RepositoryException {
        Session session = node.getSession();
        ValueFactory factory = session.getValueFactory();

        Value value = null;
        if (object != null) {
            switch (type) {
                case PropertyType.BINARY:
                    if (mapping.propertyFormat.binary != MappingRules.PropertyFormat.Binary.skip) {
                        InputStream input = null;
                        if (object instanceof InputStream) {
                            input = (InputStream) object;
                        } else if (object instanceof String) {
                            if (mapping.propertyFormat.binary == MappingRules.PropertyFormat.Binary.base64) {
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
                        date = mapping.dateParser.parse(string);
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
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                    final Node refNode = session.getNodeByIdentifier(object.toString());
                    final String identifier = refNode.getIdentifier();
                    value = factory.createValue(identifier, type);
                    break;
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.STRING:
                case PropertyType.URI:
                    value = factory.createValue(object.toString(), type);
                    break;
                case PropertyType.UNDEFINED:
                    break;
            }
        }
        return value;
    }

    /**
     * Create a JCR value from string value for the designated JCR type.
     *
     * @param type  the JCR type according to the types declared in PropertyType
     * @param value the value in the right type or a string representation of the value,
     *              for binary values a input stream can be used as parameter or a string
     *              with the base64 encoded data for the binary property
     */
    public static Object makeValueObject(int type, Object value) {
        MappingRules mapping = MappingRules.getDefaultMappingRules();
        Object object = null;
        if (value != null) {
            if (value instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection<?> collection = (Collection) value;
                Object[] multi = new Object[collection.size()];
                Iterator<?> it = collection.iterator();
                for (int i = 0; i < collection.size(); i++) {
                    multi[i] = makeValueObject(type, it.next());
                }
                object = multi;
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                Object[] multi = new Object[array.length];
                for (int i = 0; i < array.length; i++) {
                    multi[i] = makeValueObject(type, array[i]);
                }
                object = multi;
            } else {
                switch (type) {
                    case PropertyType.BINARY:
                        if (value instanceof InputStream) {
                            object = value;
                        } else if (value instanceof String) {
                            byte[] decoded = Base64.decodeBase64(value.toString());
                            object = new ByteArrayInputStream(decoded);
                        }
                        break;
                    case PropertyType.BOOLEAN:
                        object = value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString());
                        break;
                    case PropertyType.DATE:
                        Date date = value instanceof Date ? (Date) value : null;
                        if (date == null) {
                            date = mapping.dateParser.parse(value.toString());
                        }
                        if (date != null) {
                            GregorianCalendar cal = new GregorianCalendar();
                            cal.setTime(date);
                            object = cal;
                        } else {
                            throw new IllegalArgumentException("invalid date/time value: " + value);
                        }
                        break;
                    case PropertyType.DECIMAL:
                        object = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
                        break;
                    case PropertyType.DOUBLE:
                        object = value instanceof Double ? (Double) value : Double.parseDouble(value.toString());
                        break;
                    case PropertyType.LONG:
                        object = value instanceof Long ? (Long) value : Long.parseLong(value.toString());
                        break;
                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                    case PropertyType.NAME:
                    case PropertyType.PATH:
                    case PropertyType.STRING:
                    case PropertyType.URI:
                        object = value.toString();
                        break;
                    case PropertyType.UNDEFINED:
                        break;
                }
            }
        }
        return object;
    }
}
