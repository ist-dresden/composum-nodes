package com.composum.sling.nodes.update;

import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

public interface SourceUpdateService {

    /**
     * Reads a ZIP from the input stream and updates the resources at the path of the entries of the zip file so that
     * they are identical to the stream, ignoring / updating metadata.
     *
     * @param resolver       the resolver we write to
     * @param zipInputStream contains a zip with the data to import. It needs to contain the actual content to import below jcr_root.
     *                       E.g. if nodePath is /content/whatever, the zip contains the relevant content below jcr_root/content/whatever/somepath
     * @param nodePath       the path of the node we want to update
     * @throws IllegalArgumentException if something about the parameters is too fishy to go on
     */
    void updateFromZip(@Nonnull ResourceResolver resolver, @Nonnull InputStream zipInputStream, @Nonnull String nodePath)
            throws IOException, RepositoryException, TransformerException, IllegalArgumentException;

}
