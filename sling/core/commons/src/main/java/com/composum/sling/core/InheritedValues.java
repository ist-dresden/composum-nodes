package com.composum.sling.core;

import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

import java.util.HashMap;

/**
 * The value map which extends the property retrieval to the context (parents,...) of a resource.
 */
public class InheritedValues extends HashMap<String, Object> implements ValueMap {

    public static final Object UNDEFINED = new String("");

    protected ResourceHandle resource;

    protected transient ResourceHandle entryPoint;
    protected transient String relativePath;

    public InheritedValues(ResourceHandle resource) {
        this.resource = resource;
    }

    /**
     * Gets an inherited value.
     *
     * @param name the property name or path
     * @param <T>  the expected type of the value
     * @return inherited value if existing or <code>null</code>
     */
    @Override
    public <T> T get(String name, Class<T> type) {
        Object value = get(name);
        if (value == null) {
            value = findInherited(name, type);
            if (value == null) {
                value = UNDEFINED;
            }
            put(name, value); // cache the found value
        }
        return value != UNDEFINED ? (T)value : null;
    }

    /**
     * Gets an inherited value.
     *
     * @param name         the property name or path
     * @param defaultValue the default value, must not be <code>null</code>
     * @param <T>          the expected type of the value
     * @return inherited value if existing, otherwise the default value
     */
    @Override
    public <T> T get(String name, T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = (T) get(name, type);
        return value != null ? value : defaultValue;
    }

    protected <T> T findInherited(String name, Class<T> type) {
        findEntryPoint();
        T value = scanNodeHierarchy(name, type);
        // TODO if not found scan in the 'design' or context definition
        return value;
    }

    /**
     * Searches the value along the repositories hierarchy by the entry point and path determined before.
     *
     * @param name the property name or path
     * @param <T>  the expected type of the value
     * @return the value found or <code>null</code> if no such value found
     * in one of the appropriate parent nodes
     */
    protected <T> T scanNodeHierarchy(String name, Class<T> type) {
        T value;
        String path = relativePath + name;
        for (ResourceHandle parent = entryPoint; parent != null; parent = parent.getParent()) {
            value = parent.getProperty(path, type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Retrieves the first parent node for inheritance traversal
     * and defines the relativePath of the property.
     */
    protected void findEntryPoint() {
        if (entryPoint == null) {
            StringBuilder path = new StringBuilder();
            ResourceHandle parent = resource.getParent();
            String name = parent.getName();
            while (parent != null && !ResourceUtil.CONTENT_NODE.equals(name)) {
                if (path.length() > 0) {
                    path.insert(0, '/');
                }
                path.insert(0, name);
                parent = parent.getParent();
                name = parent.getName();
            }
            if (parent != null && ResourceUtil.CONTENT_NODE.equals(name)) {
                // the resource is a child of an element with a content subtree ('jcr:content/...')
                path.insert(0, '/');
                path.insert(0, ResourceUtil.CONTENT_NODE);
                path.append('/');
                relativePath = path.toString();
                entryPoint = parent.getParent();
            } else {
                // no content subtree ('jcr:content/...') detected, use node inheritance
                relativePath = "";
                entryPoint = resource.getParent();
            }
        }
    }
}
