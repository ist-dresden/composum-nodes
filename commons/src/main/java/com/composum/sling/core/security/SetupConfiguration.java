package com.composum.sling.core.security;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.servlet.SetupServlet;
import com.composum.sling.core.util.RequestUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Restricted(key = SetupServlet.SERVICE_KEY)
public class SetupConfiguration extends AbstractSlingBean {

    @Restricted(key = SetupServlet.SERVICE_KEY)
    public class ScriptBean extends AbstractSlingBean implements Comparable<ScriptBean> {

        private final ConfigurationSet.ConfigScript script;

        public ScriptBean(ConfigurationSet.ConfigScript script) {
            this.script = script;
            initialize(SetupConfiguration.this.context,
                    SetupConfiguration.this.context.getResolver().getResource(script.getPath()));
        }

        @NotNull
        public String getScriptUrl() {
            return getRequest().getContextPath() + "/bin/browser.html" + getScript().getScriptPath();
        }

        @NotNull
        public ConfigurationSet.ConfigScript getScript() {
            return script;
        }

        @Override
        public int compareTo(@NotNull ScriptBean other) {
            return script.compareTo(other.script);
        }
    }

    @Restricted(key = SetupServlet.SERVICE_KEY)
    public class ConfigBean extends AbstractSlingBean implements Comparable<ConfigBean> {

        private final ConfigurationSet.Configuration config;
        private final List<ScriptBean> scripts;

        public ConfigBean(ConfigurationSet.Configuration config, List<ScriptBean> scripts) {
            this.config = config;
            this.scripts = scripts;
            initialize(SetupConfiguration.this.context,
                    SetupConfiguration.this.context.getResolver().getResource(config.getPath()));
        }

        @NotNull
        public ConfigurationSet.Configuration getConfig() {
            return config;
        }

        @NotNull
        public List<ScriptBean> getScripts() {
            return scripts;
        }

        @Override
        public int compareTo(@NotNull ConfigBean other) {
            return config.compareTo(other.config);
        }
    }

    private transient Collection<String> categories;
    private transient List<ConfigBean> matchingSet;
    private transient ConfigurationSet configurationSet;

    public Collection<String> getCategories() {
        if (categories == null) {
            categories = getConfigurationSet().getScriptCategories().keySet();
        }
        return categories;
    }

    public List<ConfigBean> getMatchingSet() {
        if (matchingSet == null) {
            matchingSet = new ArrayList<>();
            String[] parameter = getRequest().getParameterValues("category");
            Collection<String> categories = parameter == null || parameter.length == 0 ? getCategories() : Arrays.asList(parameter);
            ConfigurationSet configurationSet = getConfigurationSet();
            for (ConfigurationSet.Configuration configuration : configurationSet.getConfigurations().values()) {
                List<ScriptBean> scripts = new ArrayList<>();
                for (ConfigurationSet.ConfigScript script : configuration.getScripts().values()) {
                    if (configuration.matches(categories) || script.matches(categories)) {
                        scripts.add(new ScriptBean(script));
                    }
                }
                if (scripts.size() > 0) {
                    Collections.sort(scripts);
                    matchingSet.add(new ConfigBean(scripts.get(0).getScript().getConfiguration(), scripts));
                }
            }
            Collections.sort(matchingSet);
        }
        return matchingSet;
    }

    @NotNull
    public ConfigurationSet getConfigurationSet() {
        if (configurationSet == null) {
            SlingHttpServletRequest request = getRequest();
            if (RequestUtil.checkSelector(request, "refresh")) {
                ConfigurationSet.clear(request);
            }
            configurationSet = ConfigurationSet.instance(request);
        }
        return configurationSet;
    }

    public SetupConfiguration(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public SetupConfiguration(BeanContext context) {
        super(context);
    }

    public SetupConfiguration() {
    }
}
