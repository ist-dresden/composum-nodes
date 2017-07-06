package com.composum.sling.clientlibs.handle;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * Interface common to all elements contained in client libraries.
 */
public interface ClientlibElement {

    /**
     * Dispatcher for the appropriate method of visitor: just calls <code>visitor.visit(this, mode,
     * parent);</code>.
     *
     * @param visitor the visitor we want to dispatch to
     * @param mode    processing mode
     * @param parent  if applicable, the parent of the visited resource
     */
    void accept(ClientlibVisitor visitor, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws IOException, RepositoryException;

    /** The resource-type for which it was created. */
    Clientlib.Type getType();

    /**
     * Creates a {@link ClientlibLink} to this element.
     *
     * @throws UnsupportedOperationException if that isn't supported by this kind of element (that is, on {@link
     *                                       ClientlibResourceFolder}).
     */
    ClientlibLink makeLink();

    /**
     * Returns a {@link ClientlibRef} {@link ClientlibRef#isSatisfiedby(ClientlibLink)} {@link #makeLink()}.
     * This is either a fresh one, or, in case of files, the one with which the file was located.
     *
     * @throws UnsupportedOperationException if that isn't supported by this kind of element (that is, on {@link
     *                                       ClientlibResourceFolder}).
     */
    ClientlibRef getRef();

}
