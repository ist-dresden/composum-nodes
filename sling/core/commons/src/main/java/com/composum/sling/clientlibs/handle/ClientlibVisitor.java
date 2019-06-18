package com.composum.sling.clientlibs.handle;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * Business interface for visitors processing client libraries: used for easy dispatching with {@link
 * ClientlibElement#accept(ClientlibVisitor, VisitorMode, ClientlibResourceFolder)}.
 */
public interface ClientlibVisitor {

    enum VisitorMode {
        /** Visited object is embedded into parent */
        EMBEDDED,
        /** Visited object is dependency of the parent. */
        DEPENDS

    }

    /**
     * The ClientlibElement (clientlib or category) whose embedded elements this visitor processes; clientlibs or
     * categories as dependency will be processed by their own visitor.
     */
    ClientlibElement getOwner();

    /**
     * Triggers the processing of the {@link #getOwner()} by calling {@link ClientlibElement#accept(ClientlibVisitor,
     * VisitorMode, ClientlibResourceFolder)}.
     *
     * @return this
     */
    ClientlibVisitor execute() throws IOException, RepositoryException;

    void visit(ClientlibCategory clientlibCategory, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException;

    void visit(Clientlib clientlib, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException;

    void visit(ClientlibResourceFolder folder, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException;

    void visit(ClientlibFile file, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws RepositoryException, IOException;

    void visit(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent);

}
