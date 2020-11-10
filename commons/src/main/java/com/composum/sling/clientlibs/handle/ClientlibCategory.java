package com.composum.sling.clientlibs.handle;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Models a category of client libraries.
 */
public class ClientlibCategory implements ClientlibElement {

    private static final Logger LOG = getLogger(ClientlibCategory.class);

    private final Clientlib.Type type;
    public final String category;
    public final boolean optional;
    public final List<Clientlib> clientlibs = new ArrayList<>();

    public ClientlibCategory(ClientlibRef ref, List<Resource> clientlibResources) {
        this.category = ref.category;
        this.optional = ref.optional;
        this.type = ref.type;
        for (Resource resource : clientlibResources) {
            Clientlib clientlib = new Clientlib(getType(), resource);
            if (clientlib.isValid())
                clientlibs.add(clientlib);
            else LOG.warn("Invalid clientlib for category {}: {}", category, resource);
        }
    }

    @Override
    public void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException {
        visitor.visit(this, mode, parent);
    }

    public ClientlibRef getRef() {
        return new ClientlibRef(getType(), ClientlibRef.PREFIX_CATEGORY + category, optional, null);
    }

    @Override
    public String toString() {
        return ClientlibRef.PREFIX_CATEGORY + category + clientlibs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientlibCategory)) return false;

        ClientlibCategory that = (ClientlibCategory) o;

        if (getType() != that.getType()) return false;
        return Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        int result = getType() != null ? getType().hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        return result;
    }

    public ClientlibLink makeLink() {
        return ClientlibLink.forCategory(getType(), category);
    }

    @Override
    public Clientlib.Type getType() {
        return type;
    }

}
