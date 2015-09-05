package com.composum.sling.core.mapping;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * the service interface for package exporting
 */
public interface PackageExporter {

    /**
     * The function to export a package described by a resource or
     * a snapshot of one resource using default package settings.
     *
     * @param output the target stream to write the ZIP content
     * @param resource the package resource or the non package source
     * @throws RepositoryException
     * @throws IOException
     */
    void exportPackage(OutputStream output, Resource resource)
            throws RepositoryException, IOException;

    /**
     * The function to export a complete package described by a package object into a ZIP stream.
     *
     * @param output the target stream to write the ZIP content
     * @param pkg the package definition object
     * @param resolver the resolver to use for resource retrieval
     * @throws RepositoryException
     * @throws IOException
     */
    void exportPackage(OutputStream output, Package pkg, ResourceResolver resolver)
            throws RepositoryException, IOException;
}
