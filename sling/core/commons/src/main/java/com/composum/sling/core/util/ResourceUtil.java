package com.composum.sling.core.util;

import com.composum.sling.core.JcrResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A collection of loads of utilities dealing with {@link Resource}s.
 */
public class ResourceUtil extends org.apache.sling.api.resource.ResourceUtil implements CoreConstants {

    private static final Logger LOG = getLogger(ResourceUtil.class);

    /**
     * retrieves all children of one of a type from a type set (node type or resource type)
     */
    public static List<Resource> getChildrenByType(final Resource resource, List<String> typeSet) {
        final ArrayList<Resource> children = new ArrayList<>();
        if (resource != null) {
            for (final Resource child : resource.getChildren()) {
                for (String type : typeSet) {
                    if (isResourceType(child, type)) {
                        children.add(child);
                        break;
                    }
                }
            }
        }
        return children;
    }

    /**
     * retrieves all children of a type (node type or resource type)
     */
    public static List<Resource> getChildrenByType(final Resource resource, String type) {
        final ArrayList<Resource> children = new ArrayList<>();
        if (resource != null) {
            for (final Resource child : resource.getChildren()) {
                if (isResourceType(child, type)) {
                    children.add(child);
                }
            }
        }
        return children;
    }

    /**
     * retrieves all children of a sling:resourceType
     */
    public static List<Resource> getChildrenByResourceType(final Resource resource, String resourceType) {
        final ArrayList<Resource> children = new ArrayList<>();
        if (resource != null) {
            for (final Resource child : resource.getChildren()) {
                if (child.isResourceType(resourceType)) {
                    children.add(child);
                }
            }
        }
        return children;
    }

    public static int getIndexOfSameType(Resource resource) {
        if (resource != null) {
            String name = resource.getName();
            String type = resource.getResourceType();
            Resource parent = resource.getParent();
            if (parent != null) {
                int index = 0;
                for (Resource child : parent.getChildren()) {
                    if (type == null || child.isResourceType(type)) {
                        if (name.equals(child.getName())) {
                            return index;
                        }
                        index++;
                    }
                }
            }
        }
        return -1;
    }

    public static Resource getNextOfSameType(Resource resource, boolean wrapAround) {
        if (resource != null) {
            String name = resource.getName();
            String type = resource.getResourceType();
            Resource parent = resource.getParent();
            if (parent != null) {
                boolean returnNext = false;
                for (Resource child : parent.getChildren()) {
                    if (type == null || child.isResourceType(type)) {
                        if (returnNext) {
                            return child;
                        }
                        if (name.equals(child.getName())) {
                            returnNext = true;
                        }
                    }
                }
                if (returnNext && wrapAround) {
                    for (Resource child : parent.getChildren()) {
                        if (type == null || child.isResourceType(type)) {
                            return child;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getNameExtension(Resource resource) {
        String extension;
        String name = resource.getName();
        if (ResourceUtil.CONTENT_NODE.equals(name)) {
            name = resource.getParent().getName();
        }
        int dot = name.lastIndexOf('.');
        extension = (dot >= 0 ? name.substring(dot + 1).toLowerCase() : "");
        return extension;
    }

    /**
     * retrieves the primary type of the resources node
     */
    public static String getPrimaryType(Resource resource) {
        String result = null;
        if (resource != null) {
            if (resource instanceof JcrResource) {
                // use the resource itself if it implements the JcrResource interface (maybe a version of a resource)
                result = ((JcrResource) resource).getPrimaryType();
            } else {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    try {
                        NodeType type = node.getPrimaryNodeType();
                        if (type != null) {
                            result = type.getName();
                        }
                    } catch (RepositoryException ignore) {
                    }
                }
                if (result == null) {
                    ValueMap values = resource.adaptTo(ValueMap.class);
                    if (values != null) {
                        result = values.get(JcrConstants.JCR_PRIMARYTYPE, (String) null);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Checks whether any of the resource's primary type, super types, sling resource type and supertypes is {resourceType}.
     */
    public static boolean isResourceType(@Nullable Resource resource, String resourceType) {
        return (resource != null && (resource.isResourceType(resourceType)
                || isPrimaryType(resource, resourceType) || isNodeType(resource, resourceType)));
    }

    public static boolean isPrimaryType(Resource resource, String primaryType) {
        return (primaryType.equals(getPrimaryType(resource)));
    }

    public static boolean isNodeType(Resource resource, String primaryType) {
        if (resource != null) {
            try {
                final Node node = resource.adaptTo(Node.class);
                return node != null && node.isNodeType(primaryType);
            } catch (RepositoryException ignore) {
            }
        }
        return false;
    }

    public static Resource getResourceType(Resource resource) {
        return resource != null ? getResourceType(resource.getResourceResolver(),
                resource.getResourceType()) : null;
    }

    public static Resource getResourceType(ResourceResolver resolver, String resourceTypeName) {
        Resource resourceType = null;
        if (StringUtils.isNotBlank(resourceTypeName)) {
            if (resourceTypeName.startsWith("/")) {
                resourceType = resolver.getResource(resourceTypeName);
            } else {
                final String[] searchPath = resolver.getSearchPath();
                for (String path : searchPath) {
                    resourceType = resolver.getResource(path + resourceTypeName);
                    if (resourceType != null) {
                        return resourceType;
                    }
                }
            }
        }
        return resourceType;
    }

    public static boolean isResourceType(Resource resource, Pattern pattern) {
        return resource != null && isResourceType(resource.getResourceResolver(), resource.getResourceType(), pattern);
    }

    public static boolean isResourceType(ResourceResolver resolver, String resourceTypeName, Pattern pattern) {
        if (StringUtils.isNotBlank(resourceTypeName)) {
            if (pattern.matcher(resourceTypeName).find()) {
                return true;
            } else {
                Resource resourceType = getResourceType(resolver, resourceTypeName);
                if (resourceType == null) {
                    return false;
                } else {
                    ValueMap values = resourceType.adaptTo(ValueMap.class);
                    String resourceSuperTypeName = values.get(PROP_RESOURCE_SUPER_TYPE, "");
                    return isResourceType(resolver, resourceSuperTypeName, pattern);
                }
            }
        }
        return false;
    }

    public static <T> T getTypeProperty(Resource resource, String name, T defaultValue) {
        T value = getTypeProperty(resource, name, PropertyUtil.getType(defaultValue));
        return value != null ? value : defaultValue;
    }

    public static <T> T getTypeProperty(Resource resource, String name, Class<T> type) {
        return resource != null ? getTypeProperty(resource.getResourceResolver(),
                resource.getResourceType(), name, type) : null;
    }

    public static <T> T getTypeProperty(ResourceResolver resolver, String resourceTypeName,
                                        String name, Class<T> type) {
        T value = null;
        Resource resourceType = getResourceType(resolver, resourceTypeName);
        if (resourceType != null) {
            ValueMap values = resourceType.adaptTo(ValueMap.class);
            value = values.get(name, type);
            if (value == null) {
                String resourceSuperTypeName = values.get(PROP_RESOURCE_SUPER_TYPE, "");
                value = getTypeProperty(resolver, resourceSuperTypeName, name, type);
            }
        }
        return value;
    }

    public static Resource getOrCreateResource(ResourceResolver resolver, String path)
            throws RepositoryException {
        return getOrCreateResource(resolver, path, null);
    }

    public static Resource getOrCreateResource(ResourceResolver resolver, String path, String primaryTypes)
            throws RepositoryException {
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            path = normalize(path);
            int lastPathSegment = path.lastIndexOf('/');
            String parentPath = "/";
            String name = path;
            if (lastPathSegment >= 0) {
                name = path.substring(lastPathSegment + 1);
                parentPath = path.substring(0, lastPathSegment);
                if (StringUtils.isBlank(parentPath)) {
                    parentPath = "/";
                }
            }
            int lastTypeSegment;
            String parentTypes = primaryTypes;
            String type = primaryTypes;
            if (primaryTypes != null && (lastTypeSegment = primaryTypes.lastIndexOf('/')) >= 0) {
                type = primaryTypes.substring(lastTypeSegment + 1);
                parentTypes = primaryTypes.substring(0, lastTypeSegment);
            }
            Resource parent = getOrCreateResource(resolver, parentPath, parentTypes);
            if (parent != null) {
                Node node = parent.adaptTo(Node.class);
                if (node != null) {
                    if (StringUtils.isNotBlank(type)) {
                        node.addNode(name, type);
                    } else {
                        node.addNode(name);
                    }
                }
                resource = parent.getChild(name);
            }
        }
        return resource;
    }

    public static boolean containsPath(List<Resource> collection, Resource resource) {
        return containsPath(collection, resource.getPath());
    }

    public static boolean containsPath(List<Resource> collection, String path) {
        for (Resource item : collection) {
            if (item.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array of two elements for a nontrivial path (containing /): the parent path and the name.
     * The parent of a toplevel node like "/var" is returned as "/".
     * If it doesn't contain a / , the result has null in the first place.
     */
    @Nonnull
    public static String[] splitPathAndName(@Nonnull String path) {
        String[] result = new String[2];
        int nameSeparator = path.lastIndexOf('/');
        if (nameSeparator >= 0) {
            result[0] = path.substring(0, nameSeparator);
            result[1] = path.substring(nameSeparator + 1);
            if (result[0].isEmpty()) // absolute path at toplevel, like /var
                result[0] = "/";
        } else {
            result[0] = null; // unclear what to return here - null or "" .
            result[1] = path;
        }
        return result;
    }

    /**
     * Retrieves the resources child resource, creates this child if not existing.
     *
     * @param resource     the resource to extend
     * @param relPath      the path to the requested child resource
     * @param primaryTypes the 'path' of primary types for the new nodes (optional, can be 'null')
     * @return the requested child
     */
    public static Resource getOrCreateChild(Resource resource, String relPath, String primaryTypes)
            throws RepositoryException {
        Resource child = null;
        if (resource != null) {
            ResourceResolver resolver = resource.getResourceResolver();
            String path = resource.getPath();
            while (relPath.startsWith("/")) {
                relPath = relPath.substring(1);
            }
            if (StringUtils.isNotBlank(relPath)) {
                path += "/" + relPath;
            }
            child = getOrCreateResource(resolver, path, primaryTypes);
        }
        return child;
    }

    /**
     * Checks the access control policies for enabled changes (node creation and property change).
     */
    public static boolean isWriteEnabled(Resource resource, String relPath) throws RepositoryException {

        ResourceResolver resolver = resource.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        AccessControlManager accessManager = AccessControlUtil.getAccessControlManager(session);

        String resourcePath = resource.getPath();
        Privilege[] addPrivileges = new Privilege[]{
                accessManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES)
        };
        boolean result = accessManager.hasPrivileges(resourcePath, addPrivileges);

        if (StringUtils.isNotBlank(relPath)) {
            if (!resourcePath.endsWith("/")) {
                resourcePath += "/";
            }
            resourcePath += relPath;
        }
        Privilege[] changePrivileges = new Privilege[]{
                accessManager.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES)
        };
        try {
            result = result && accessManager.hasPrivileges(resourcePath, changePrivileges);
        } catch (PathNotFoundException pnfex) {
            // ok, let's create it
        }

        return result;
    }

    /**
     * Returns 'true' is this resource represents a 'file' witch can be displayed (a HTML file).
     */
    public static boolean isRenderableFile(Resource resource) {
        boolean result = false;
        try {
            Node node = resource.adaptTo(Node.class);
            if (node != null) {
                NodeType type = node.getPrimaryNodeType();
                if (ResourceUtil.TYPE_FILE.equals(type.getName())) {
                    String resoureName = resource.getName();
                    result = resoureName.toLowerCase().endsWith(LinkUtil.EXT_HTML);
                }
            }
        } catch (RepositoryException e) {
            // ok, not renderable
        }
        return result;
    }

    /**
     * Returns 'true' is this resource represents a 'file'.
     */
    public static boolean isFile(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                NodeType type = node.getPrimaryNodeType();
                if (type != null) {
                    String typeName = type.getName();
                    switch (typeName) {
                        case TYPE_FILE:
                            return true;
                        case TYPE_RESOURCE:
                        case TYPE_UNSTRUCTURED:
                            try {
                                Property mimeType = node.getProperty(PROP_MIME_TYPE);
                                if (mimeType != null && StringUtils.isNotBlank(mimeType.getString())) {
                                    node.getProperty(ResourceUtil.PROP_DATA);
                                    // PathNotFountException if not present
                                    return true;
                                }
                            } catch (PathNotFoundException pnfex) {
                                // ok, was a check only
                            }
                            break;
                    }
                }
            } catch (RepositoryException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return false;
    }

    public static Resource getDataResource(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                try {
                    node.getProperty(ResourceUtil.PROP_DATA);
                    return resource;
                } catch (PathNotFoundException pnfex) {
                    Node contentNode = node.getNode(CONTENT_NODE);
                    contentNode.getProperty(PROP_DATA);
                    return resource.getChild(CONTENT_NODE);
                }
            } catch (RepositoryException rex) {
                // ok, property doesn't exist
            }
        }
        return null;
    }

    public static Binary getBinaryData(Resource resource) {
        return PropertyUtil.getBinaryData(resource.adaptTo(Node.class));
    }

    /** Finds a mix:referenceable by its jcr:uuid. */
    @Nullable
    public static Resource getByUuid(@Nonnull ResourceResolver resolver, @Nullable String uuid) throws RepositoryException {
        if (StringUtils.isBlank(uuid)) return null;
        Resource result = null;
        Session session = resolver.adaptTo(Session.class);
        if (session != null) {
            try {
                Node node = session.getNodeByIdentifier(uuid);
                result = resolver.getResource(node.getPath());
            } catch (ItemNotFoundException e) { // OK.
                LOG.debug("Node not found by uuid {}", uuid);
            }
        } else {
            throw new RepositoryException("Cannot get JCR Session from resolver " + resolver);
        }
        return result;
    }

    /** Finds the referenced node from a property of type REFERENCE, WEAKREFERENCE or PATH or convertable to that. */
    @Nullable
    public static Resource getReferredResource(@Nullable Resource propertyResource) throws RepositoryException {
        if (propertyResource == null) return null;
        Property property = propertyResource.adaptTo(Property.class);
        Node node = property != null ? property.getNode() : null;
        String path = node != null ? node.getPath() : null;
        return path != null ? propertyResource.getResourceResolver().getResource(path) : null;
    }

}
