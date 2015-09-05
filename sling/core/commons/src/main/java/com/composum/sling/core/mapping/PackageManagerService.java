package com.composum.sling.core.mapping;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 *
 */
@Component(
        label = "Composum Package Manager Service",
        description = "Provides the management of package definitions in the repository.",
        immediate = true,
        metatype = true
)
@Service
public class PackageManagerService implements PackageManager {

    @Property(
            label = "Packages Root",
            description = "the root path to store package definitions in the repository",
            value = "/var/packages"
    )
    public static final String PACKAGES_ROOT = "composum.packages.root";

    public static final ResourceFilter PACKAGE_FILTER = new ResourceFilter.ResourceTypeFilter(
            new StringFilter.WhiteList("^" + Package.RESOURCE_TYPE_PACKAGE + "$")
    );

    public static final ResourceFilter PACKAGE_TREE_FILTER = new ResourceFilter.FilterSet(
            ResourceFilter.FilterSet.Rule.or,
            ResourceFilter.FOLDER,
            PACKAGE_FILTER
    );

    @Reference
    protected PackageExporter packageExporter;

    @Reference
    protected PackageImporter packageImporter;

    //
    // service interface implementation
    //

    @Override
    public List<Resource> listPackages(String suffix, ResourceResolver resolver) {
        Resource resource = getResource(suffix, resolver);
        List<Resource> list = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            if (PACKAGE_TREE_FILTER.accept(child)) {
                list.add(child);
            }
        }
        return list;
    }

    @Override
    public Resource retrievePackage(String suffix, ResourceResolver resolver) {
        Resource resource = getResource(suffix, resolver);
        return PACKAGE_FILTER.accept(resource) ? resource : null;
    }

    @Override
    public Resource getResource(String suffix, ResourceResolver resolver) {
        Resource resource = null;
        if (suffix != null && suffix.startsWith(getPackagesRoot())) {
            resource = resolver.getResource(suffix);
        }
        if (resource == null) {
            String path = getPackagesRoot();
            if (suffix != null) {
                path += (suffix.startsWith("/") ? "" : "/") + suffix;
            }
            resource = resolver.getResource(path);
        }
        return resource;
    }

    @Override
    public void setupPackage(Package pkg, ResourceResolver resolver)
            throws RepositoryException {
        setupPackage(pkg, null, resolver);
    }

    @Override
    public void setupPackage(Package pkg, String groupPath, ResourceResolver resolver)
            throws RepositoryException {
        Resource packageRoot = ResourceUtil.getOrCreateResource(resolver, getPackagesRoot());
        Package.toResource(packageRoot, groupPath, pkg);
    }

    @Override
    public void deletePackage(Resource resource)
            throws RepositoryException {

        Node node = null;
        if (resource == null ||
                (node = resource.adaptTo(Node.class)) == null ||
                !Package.RESOURCE_TYPE_PACKAGE.equals(resource.getResourceType())) {
             throw new IllegalArgumentException("resource '" + resource.getPath() + "' is not a package");
        }

        node.remove();
    }

    @Override
    public void importPackage(InputStream packageStream, OutputStream logStream, ResourceResolver resolver)
            throws RepositoryException, IOException {
        packageImporter.importPackage(packageStream, logStream, resolver);
    }

    @Override
    public void exportPackage(OutputStream exportStream, Resource packageResource)
            throws RepositoryException, IOException {
        packageExporter.exportPackage(exportStream, packageResource);
    }

    @Override
    public void exportPackage(OutputStream exportStream, Package pkg, ResourceResolver resolver)
            throws RepositoryException, IOException {
        packageExporter.exportPackage(exportStream, pkg, resolver);
    }

    //
    // service configuration
    //

    @Override
    public String getPackagesRoot() {
        return (String) properties.get(PACKAGES_ROOT);
    }

    @Override
    public ResourceFilter getListFilter() {
        return PACKAGE_TREE_FILTER;
    }

    @Override
    public ResourceFilter getPackageFilter() {
        return PACKAGE_FILTER;
    }

    protected Dictionary properties;

    protected void activate(ComponentContext context) {
        this.properties = context.getProperties();
    }

    protected void deactivate(ComponentContext context) {
        this.properties = null;
    }
}
