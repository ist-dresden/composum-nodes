package com.composum.sling.core.config;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * the implementation for the elements of the general OSGi resource filter configuration set
 */
@Component(
        name = "ComposumFilterConfiguration",
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Filter Configuration"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
@Designate(ocd = FilterConfigurationImpl.Configuration.class, factory = true)
public class FilterConfigurationImpl implements FilterConfiguration {

    @ObjectClassDefinition(name = "Composum Filter Configuration", description = "the configurable set of node filters for the node tree view")
    public @interface Configuration {

        @AttributeDefinition(
                name = "Name",
                description = "name to identify and select the filter"
        )
        String name() default "";

        @AttributeDefinition(
                name = "Filter",
                description = "the resource filter definition rule"
        )
        String filter() default "";

    }

    protected volatile String name;

    protected volatile ResourceFilter filter;

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

    /**
     * creates the filter instance for the configured rule
     */
    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        name = configuration.name();
        filter = ResourceFilterMapping.fromString(configuration.filter());
    }

    @Deactivate
    protected void deactivate() {
        name = null;
        filter = null;
    }
}
