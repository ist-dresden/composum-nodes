package com.composum.sling.nodes.components;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.BeanContext;
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
        return names.stream()
                .filter(name ->
                        requireNonNull(getConfigurationManager().getConfigurationMetadata(name)).isSingleton())
                .map(name ->
                        new SingletonConfigInfo(name, getConfigurationManager().getConfiguration(resource, name)))
                .collect(Collectors.toList());
    }

    public List<CollectionConfigInfo> getCollectionConfigurations() {
        SortedSet<String> names = getConfigurationManager().getConfigurationNames();
        return names.stream()
                .filter(name ->
                        requireNonNull(getConfigurationManager().getConfigurationMetadata(name)).isCollection())
                .map(name -> new CollectionConfigInfo(getConfigurationManager().getConfigurationCollection(resource, name)))
                .collect(Collectors.toList());
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

    public class CollectionConfigInfo {

        protected final ConfigurationCollectionData collectionConfigData;
        protected final ConfigurationMetadata metadata;

        public CollectionConfigInfo(ConfigurationCollectionData configurationCollection) {
            this.collectionConfigData = configurationCollection;
            this.metadata = getConfigurationManager().getConfigurationMetadata(collectionConfigData.getConfigName());
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
