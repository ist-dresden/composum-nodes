package com.composum.nodes.debugutil;

import java.util.Arrays;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs OSGI events.
 */
@Component(
        service = {EventHandler.class},
        property = {
                EventConstants.EVENT_TOPIC + "=*"
        },
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = OsgiEventLogger.Config.class)
public class OsgiEventLogger implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiEventLogger.class);

    @Override
    public void handleEvent(Event event) {
        if (config == null || !config.enabled() || event.getTopic().startsWith("org/osgi/framework/")) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        buf.append("Event ").append(event.getTopic()).append(" props ").append(Arrays.asList(event.getPropertyNames()));
        for (String prop : Arrays.asList("path", "userid", "resourceType", "service.id")) {
            if (event.containsProperty(prop)) {
                buf.append(" ").append(prop).append("=").append(event.getProperty(prop));
            }
        }
        if (event.containsProperty("service.objectClass")) {
            buf.append(" service.objectClass=").append(Arrays.asList((String[]) event.getProperty("service" +
                    ".objectClass")));
        }
        LOG.info(buf.toString());
    }

    protected Config config;

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil OSGI Event Logger",
            description = "Logs OSGI events if enabled."
    )
    public @interface Config {
        @AttributeDefinition(
                description = "Enable the event logger"
        )
        boolean enabled() default false;
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

}
