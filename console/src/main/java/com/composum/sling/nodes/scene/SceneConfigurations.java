package com.composum.sling.nodes.scene;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.CoreConstants;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.query.Query;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class SceneConfigurations implements Serializable {

    public static final String SA_INSTANCE = SceneConfigurations.class.getName() + "#instance";

    public static final String RT_SCENE_CONFIG_SET = "composum/nodes/console/scene";
    public static final String ORDER = "order";
    public static final int ORDER_DEFAULT = 50;

    public static final String PROP_ID = "id";
    public static final String PROP_KEY = "key";
    public static final String PROP_URI = "uri";
    public static final String PROP_DISABLED = "disabled";
    public static final String PROP_TEMPLATE = "template";

    public static final String QUERY_BASE = "/jcr:root";
    public static final String QUERY_RULE = "/*[@sling:resourceType='" + RT_SCENE_CONFIG_SET + "']";

    public class Config implements Comparable<Config>, Serializable {

        public class Tool {

            private final String name;
            private final String uri;
            private final String label;
            private final String description;

            public Tool(ResourceHandle handle) {
                name = handle.getName();
                uri = handle.getProperty(PROP_URI, "");
                label = handle.getTitle();
                description = handle.getProperty(CoreConstants.JCR_DESCRIPTION, "");
            }

            public String getName() {
                return name;
            }

            public String getUri() {
                return uri;
            }

            public String getLabel() {
                return label;
            }

            public String getDescription() {
                return description;
            }
        }

        private final int order;
        private final String path;
        private final String key;
        private final boolean disabled;
        private final Map<String, Tool> tools;

        public Config(ResourceHandle handle) {
            order = handle.getProperty(ORDER, ORDER_DEFAULT);
            key = handle.getProperty(PROP_KEY, handle.getName());
            path = handle.getPath();
            disabled = handle.getProperty(PROP_DISABLED, Boolean.FALSE);
            tools = new LinkedHashMap<>();
            for (Resource child : handle.getChildren()) {
                Tool tool = new Tool(ResourceHandle.use(child));
                tools.put(tool.getName(), tool);
            }
        }

        public String getPath() {
            return path;
        }

        public String getKey() {
            return key;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public Tool getTool(String id) {
            return tools.get(id);
        }

        public Collection<Tool> getTools() {
            return tools.values();
        }

        @Override
        public int compareTo(Config other) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(order, other.order);
            builder.append(getPath(), other.getPath());
            return builder.toComparison();
        }
    }

    private final TreeMap<String, Config> sceneConfigs;

    @Nonnull
    public static SceneConfigurations instance(@Nonnull final SlingHttpServletRequest request) {
        HttpSession session = request.getSession(true);
        SceneConfigurations instance = null;
        try {  // try to use cached configuration
            instance = (SceneConfigurations) session.getAttribute(SA_INSTANCE);
        } catch (ClassCastException ignore) {
        }
        if (instance == null) {
            instance = new SceneConfigurations(request.getResourceResolver());
            session.setAttribute(SA_INSTANCE, instance);
        }
        return instance;
    }

    @Nullable
    public Config getSceneConfig(String name) {
        return sceneConfigs.get(name);
    }

    @Nonnull
    public Collection<Config> getSceneConfigs() {
        return sceneConfigs.values();
    }

    public SceneConfigurations(@Nonnull final ResourceResolver resolver) {
        sceneConfigs = new TreeMap<>();
        for (String path : resolver.getSearchPath()) {
            findSceneConfigs(sceneConfigs, resolver, QUERY_BASE + path + QUERY_RULE);
        }
    }

    protected void findSceneConfigs(@Nonnull final TreeMap<String, Config> pageConfigs,
                                    @Nonnull final ResourceResolver resolver, @Nonnull final String query) {
        @SuppressWarnings("deprecation")
        Iterator<Resource> pageConfigResources = resolver.findResources(query, Query.XPATH);
        while (pageConfigResources.hasNext()) {
            Config pageConfig = new Config(ResourceHandle.use(pageConfigResources.next()));
            if (!pageConfig.isDisabled() && !pageConfigs.containsKey(pageConfig.getKey())) {
                pageConfigs.put(pageConfig.getKey(), pageConfig);
            }
        }
    }
}
