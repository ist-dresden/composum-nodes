package com.composum.sling.clientlibs.handle;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Models a client library, containing one or several ClientlibResourceFolders.
 */
public class Clientlib implements ClientlibElement {

    private static final Logger LOG = getLogger(Clientlib.class);

    /** Allowed characters in a category name, for use in regex character classes */
    public static final String CATEGORYNAME_CHARS = "a-zA-Z0-9._-";

    /**
     * Attribute for Client libraries (only the uppermost folder with {@link #RESOURCE_TYPE}) that gives a list of
     * categories this client library belongs to.
     */
    public static final String PROP_CATEGORY = "category";

    /**
     * Attribute for Client libraries (only the uppermost folder with {@link #RESOURCE_TYPE}) that gives an integer that
     * orders the client libraries if there are several belonging to the same {@link #PROP_CATEGORY} . Default value
     * (that is, if not present) is 0.
     */
    public static final String PROP_ORDER = "order";

    /** Resource type of client libraries. */
    public static final String RESOURCE_TYPE = "composum/nodes/commons/clientlib";

    /** A reference that matches this. */
    @Override
    public ClientlibRef getRef() {
        return new ClientlibRef(getType(), resource.getPath(), true, null);
    }

    /** A link that matches this. */
    @Override
    public ClientlibLink makeLink() {
        return new ClientlibLink(getType(), ClientlibLink.Kind.CLIENTLIB, resource.getPath(), null);
    }

    /** Type of the element: {@link #link}, {@link #css}, {@link #js}, {@link #img}. */
    public enum Type {link, css, js, img}

    private final Type type;
    public final ResourceHandle resource;

    public Clientlib(Type type, Resource resource) {
        this.type = type;
        this.resource = ResourceHandle.use(resource);
    }

    @Override
    public Type getType() {
        return type;
    }

    public boolean isValid() {
        return resource.isResourceType(RESOURCE_TYPE);
    }

    public ClientlibResourceFolder getResourceFolder() {
        Resource child = resource.getChild(getType().name());
        return null != child ? new ClientlibResourceFolder(getType(), child) : null;
    }

    public int getOrder() {
        return resource.getProperty(PROP_ORDER, 0);
    }

    @Nonnull
    public List<String> getCategories() {
        return Arrays.asList(resource.getProperty(PROP_CATEGORY, new String[0]));
    }

    @Override
    public void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException {
        visitor.visit(this, mode, parent);
    }

    @Override
    public String toString() {
        return getType() + ":" + resource.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Clientlib)) return false;

        Clientlib that = (Clientlib) o;

        if (getType() != that.getType()) return false;
        return resource != null ? resource.getPath().equals(that.resource.getPath()) : that.resource == null;
    }

    @Override
    public int hashCode() {
        int result = getType() != null ? getType().hashCode() : 0;
        result = 92821 * result + (resource != null ? resource.getPath().hashCode() : 0);
        return result;
    }

    /**
     * Removes invalid chars (according to {@link #CATEGORYNAME_CHARS}) from a category; returns null if there is
     * nothing left / was empty, anyway.
     */
    public static String sanitizeCategory(String category) {
        String res = null;
        if (StringUtils.isNotBlank(category)) {
            res = category.trim().replaceAll("[^" + CATEGORYNAME_CHARS + "]", "");
            if (!res.equals(category)) LOG.error("Invalid characters in category {}", category);
        }
        return res;
    }
}
