package com.composum.sling.clientlibs.handle;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Models a resource folder for a {@link Clientlib} - this can be the folder js/css directly below it, or a lower level
 * folder.
 */
public class ClientlibResourceFolder implements ClientlibElement {
    /**
     * Boolean value, default false; if true at the resource folder, the embedded files are not compacted into one file.
     * Always true when debugging mode is activated.
     */
    public static final String PROP_EXPANDED = "expanded";

    /**
     * Boolean value, default false; if false for a missing dependency / embedding a warning is logged.
     */
    public static final String PROP_OPTIONAL = "optional";

    /**
     * Array of strings with path patterns (ClientlibRef) for resources / client libraries that are required.
     */
    public static final String PROP_DEPENDS = "depends";

    /**
     * Array of strings with path patterns (ClientlibRef) for resources / client libraries that are components of this
     * client library.
     */
    public static final String PROP_EMBED = "embed";

    public final ResourceHandle resource;
    protected final ClientlibResourceFolder parent;
    protected Map<String, String> additionalProperties;
    protected final Clientlib.Type type;
    protected Boolean expanded;
    protected Boolean optional;

    public ClientlibResourceFolder(Clientlib.Type type, Resource resource) {
        this(type, resource, null);
    }

    protected ClientlibResourceFolder(Clientlib.Type type, Resource resource, ClientlibResourceFolder parent) {
        this.type = type;
        this.resource = ResourceHandle.use(resource);
        this.parent = parent;
    }

    public boolean isValid() {
        return resource.isValid();
    }

    public boolean getExpanded() {
        if (null == expanded) expanded = resource.getProperty(PROP_EXPANDED, Boolean.class);
        if (null == expanded && null != parent) expanded = parent.getExpanded();
        if (null == expanded) expanded = false;
        return expanded;
    }

    public boolean getOptional() {
        if (null == optional) optional = resource.getProperty(PROP_OPTIONAL, Boolean.class);
        if (null == optional && null != parent) optional = parent.getOptional();
        if (null == optional) optional = false;
        return optional;
    }

    @Nonnull
    public List<ClientlibRef> getDependencies() {
        return getClientlib2Refs(PROP_DEPENDS);
    }

    @Nonnull
    public List<ClientlibRef> getEmbedded() {
        return getClientlib2Refs(PROP_EMBED);
    }

    @Override
    public Clientlib.Type getType() {
        return type;
    }

    @Nonnull
    protected List<ClientlibRef> getClientlib2Refs(String property) {
        List<ClientlibRef> res = new ArrayList<>();
        for (String rule : resource.getProperty(property, new String[0])) {
            res.add(new ClientlibRef(type, rule.trim(), getOptional(), getAdditionalProperties()));
        }
        return res;
    }

    /**
     * Returns all children - either {@link ClientlibResourceFolder} as well, or {@link ClientlibFile} .
     */
    public List<ClientlibElement> getChildren() {
        List<ClientlibElement> children = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            if (isFile(child)) children.add(new ClientlibFile(null, type, child, getAdditionalProperties()));
            else children.add(new ClientlibResourceFolder(type, child, this));
        }
        return children;
    }

    public Map<String, String> getAdditionalProperties() {
        if (null == additionalProperties) {
            additionalProperties = new HashMap<>();
            ClientlibResourceFolder folder = this;
            while (null != folder) {
                for (String key : ClientlibLink.LINK_PROPERTIES) {
                    if (!additionalProperties.containsKey(key)) {
                        String property = folder.resource.getProperty(key);
                        if (null != property) additionalProperties.put(key, property);
                    }
                }
                folder = folder.parent;
            }
        }
        return additionalProperties;
    }

    /**
     * Distinguishes file resources from resource folders: child is considered a resource folder if this is false.
     */
    public static boolean isFile(Resource resource) {
        return resource.isResourceType(ResourceUtil.TYPE_FILE) ||
                resource.isResourceType(ResourceUtil.TYPE_LINKED_FILE);
    }

    @Override
    public void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException {
        visitor.visit(this, mode, parent);
    }

    @Override
    public String toString() {
        return type + ":" + resource.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientlibResourceFolder)) return false;

        ClientlibResourceFolder that = (ClientlibResourceFolder) o;

        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.getPath().hashCode() : 0;
        result = 92821 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    /**
     * Not supported.
     */
    @Override
    public ClientlibLink makeLink() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Not supported.
     */
    @Override
    public ClientlibRef getRef() {
        throw new UnsupportedOperationException("Not implemented.");
    }

}
