package com.composum.sling.nodes.components;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;

@Restricted(key = NodeServlet.SERVICE_KEY)
public class CAConfigModel extends ConsoleServletBean {

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
                .map(SingletonConfigInfo::new)
                .collect(Collectors.toList());
    }

    public class SingletonConfigInfo {

        protected final ConfigurationData configurationData;
        protected final String name;
        protected final ConfigurationMetadata metadata;
        protected final List<ValueInfo<?>> valueInfos = new ArrayList<>();

        public SingletonConfigInfo(String name) {
            this.name = name;
            this.metadata = getConfigurationManager().getConfigurationMetadata(name);
            this.configurationData = getConfigurationManager().getConfiguration(resource, name);

            for (String propertyName : configurationData.getPropertyNames()) {
                ValueInfo<?> valueInfo = configurationData.getValueInfo(propertyName);
                valueInfos.add(valueInfo);
            }
            Collections.sort(valueInfos,
                    Comparator.comparing(valueInfo -> valueInfo.getPropertyMetadata().getOrder()));
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
            return valueInfos;
        }

    }

}
