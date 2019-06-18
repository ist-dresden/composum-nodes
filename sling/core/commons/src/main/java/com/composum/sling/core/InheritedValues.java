package com.composum.sling.core;

import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.Validate;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.HashMap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * The value map which extends the property retrieval to the context (parents,...) of a resource.
 */
public class InheritedValues extends HashMap<String, Object> implements ValueMap {

    /**
     * the various types of inheritance
     * <dl>
     * <dt>contentRelated</dt>
     * <dd>
     * Each node at the same position relative to the 'jcr:content' nodes is checked for an inherited value.
     * <code><pre>
     *     - jcr:content/aaa/bbb/{relative/property/path}
     *       - ...
     *       - jcr:content/aaa/bbb/{relative/property/path}
     * </pre></code></dd>
     * <dt>contentBased</dt>
     * <dd>
     * The 'jcr:content' parents are the base to retrieve the inherited value.
     * <code><pre>
     *     - jcr:content/{relative/property/path}
     *       - ...
     *       - jcr:content/aaa/.../xxx/{relative/property/path}
     * </pre></code></dd>
     * <dt>nodeRelated</dt>
     * <dd>
     * Each parent node can store an appropriate value.
     * <code><pre>
     *     - jcr:content/{relative/property/path}
     *       - aaa
     *         - bbb/{relative/property/path}
     *           ...
     *           - xxx/{relative/property/path}
     * </pre></code>
     * Can be restricted to the subtree of 'jcr:content/...'.</dd>
     * <dt>sameContent</dt>
     * <dd>
     * The same as 'nodeRelated' but the search is stopped at the next 'jcr:content' parent node if present.
     * </dd>
     * </dl>
     */
    public enum Type {
        contentRelated, contentBased, nodeRelated, sameContent,
        /**
         * Special value to signify that a (context dependent) "default" inheritance type is to be used, for usage in
         * annotation attributes to specify defaults, since a null value cannot be used there. Only use this in
         * annotations, not in other code .Context-dependent; it is a runtime error if this is actually used in
         * ResourceHandle.
         */
        useDefault
    }

    public static final Object UNDEFINED = "";

    protected final Resource resource;
    protected final Type inheritanceType;

    protected transient Resource exitPoint;
    protected transient Resource entryPoint;
    protected transient String relativePath;

    public InheritedValues(Resource resource) {
        this(resource, Type.contentRelated);
    }

    public InheritedValues(Resource resource, Type inheritanceType) {
        Validate.notNull(inheritanceType,"Inheritance type null is not permitted.");
        Validate.isTrue(Type.useDefault != inheritanceType, "useDefault is not a valid inheritance strategy");
        this.resource = resource;
        this.inheritanceType = inheritanceType;
    }

    @Deprecated
    public InheritedValues(Resource resource, boolean nodeInheritance) {
        this(resource, Type.sameContent);
    }

    @Deprecated
    public InheritedValues(Resource resource, boolean nodeInheritance, boolean restrictToSameContent) {
        this(resource, nodeInheritance ? (restrictToSameContent ? Type.sameContent : Type.nodeRelated) : Type.contentRelated);
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
        return value != UNDEFINED ? type.cast(value) : null;
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
        T value = get(name, type);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected <T> T findInherited(String name, Class<T> type) {
        HierarchyScanResult found = findOriginAndValue(name, type);
        return found != null ? (T) found.value : null;
    }

    public static class HierarchyScanResult {

        public final Resource origin;
        public final Object value;

        public HierarchyScanResult(Resource origin, Object value) {
            this.origin = origin;
            this.value = value;
        }
    }

    /**
     * Searches the value along the repositories hierarchy by the entry point and path determined before.
     *
     * @param name the property name or path
     * @param type the expected type of the value
     * @return the value found or <code>null</code> if no such value found
     * in one of the appropriate parent nodes
     */
    public HierarchyScanResult findOriginAndValue(String name, Class<?> type) {
        Object value;
        findEntryPoint();
        String path = getRelativePath(name);
        for (Resource parent = entryPoint; parent != null; parent = parent.getParent()) {
            ValueMap parentProps = parent.adaptTo(ValueMap.class);
            if (parentProps != null) {
                value = parentProps.get(path, type);
                if (value != null) {
                    return new HierarchyScanResult(parent, value);
                }
            }
            if (exitPoint != null && parent.getPath().equals(exitPoint.getPath())) {
                break;
            }
        }
        return null;
    }

    protected String getRelativePath(String name) {
        String path = relativePath;
        if (!path.endsWith("/") && isNotBlank(path)) {
            path += "/";
        }
        path += name;
        path = path.replaceAll("^\\./", "");
        path = path.replaceAll("/\\./", "/");
        path = path.replaceAll("/[^/]+/\\.\\./", "/");
        return path;
    }

    /**
     * Retrieves the first parent node for inheritance traversal (that is, the node above jcr:content)
     * and defines the relativePath of the property wrt. this node.
     */
    protected void findEntryPoint() {
        if (entryPoint == null) {
            StringBuilder path = new StringBuilder();
            Resource parent = resource;
            String name;
            while (parent != null && !ResourceUtil.CONTENT_NODE.equals(name = parent.getName())) {
                if (inheritanceType != Type.contentBased) {
                    if (path.length() > 0) {
                        path.insert(0, '/');
                    }
                    path.insert(0, name);
                }
                parent = parent.getParent();
            }
            if (inheritanceType != Type.nodeRelated && inheritanceType != Type.sameContent && parent != null) {
                // the resource is a child of an element with a content subtree ('jcr:content/...')
                path.insert(0, '/');
                path.insert(0, ResourceUtil.CONTENT_NODE);
                relativePath = path.toString();
                entryPoint = parent.getParent();
            } else {
                // node inheritance or no content subtree ('jcr:content/...') detected...
                relativePath = "";
                entryPoint = resource.getParent();
                if (inheritanceType == Type.sameContent && parent != null) {
                    exitPoint = parent;
                }
            }
        }
    }
}
