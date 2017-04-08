package com.composum.sling.core;

import org.apache.sling.api.resource.Resource;

/**
 * The interface for 'Beans' to implement a Model based on e JCR resource without a mapping framework.
 * Such a 'bean' can be declared as variable in aJSP context using the 'component' tag of the Composum 'nodes'
 * tag library (cpnl).
 */
public interface SlingBean {

    /**
     * This basic initialization sets up the context and resource attributes only,
     * all the other attributes are set 'lazy' during their getter calls.
     *
     * @param context  the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     * @param resource the resource to use (normally the resource addressed by the request)
     */
    void initialize(BeanContext context, Resource resource);

    /**
     * Uses the context for initialization - must call the 'main' initialization
     * - initialize(context,resource) - with the resource determined from the context.
     *
     * @param context the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     */
    void initialize(BeanContext context);

    /**
     * returns the name of the resource wrapped by this bean
     */
    String getName();

    /**
     * returns the path of the resource wrapped by this bean
     */
    String getPath();

    /**
     * returns the type of the resource wrapped by this bean
     */
    String getType();
}
