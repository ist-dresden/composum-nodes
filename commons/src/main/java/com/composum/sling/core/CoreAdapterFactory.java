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

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mirko Zeibig
 */
@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes (Core) Library Adapters :  Adapts Resources to ResourceHandles",
                "adaptables=org.apache.sling.api.resource.Resource",
                "adapters=com.composum.sling.core.ResourceHandle"
        }
)
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
     * @param type           target type
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
     * @param type     target type
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
