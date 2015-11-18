package com.composum.sling.core.util;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Helper methods for Package handling (VLT Package Manager)
 */
public class PackageUtil {

    public enum TreeType {group, jcrpckg}

    ;

    public static JcrPackageManager createPackageManager(SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        JcrPackageManager manager = PackagingService.getPackageManager(session);
        return manager;
    }

    public static String getPath(SlingHttpServletRequest request) {
        RequestPathInfo reqPathInfo = request.getRequestPathInfo();
        String path = reqPathInfo.getSuffix();
        if (StringUtils.isBlank(path)) {
            path = "/";
        } else {
            while (path.endsWith("/") && !"/".equals(path)) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    public static Resource getResource(SlingHttpServletRequest request, String path) throws RepositoryException {
        Resource resource = null;
        JcrPackageManager manager = PackageUtil.createPackageManager(request);
        Node node = null;
        node = manager.getPackageRoot(true);
        if (node != null) {
            ResourceResolver resolver = request.getResourceResolver();
            String resourcePath = node.getPath() + path;
            resource = resolver.getResource(resourcePath);
        }
        return resource;
    }

    public static JcrPackage getJcrPackage(JcrPackageManager manager, Resource resource) throws RepositoryException {
        JcrPackage jcrPackage = null;
        Node node;
        if (resource != null && (node = resource.adaptTo(Node.class)) != null) {
            jcrPackage = manager.open(node);
        }
        return jcrPackage;
    }

    public static TreeType getTreeType(SlingHttpServletRequest request, String path) {
        TreeType type = null;
        try {
            Resource resource = getResource(request, path);
            JcrPackageManager manager = createPackageManager(request);
            JcrPackage jcrPackage = getJcrPackage(manager, resource);
            type = jcrPackage != null ? TreeType.jcrpckg : TreeType.group;
        } catch (RepositoryException rex) {
            // ok, it's not a package related resource
        }
        return type;
    }
}
