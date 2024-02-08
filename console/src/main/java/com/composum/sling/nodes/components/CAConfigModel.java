package com.composum.sling.nodes.components;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;
import com.google.gson.stream.JsonWriter;

@Restricted(key = NodeServlet.SERVICE_KEY)
public class CAConfigModel extends ConsoleServletBean {

    private static final Logger LOG = LoggerFactory.getLogger(CAConfigModel.class);

    protected ConfigurationManager configurationManager;

    public CAConfigModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public CAConfigModel(BeanContext context) {
        super(context);
    }

    public CAConfigModel() {
        super();
    }

    @Override
    @NotNull
    public ResourceHandle getResource() {
        Resource resource = null;
        final SlingHttpServletRequest request = context.getRequest();
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String suffix = pathInfo.getSuffix();
        if (StringUtils.isNotBlank(suffix)) {
            resource = request.getResourceResolver().getResource(suffix);
        }
        return resource != null ? ResourceHandle.use(resource) : super.getResource();
    }

    public String getViewType() {
        try {
            if (getPath().startsWith("/content")) {
                return "effectiveConfigurationsView";
            } else if (getPath().matches(".*/sling:configs($|/).*")) {
                if (getName().equals("sling:configs")) {
                    return "listConfigurationsView";
                } else if (getResource().getParent().getName().equals("sling:configs")) {
                    String configName = getResource().getName();
                    ConfigurationMetadata metadata = getConfigurationManager().getConfigurationMetadata(configName);
                    if (metadata.isCollection()) {
                        return "collectionView";
                    } else {
                        return "configurationView";
                    }
                } else if (getResource().getParent().getParent().getName().equals("sling:configs")) {
                    String configName = getResource().getParent().getName();
                    ConfigurationMetadata metadata = getConfigurationManager().getConfigurationMetadata(configName);
                    if (metadata.isCollection()) {
                        return "configurationView";
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Cannot determine view type for {}", getPath(), e);
        }
        return null;
    }

    protected ConfigurationManager getConfigurationManager() {
        if (configurationManager == null) {
            configurationManager = context.getService(ConfigurationManager.class);
        }
        return configurationManager;
    }

    /**
     * Returns all configuration metadata.
     */
    public List<ConfigurationMetadata> getAllMetaData() {
        SortedSet<String> names = getConfigurationManager().getConfigurationNames();
        return names.stream()
                .map(name -> getConfigurationManager().getConfigurationMetadata(name))
                .collect(Collectors.toList());
    }

    public List<SingletonConfigInfo> getSingletonConfigurations() {
        SortedSet<String> names = getConfigurationManager().getConfigurationNames();
        List<SingletonConfigInfo> result = names.stream()
                .filter(name ->
                        requireNonNull(getConfigurationManager().getConfigurationMetadata(name)).isSingleton())
                .map(name ->
                        new SingletonConfigInfo(name, getConfigurationManager().getConfiguration(resource, name)))
                .collect(Collectors.toList());
        return result;
    }

    public List<CollectionConfigInfo> getCollectionConfigurations() {
        SortedSet<String> names = getConfigurationManager().getConfigurationNames();
        List<CollectionConfigInfo> result = names.stream()
                .filter(name ->
                        requireNonNull(getConfigurationManager().getConfigurationMetadata(name)).isCollection())
                .map(name -> new CollectionConfigInfo(getConfigurationManager().getConfigurationCollection(resource, name)))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * The information about the configuration collection this resource represents.
     */
    public CollectionConfigInfo getThisCollectionConfiguration() {
        CollectionConfigInfo configInfo = getCollectionConfigurations().stream()
                .filter(collectionConfigInfo -> collectionConfigInfo.getCollectionConfigData().getConfigName().equals(getName()))
                .findFirst().orElse(null);
        return configInfo;
    }

    /**
     * The information about the configuration this resource represents. It can either be a singleton configuration
     * or an item in a collection.
     */
    public SingletonConfigInfo getThisSingletonConfiguration() {
        Resource configResource = getResource();
        while (configResource != null && configResource.getParent() != null
                && !configResource.getParent().getName().equals("sling:configs")) {
            configResource = configResource.getParent();
        }
        String configName = configResource != null ? configResource.getName() : null;
        ConfigurationData configurationData = configName != null ? getConfigurationManager().getConfiguration(getResource(), configName) : null;
        if (configurationData != null) {
            return new SingletonConfigInfo(configName, configurationData);
        }
        return null;
    }

    public ValueInfo<?> getThisProperty() {
        String propertyName = getRequest().getParameter("propertyName");
        SingletonConfigInfo configInfo = getThisSingletonConfiguration();
        ValueInfo<?> result = null;
        if (configInfo != null && propertyName != null) {
            result = configInfo.getValueInfos().stream()
                    .filter(valueInfo -> valueInfo.getPropertyMetadata() != null
                            && valueInfo.getPropertyMetadata().getName().equals(propertyName))
                    .findFirst().orElse(null);
        }
        return result;
    }

    /**
     * List of all paths that are referenced from a sling:configRef of the resource or one of it's parents.
     */
    public List<String> getReferencedConfigPaths() {
        List<String> paths = new ArrayList<>();
        Resource resource = getResource();
        while (resource != null) {
            ValueMap properties = resource.getValueMap();
            String configRef = properties.get("sling:configRef", String.class);
            if (configRef != null) {
                paths.add(configRef + "/sling:configs");
            }
            resource = resource.getParent();
        }
        for (String possibleDefault : new String[]{"/conf/global/sling:configs", "/apps/conf/sling:configs", "/libs/conf/sling:configs"}) {
            // add the resource to the list if it exists
            Resource defaultResource = getResolver().getResource(possibleDefault);
            if (defaultResource != null) {
                paths.add(possibleDefault);
            }
        }
        return paths;
    }

    public class CollectionConfigInfo {

        protected final ConfigurationCollectionData collectionConfigData;
        protected final ConfigurationMetadata metadata;

        public CollectionConfigInfo(ConfigurationCollectionData configurationCollection) {
            this.collectionConfigData = configurationCollection;
            this.metadata = getConfigurationManager().getConfigurationMetadata(collectionConfigData.getConfigName());
        }

        /**
         * Whether this collection is configured to inherit from parent configurations - sling:configPropertyInherit .
         */
        public boolean isInherits() {
            Object inheritProperty = collectionConfigData.getProperties().get("sling:configCollectionInherit");
            return inheritProperty instanceof Boolean && (Boolean) inheritProperty;
        }

        public boolean isResourceExists() {
            return getResource().getChild(metadata.getName()) != null;
        }

        public ConfigurationCollectionData getCollectionConfigData() {
            return collectionConfigData;
        }

        public ConfigurationMetadata getMetadata() {
            return metadata;
        }

        public List<SingletonConfigInfo> getConfigs() {
            return collectionConfigData.getItems().stream()
                    .map(item -> new SingletonConfigInfo(collectionConfigData.getConfigName(), item))
                    .collect(Collectors.toList());
        }
    }

    public class SingletonConfigInfo {

        protected final ConfigurationData configurationData;
        protected final String name;
        protected final ConfigurationMetadata metadata;

        public SingletonConfigInfo(String name, ConfigurationData configurationData) {
            this.name = name;
            this.metadata = getConfigurationManager().getConfigurationMetadata(name);
            this.configurationData = configurationData;
        }

        public ConfigurationData getConfigurationData() {
            return configurationData;
        }

        public String getName() {
            return name;
        }

        public ConfigurationMetadata getMetadata() {
            return metadata;
        }

        public boolean isResourceExists() {
            return getResource().getChild(this.getName()) != null;
        }

        /**
         * Whether this configuration is configured to inherit from parent configurations - sling:configPropertyInherit .
         */
        public boolean isInherits() {
            Boolean inheritProperty = configurationData.getEffectiveValues()
                    .get("sling:configPropertyInherit", Boolean.class);
            return inheritProperty != null && inheritProperty;
        }

        public List<ValueInfo<?>> getValueInfos() {
            List<ValueInfo<?>> valueInfos = new ArrayList<>();
            for (String propertyName : configurationData.getPropertyNames()) {
                if (!propertyName.startsWith("jcr:") && !propertyName.startsWith("sling:")) {
                    ValueInfo<?> valueInfo = configurationData.getValueInfo(propertyName);
                    valueInfos.add(valueInfo);
                }
            }
            Collections.sort(valueInfos,
                    Comparator.comparingInt(valueInfo ->
                            valueInfo.getPropertyMetadata() != null ? valueInfo.getPropertyMetadata().getOrder() : Integer.MAX_VALUE
                    ));
            return valueInfos;
        }

        public List<PropertyInfo> getPropertyInfos() {
            return getValueInfos().stream()
                    .map(valueInfo -> new PropertyInfo(valueInfo.getName(), valueInfo))
                    .collect(Collectors.toList());
        }

    }

    public class PropertyInfo {

        protected final ValueInfo<?> valueInfo;
        protected final String name;
        protected final ConfigurationMetadata metadata;
        private final Class<?> type;

        public PropertyInfo(String name, ValueInfo<?> valueInfo) {
            this.name = name;
            this.metadata = getConfigurationManager().getConfigurationMetadata(name);
            this.valueInfo = valueInfo;
            this.type = valueInfo != null && valueInfo.getPropertyMetadata() != null ? valueInfo.getPropertyMetadata().getType() : null;
        }

        public ValueInfo<?> getValueInfo() {
            return valueInfo;
        }

        public String getName() {
            return name;
        }

        public ConfigurationMetadata getMetadata() {
            return metadata;
        }

        public boolean isResourceExists() {
            return getResource().getChild(this.getName()) != null;
        }

        public boolean isMultiValue() {
            return type != null && (
                    type.isArray() || Collection.class.isAssignableFrom(type)
            );
        }

        public String getJsonValue() {
            ValueInfo valueInfo = this.getValueInfo();
            Object value = CAConfigModel.this.getResource().getValueMap().get(valueInfo.getName());
            return toJson(value);
        }

        public String getRenderedValue() {
            ValueInfo valueInfo = this.getValueInfo();
            Object value = CAConfigModel.this.getResource().getValueMap().get(valueInfo.getName());
            return CAConfigModel.renderValueAsString(value);
        }

        public boolean isRequired() {
            PropertyMetadata<?> propMetadata = valueInfo.getPropertyMetadata();
            return propMetadata != null && propMetadata.getProperties() != null
                    && "true".equals(propMetadata.getProperties().get("required"));
        }

        /** All properties except "required", which is handled in {@link #isRequired()}. */
        public Properties getProperties() {
            Properties props = new Properties();
            if (valueInfo.getPropertyMetadata() != null && valueInfo.getPropertyMetadata().getProperties() != null) {
                props.putAll(valueInfo.getPropertyMetadata().getProperties());
                props.remove("required");
            }
            return props;
        }

        public String getTypeName() {
            if (type == null) {
                return null;
            }
            if (valueInfo != null && valueInfo.getPropertyMetadata() != null
                    && valueInfo.getPropertyMetadata().getProperties() != null) {
                Map<String, String> props = valueInfo.getPropertyMetadata().getProperties();
                String widgetType = props.get("widgetType");
                if ("pathbrowser".equals(widgetType)) {
                    return "Path";
                }
            }
            return getBasicTypeName(type);
        }

        protected String getBasicTypeName(Class<?> clazz) {
            if (clazz == null) {
                return null;
            }
            if (Long.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)
                    || int.class.isAssignableFrom(clazz)
                    || long.class.isAssignableFrom(clazz)
                    || short.class.isAssignableFrom(clazz)
                    || byte.class.isAssignableFrom(clazz)) {
                return "Long";
            } else if (Double.class.isAssignableFrom(clazz)
                    || Float.class.isAssignableFrom(clazz)
                    || double.class.isAssignableFrom(clazz)
                    || float.class.isAssignableFrom(clazz)
            ) {
                return "Double";
            } else if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
                return "Boolean";
            } else if (String.class.isAssignableFrom(clazz)) {
                return "String";
            } else if (clazz.isArray()) {
                return getBasicTypeName(clazz.getComponentType());
            } else if (Collection.class.isAssignableFrom(clazz)) {
                // cannot determine that. :-( String works probably.
                return "String";
            } else {
                return clazz.getName();
            }
        }

    }

    public static String renderValueInfoAsString(Object valueInfo) {
        Object object = ((ValueInfo<?>) valueInfo).getEffectiveValue();
        if (object == null) {
            return "";
        } else if (object instanceof ConfigurationData) {
            ConfigurationData nestedData = ((ConfigurationData) object);
            Map<String, String> stringMap = new HashMap<>();
            for (String key : nestedData.getPropertyNames()) {
                ValueInfo<?> nestedValueInfo = nestedData.getValueInfo(key);
                String nestedValueAsString = renderValueInfoAsString(nestedValueInfo);
                stringMap.put(key, nestedValueAsString);
            }
            return stringMap.toString();
        } else if (Object[].class.isAssignableFrom(object.getClass())) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (Object[]) object) {
                if (builder.length() > 0) {
                    builder.append("<br/>");
                }
                builder.append(item);
            }
            return builder.toString();
        } else {
            return object.toString();
        }
    }

    public static String renderValueAsString(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof ValueMap) {
            // we don't want to render system properties, just stuff that belongs to the configuration.
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((ValueMap) value).entrySet()) {
                if (!entry.getKey().startsWith("jcr:")) {
                    map.put(entry.getKey(), renderValueAsString(entry.getValue()));
                }
            }
            return map.toString();
        }
        return toJson(value);
    }

    protected static String toJson(Object value) {
        Writer writer = new StringWriter();
        @NotNull JsonWriter jsonWriter = new JsonWriter(writer);
        try {
            JsonUtil.jsonValue(jsonWriter, value);
        } catch (IOException e) {
            LOG.error("Error in converting to String: {}", value, e);
            return "(error)";
        }
        return writer.toString();
    }

}
