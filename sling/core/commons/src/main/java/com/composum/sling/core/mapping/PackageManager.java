package com.composum.sling.core.mapping;

import com.composum.sling.core.filter.ResourceFilter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 *
 */
public interface PackageManager extends PackageImporter, PackageExporter {

    List<Resource> listPackages(String suffix, ResourceResolver resolver);

    ResourceFilter getListFilter();

    Resource retrievePackage (String suffix, ResourceResolver resolver);

    ResourceFilter getPackageFilter();

    Resource getResource(String suffix, ResourceResolver resolver);

    void setupPackage(Package pkg, ResourceResolver resolver)
            throws RepositoryException;

    void setupPackage(Package pkg, String groupPath, ResourceResolver resolver)
            throws RepositoryException;

    void deletePackage(Resource resource) throws RepositoryException;

    String getPackagesRoot();
}
