package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import org.apache.sling.api.resource.LoginException;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface ClientlibService {

    String ENCODING_GZIP = "gzip";

    void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties, Writer writer)
            throws IOException;

    void resetContent(Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException;

    Map<String, Object> prepareContent(Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException;

    void deliverContent(Clientlib clientlib, Writer writer, String encoding)
            throws IOException, RepositoryException;
}
