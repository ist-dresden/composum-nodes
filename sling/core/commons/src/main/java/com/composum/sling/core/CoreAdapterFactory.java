/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: CoreAdapterFactory.java
 * Autor: Mirko Zeibig
 * Datum: 11.01.2013 08:58:09
 */

package com.composum.sling.core;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mirko Zeibig
 *
 */
@Component(label = "Composum Core Sling Library Adapters",
           description = " Adapts Resources to ResourceHandles")
@Properties({
    @Property(name = "service.description", value = "Composum Core Sling Library Adapters"),
    @Property(name = "adaptables", value="org.apache.sling.api.resource.Resource", propertyPrivate = true),
    @Property(name = "adapters", value="com.composum.sling.core.ResourceHandle", propertyPrivate = true)
})
@Service
public class CoreAdapterFactory implements AdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(CoreAdapterFactory.class);

    /**
     * @see org.apache.sling.api.adapter.AdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof ResourceHandle) {
            return getAdapter((ResourceHandle) adaptable, type);
        } else if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        }
        log.warn("Unable to handle adaptable {}", adaptable.getClass().getName());
        return null;
    }

    /**
     * Handles <code>resourceHandle.adaptTo(ResourceHandle.class)</code>.
     *
     * @param resourceHandle Object to adapt
     * @param type target type
     * @return original Object
     */
    protected static <AdapterType> AdapterType getAdapter(ResourceHandle resourceHandle, Class<AdapterType> type) {
        if (type == ResourceHandle.class) {
            return type.cast(resourceHandle);
        }
        log.info("Unable to adapt resource on {} to type {}", resourceHandle.getPath(), type.getName());
        return null;
    }

    /**
     * Handles <code>resource.adaptTo(ResourceHandle.class)</code>, to wrap a resource with an ResourceHandle.
     *
     * @param resource resource to adapt/wrap
     * @param type target type
     * @return wrapped resource
     */
    protected static <AdapterType> AdapterType getAdapter(Resource resource, Class<AdapterType> type) {
        if (type == ResourceHandle.class) {
            return type.cast(new ResourceHandle(resource));
        }
        log.info("Unable to adapt resource on {} to type {}", resource.getPath(), type.getName());
        return null;
    }

}
