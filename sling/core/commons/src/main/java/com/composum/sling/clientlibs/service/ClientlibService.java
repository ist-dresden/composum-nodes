package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.processor.RendererContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

public interface ClientlibService {

    String ENCODING_GZIP = "gzip";

    // Clientlib composition and delivery

    void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                              Writer writer, RendererContext context)
            throws IOException;

    void resetContent(Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException;

    Map<String, Object> prepareContent(SlingHttpServletRequest request,
                                       Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException;

    void deliverContent(Clientlib clientlib, OutputStream outputStream, String encoding)
            throws IOException, RepositoryException;

    boolean mapClientlibURLs();

    boolean useMinifiedFiles();
}
