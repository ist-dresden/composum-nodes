package com.composum.sling.core.mapping;

import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * the service interface for package importing
 */
public interface PackageImporter {

    /**
     * The function to import a complete ZIPed package described by its package definition into the repository.
     *
     * @param input the source stream with the ZIP content to import
     * @param resolver the resolver to use for resource retrieval
     * @throws RepositoryException
     * @throws IOException
     */
    void importPackage(InputStream input, OutputStream log, ResourceResolver resolver)
            throws RepositoryException, IOException;
}
