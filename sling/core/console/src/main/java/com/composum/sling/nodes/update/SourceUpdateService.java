package com.composum.sling.nodes.update;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

public interface SourceUpdateService {

    /**
     * Reads an XML from the input stream and updates the resource so that it is identical to the stream, ignoring / updating metadata.
     */
    void updateFromXml(@Nonnull Resource resource, @Nonnull InputStream inputStream) throws RepositoryException, IOException, ParserConfigurationException, SAXException, TransformerException;

    /**
     * Reads a ZIP from the input stream and updates the resources at the path of the entries of the zip file so that
     * they are identical to the stream, ignoring / updating metadata.
     */
    void updateFromZip(ResourceResolver resolver, InputStream zipInputStream) throws IOException, RepositoryException, TransformerException;

}
