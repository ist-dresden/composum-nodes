package com.composum.nodes.debugutil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs resource changes matching a given regex.
 */
@Component(
        service = {ResourceChangeListener.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Resource Change Listener",
                ResourceChangeListener.PATHS + "=/",
//                ResourceChangeListener.PATHS + "=/var/composum",
//                ResourceChangeListener.PATHS + "=/content",
//                ResourceChangeListener.PATHS + "=/conf",
//                ResourceChangeListener.PATHS + "=/apps",
//                ResourceChangeListener.PATHS + "=/libs",
//                ResourceChangeListener.PATHS + "=/jcr:system/jcr:versionStorage",
//                ResourceChangeListener.PATHS + "=/public",
//                ResourceChangeListener.PATHS + "=/preview",
//                ResourceChangeListener.PATHS + "=/tmp",
        },
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ResourceChangeLogger.Config.class)
public class ResourceChangeLogger implements ResourceChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceChangeLogger.class);

    protected volatile Pattern regex;

    @Override
    public void onChange(List<ResourceChange> rawChanges) {
        if (config == null || config.enabled() || regex == null) {
            return;
        }
        StringBuffer buf = new StringBuffer("RESOURCE CHANGE: ");
        List<ResourceChange> changes = rawChanges.stream().filter((chg) ->
                regex.matcher(chg.getPath()).matches()
        ).collect(Collectors.toList());
        Collections.sort(changes, Comparator.comparing((r) -> r.getPath()));
        for (ResourceChange change : changes) {
            buf.append("\n        ").append(change.getType()).append(" : ").append(change.getPath());
        }
        if ((!changes.isEmpty())) {
            LOG.info(buf.toString());
        }
    }

    protected Config config;

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Resource Change Logger",
            description = "If enabled, this logs resource changes that match a given regular expression."
    )
    public @interface Config {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;

        @AttributeDefinition(description = "Regex for changes we need to log, e.g. ^(/public|/preview|/content|/var/composum)")
        String regex();
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.regex = null;
        this.config = config;
        if (config.enabled() && StringUtils.isNotBlank(config.regex())) {
            try {
                this.regex = Pattern.compile(config.regex());
            } catch (PatternSyntaxException e) {
                LOG.error("Broken regex {}", config.regex(), e);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

}
