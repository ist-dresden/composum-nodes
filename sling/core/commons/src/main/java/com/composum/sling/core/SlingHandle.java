package com.composum.sling.core;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

/**
 * the wrapper to access the Sling engine from any type of context (JSP, Groovy, Servlet, ...)
 */
public class SlingHandle {

    // initialized attributes

    protected BeanContext context;

    // attributes retrieved on demand

    private transient SlingBindings slingBindings;
    private transient SlingScriptHelper scriptHelper;

    /**
     * construction based on any kind of BeanContext
     */
    public SlingHandle(BeanContext context) {
        this.context = context;
    }

    /**
     * returns the 'sling' attribute of the 'slingBindings'
     */
    public SlingScriptHelper getScriptHelper() {
        if (scriptHelper == null) {
            scriptHelper = getSlingBindings().getSling();
        }
        return scriptHelper;
    }

    /**
     * retrieves the 'slingBindings' attribute from the context
     */
    public SlingBindings getSlingBindings() {
        if (slingBindings == null) {
            slingBindings = (SlingBindings) context.getRequest().getAttribute(SlingBindings.class.getName());
        }
        return slingBindings;
    }

    /**
     * retrieves a service implementation using the 'sling' script helper
     */
    public <T> T getService(Class<T> type) {
        return getScriptHelper().getService(type);
    }

    /**
     * retrieves a class using the Slings DynamicClassLoaderManager implementation
     */
    public Class<?> getType(String className) throws ClassNotFoundException {
        Class<?> type = null;
        // use Sling DynamicClassLoader
        if (getScriptHelper() != null) {
            DynamicClassLoaderManager dclm = getService(DynamicClassLoaderManager.class);
            if (dclm != null) {
                type = dclm.getDynamicClassLoader().loadClass(className);
            }
        }
        // fallback to default ClassLoader
        if (type == null) {
            type = Class.forName(className);
        }
        return type;
    }
}
