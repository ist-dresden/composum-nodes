package com.composum.sling.nodes.components;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;

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

}
