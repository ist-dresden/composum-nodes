package com.composum.sling.core.config;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

/**
 * the implementation for the elements of the general OSGi resource filter configuration set
 */
@Component(
        name = "ComposumFilterConfiguration",
        label = "Composum Filter Configuration",
        description = "the configurable set of node filters for the node tree view",
        configurationFactory = true, policy = ConfigurationPolicy.REQUIRE,
        metatype = true, immediate = true)
@Service()
public class FilterConfigurationImpl implements FilterConfiguration {

    /**
     * the name to identify and select a configured filter
     */
    public String getName() {
        return name;
    }

    /**
     * the filter instance created from the filter rule in the configuration
     */
    public ResourceFilter getFilter() {
        return filter;
    }

    @Property(
            name = "name",
            label = "Name",
            description = "name to identify and select the filter",
            value = ""
    )
    protected String name;

    @Property(
            name = "filter",
            label = "Filter",
            description = "the resource filter definition rule",
            value = ""
    )
    protected ResourceFilter filter;

    /**
     * creates the filter instance for the configured rule
     */
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary properties = context.getProperties();
        name = (String) properties.get("name");
        filter = ResourceFilterMapping.fromString((String) properties.get("filter"));
    }
}