package com.composum.sling.clientlibs.handle;

import com.composum.sling.core.util.ResourceUtil;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Map;

/**
 * Models a file that is referenced by a client library.
 */
public class ClientlibFile implements ClientlibElement {

    public final FileHandle handle;
    private final Clientlib.Type type;
    private final ClientlibRef ref;
    /** Additional properties, e.g. {@link ClientlibLink#PROP_REL}. */
    public final Map<String, String> properties;

    public ClientlibFile(ClientlibRef ref, Clientlib.Type type, Resource resource, Map<String, String> properties) {
        if (null == resource) throw new IllegalArgumentException("path is null");
        this.type = type;
        this.handle = new FileHandle(resource);
        this.properties = properties;
        this.ref = null != ref ? ref : new ClientlibRef(type, handle.getPath(), false, properties);
    }

    @Override
    public void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException {
        visitor.visit(this, mode, parent);
    }

    public static boolean isFile(Resource resource) {
        return resource.isResourceType(ResourceUtil.TYPE_FILE) ||
                resource.isResourceType(ResourceUtil.TYPE_LINKED_FILE);
    }

    public ClientlibLink makeLink() {
        return new ClientlibLink(type, ClientlibLink.Kind.FILE, handle.getPath(), properties);
    }

    public ClientlibRef getRef() {
        return ref;
    }

    @Override
    public String toString() {
        return type + ":" + handle.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientlibFile)) return false;

        ClientlibFile that = (ClientlibFile) o;

        if (handle.getPath() != null ? !handle.getPath().equals(that.handle.getPath()) : that.handle.getPath() != null)
            return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = handle != null && handle.getPath() != null ? handle.getPath().hashCode() : 0;
        result = 92821 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public Clientlib.Type getType() {
        return type;
    }
}
