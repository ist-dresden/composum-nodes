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
@Component(metatype = true,
           label = "Composum Core Sling Library Adapters",
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
     * Verarbeitet <code>resourceHandle.adaptTo(ResourceHandle.class)</code>, indem das Original geliefert wird.
     *
     * @param resourceHandle das zu adaptierende Objekt
     * @param type der Zieltyp
     * @return das Original
     */
    protected static <AdapterType> AdapterType getAdapter(ResourceHandle resourceHandle, Class<AdapterType> type) {
        if (type == ResourceHandle.class) {
            return type.cast(resourceHandle);
        }
        log.info("Unable to adapt resource on {} to type {}", resourceHandle.getPath(), type.getName());
        return null;
    }

    /**
     * Verarbeitet <code>resource.adaptTo(ResourceHandle.class)</code>, indem das Original gewrappt wird.
     *
     * @param resource das zu adaptierende Objekt
     * @param type der Zieltyp
     * @return die gewrappte Resource
     */
    protected static <AdapterType> AdapterType getAdapter(Resource resource, Class<AdapterType> type) {
        if (type == ResourceHandle.class) {
            return type.cast(new ResourceHandle(resource));
        }
        log.info("Unable to adapt resource on {} to type {}", resource.getPath(), type.getName());
        return null;
    }

}
