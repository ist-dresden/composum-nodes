package com.composum.sling.core;

import org.apache.sling.api.resource.Resource;

/**
 *
 */
public class AssetHandle extends ResourceHandle {

    /**
     * Creates a new wrapper instance delegating all method calls to the given
     * <code>resource</code>.
     *
     * @param resource
     *            the resource to wrap
     */
    public AssetHandle(Resource resource) {
        super(resource);
    }
}
