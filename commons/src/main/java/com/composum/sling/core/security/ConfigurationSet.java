package com.composum.sling.core.security;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.jcr.query.Query;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.composum.sling.core.util.CoreConstants.PROP_DESCRIPTION;

public class ConfigurationSet implements Serializable {

    public static final String SA_INSTANCE = ConfigurationSet.class.getName() + "#instance";

    public static final String PN_SCRIPT = "script";
    public static final String PN_AUTO_SETUP = "autoSetup";
    public static final String PN_RANK = "rank";

    public static final String QUERY =
            "/jcr:root/conf//config[@sling:resourceType='composum/nodes/commons/components/security/config']";

    public class ConfigScript implements Comparable<ConfigScript>, Serializable {

        private final Configuration configuration;
        private final List<String> category;
        private final String label;
        private final String name;
        private final String path;
        private final String description;
        private final boolean autoSetup;
        private final String scriptPath;
        private final Long rank;

        public ConfigScript(@Nonnull final Configuration configuration, @Nonnull final ResourceHandle handle) {
            this.configuration = configuration;
            category = Arrays.asList(handle.getProperty("category", new String[0]));
            label = handle.getTitle();
            name = handle.getName();
            path = handle.getPath();
            description = handle.getProperty(PROP_DESCRIPTION, "");
            scriptPath = handle.getProperty(PN_SCRIPT, "");
            autoSetup = handle.getProperty(PN_AUTO_SETUP, Boolean.FALSE);
            rank = handle.getProperty(PN_RANK, Long.class);
        }

        @Nonnull
        public Configuration getConfiguration() {
            return configuration;
        }

        public boolean matches(@Nonnull final Collection<String> category) {
            for (String ownCategory : getCategory()) {
                if (category.contains(ownCategory)) {
                    return true;
                }
            }
            return false;
        }

        @Nonnull
        public List<String> getCategory() {
            return category;
        }

        @Nonnull
        public String getCategories() {
            return StringUtils.join(getCategory(), ",");
        }

        public String getRank() {
            return rank != null ? Long.toString(rank) : null;
        }

        @Nonnull
        public String getLabel() {
            return label;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        @Nonnull
        public String getDescription() {
            return description;
        }

        public boolean isAutoSetup() {
            return autoSetup;
        }

        @Nonnull
        public String getScriptPath() {
            return scriptPath;
        }

        @Override
        public int compareTo(ConfigScript other) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(rank != null ? String.format("%04d", rank) : "0050", other.rank != null ? String.format("%04d", other.rank) : "0050");
            builder.append(getLabel(), other.getLabel());
            return builder.toComparison();
        }
    }

    public class Configuration implements Comparable<Configuration>, Serializable {

        private final List<String> category;
        private final String label;
        private final String name;
        private final String path;
        private final String description;
        private final Map<String, ConfigScript> scripts = new TreeMap<>();
        private final Long rank;

        public Configuration(@Nonnull final ResourceHandle handle) {
            category = Arrays.asList(handle.getProperty("category", new String[0]));
            label = handle.getTitle();
            name = handle.getName();
            path = handle.getPath();
            description = handle.getProperty(PROP_DESCRIPTION, "");
            rank = handle.getProperty(PN_RANK, Long.class);
            for (final Resource scriptRes : handle.getChildren()) {
                addScript(new ConfigScript(this, ResourceHandle.use(scriptRes)));
            }
        }

        public boolean matches(@Nonnull final Collection<String> category) {
            for (String ownCategory : getCategory()) {
                if (category.contains(ownCategory)) {
                    return true;
                }
            }
            return false;
        }

        @Nonnull
        public List<String> getCategory() {
            return category;
        }

        @Nonnull
        public String getCategories() {
            return StringUtils.join(getCategory(), ",");
        }

        public String getRank() {
            return rank != null ? Long.toString(rank) : null;
        }

        @Nonnull
        public String getLabel() {
            return label;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        @Nonnull
        public String getDescription() {
            return description;
        }

        @Nonnull
        public Map<String, ConfigScript> getScripts() {
            return scripts;
        }

        public void addScript(@Nonnull final ConfigScript script) {
            scripts.put(script.getLabel(), script);
            for (final String category : getCategory()) {
                final List<ConfigScript> categoryScripts = scriptCategories.computeIfAbsent(category, k -> new ArrayList<>());
                if (!categoryScripts.contains(script)) {
                    categoryScripts.add(script);
                    Collections.sort(categoryScripts);
                }
            }
            for (final String category : script.getCategory()) {
                final List<ConfigScript> categoryScripts = scriptCategories.computeIfAbsent(category, k -> new ArrayList<>());
                if (!categoryScripts.contains(script)) {
                    categoryScripts.add(script);
                    Collections.sort(categoryScripts);
                }
                final List<Configuration> categoryConfigurations = configCategories.computeIfAbsent(category, k -> new ArrayList<>());
                if (!categoryConfigurations.contains(this)) {
                    categoryConfigurations.add(this);
                    Collections.sort(categoryConfigurations);
                }
            }
        }

        @Override
        public int compareTo(Configuration other) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(rank != null ? String.format("%04d", rank) : "0050", other.rank != null ? String.format("%04d", other.rank) : "0050");
            builder.append(getLabel(), other.getLabel());
            return builder.toComparison();
        }
    }

    protected final Map<String, Configuration> configurations = new TreeMap<>();
    protected final Map<String, List<Configuration>> configCategories = new TreeMap<>();
    protected final Map<String, List<ConfigScript>> scriptCategories = new TreeMap<>();

    @SuppressWarnings("deprecation")
    public ConfigurationSet(@Nonnull final ResourceResolver resolver) {
        final Iterator<Resource> configIterator = resolver.findResources(QUERY, Query.XPATH);
        while (configIterator.hasNext()) {
            addConfiguration(new Configuration(ResourceHandle.use(configIterator.next())));
        }
    }

    public Map<String, List<Configuration>> getConfigCategories() {
        return configCategories;
    }

    public Map<String, List<ConfigScript>> getScriptCategories() {
        return scriptCategories;
    }

    public Map<String, Configuration> getConfigurations() {
        return configurations;
    }

    public void addConfiguration(@Nonnull final Configuration configuration) {
        configurations.put(configuration.getLabel(), configuration);
        for (final String category : configuration.getCategory()) {
            final List<Configuration> categoryConfigurations = configCategories.computeIfAbsent(category, k -> new ArrayList<>());
            if (!categoryConfigurations.contains(configuration)) {
                categoryConfigurations.add(configuration);
                Collections.sort(categoryConfigurations);
            }
        }
    }

    @Nonnull
    public static ConfigurationSet instance(@Nonnull final SlingHttpServletRequest request) {
        ConfigurationSet instance = null;
        final HttpSession session = request.getSession(false);
        if (session != null) {
            try {  // try to use cached configuration
                instance = (ConfigurationSet) session.getAttribute(SA_INSTANCE);
            } catch (ClassCastException ignore) {
            }
        }
        if (instance == null) {
            instance = new ConfigurationSet(request.getResourceResolver());
            if (session != null) {
                session.setAttribute(SA_INSTANCE, instance);
            }
        }
        return instance;
    }

    public static void clear(@Nonnull final SlingHttpServletRequest request) {
        ConfigurationSet instance = null;
        final HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SA_INSTANCE);
        }
    }
}
