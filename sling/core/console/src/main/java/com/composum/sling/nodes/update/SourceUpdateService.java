package com.composum.sling.nodes.update;

import org.apache.sling.api.resource.Resource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public interface SourceUpdateService {

    /**
     * Reads an XML from the input stream and updates the resource so that it is identical to the stream, ignoring / updating metadata.
     */
    void updateFromXml(Resource resource, InputStream inputStream) throws RepositoryException, IOException, ParserConfigurationException, SAXException;
}
