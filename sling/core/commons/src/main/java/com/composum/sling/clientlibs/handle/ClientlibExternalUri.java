package com.composum.sling.clientlibs.handle;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Map;

/**
 * Models a reference to external URLs - as links, js or css.
 */
public class ClientlibExternalUri implements ClientlibElement {

    protected final ClientlibLink link;

    /**
     * Creates the element.
     *
     * @param type       the type of the link
     * @param uri        the URL (HTTP/HTTPS/protocol omitted)
     * @param properties optionally, additional properties
     */
    public ClientlibExternalUri(Clientlib.Type type, String uri, Map<String, String> properties) {
        link = new ClientlibLink(type, ClientlibLink.Kind.EXTERNALURI, uri, properties);
    }

    /**
     * Calls the visitor with mode {@link com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode#DEPENDS},
     * since external references are never embedded.
     */
    @Override
    public void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) {
        visitor.visit(this, ClientlibVisitor.VisitorMode.DEPENDS, parent);
    }

    @Override
    public Clientlib.Type getType() {
        return link.type;
    }

    @Override
    public ClientlibLink makeLink() {
        return link;
    }

    @Override
    public ClientlibRef getRef() {
        return new ClientlibRef(link.type, link.path, false, link.properties);
    }


    @Override
    public String toString() {
        return link.toString();
    }
}
