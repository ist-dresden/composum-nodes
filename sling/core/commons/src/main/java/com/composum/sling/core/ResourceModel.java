package com.composum.sling.core;

import org.apache.sling.api.resource.Resource;

public class ResourceModel extends AbstractSlingBean {

    /**
     * initialize bean using the context an the resource given explicitly
     */
    public ResourceModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    /**
     * initialize bean using the context with the 'resource' attribute within
     */
    public ResourceModel(BeanContext context) {
        super(context);
    }

    /**
     * if this constructor is used, the bean must be initialized using the 'initialize' method!
     */
    public ResourceModel() {
    }
}
