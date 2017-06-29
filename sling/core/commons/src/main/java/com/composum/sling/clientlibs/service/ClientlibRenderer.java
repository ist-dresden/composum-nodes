package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibCategory;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.processor.RendererContext;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;

public interface ClientlibRenderer {

    /**
     * Renders the given clientlib / category to writer.
     *  @param clientlib  a {@link Clientlib} or {@link
     *                   ClientlibCategory}
     *
     */
    void renderClientlibLinks(ClientlibElement clientlib,
                              Writer writer, RendererContext context)
            throws IOException, RepositoryException;

}
