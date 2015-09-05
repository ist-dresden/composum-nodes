package com.composum.sling.core.config;

import com.composum.sling.core.filter.ResourceFilter;

/**
 * the interface for the elements of the general OSGi resource filter configuration set
 */
public interface FilterConfiguration {

    /**
     * the name to identify and select a configred filter
     */
    String getName();

    /**
     * the filter instance created from the filter rule in the configuration
     */
    ResourceFilter getFilter();
}