package com.composum.sling.core.util;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

/** Helper methods for Package handling (VLT Package Manager) */
public class PackageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PackageUtil.class);

    public enum TreeType {
        group, jcrpckg
    }

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
        if (ResourceHandle.isValid(resource) && (node = resource.adaptTo(Node.class)) != null) {
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

    public static String getFilename(JcrPackage pckg) {
        StringBuilder filename = new StringBuilder();
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                filename.append(pckgDef.get(JcrPackageDefinition.PN_NAME));
                String version = pckgDef.get(JcrPackageDefinition.PN_VERSION);
                if (version != null) {
                    filename.append('-').append(version);
                }
                filename.append(".zip");
            } else {
                Node node = pckg.getNode();
                filename.append(node.getName());
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return filename.toString();
    }

    public static String getDownloadUrl(JcrPackage pckg) {
        StringBuilder downloadUrl = new StringBuilder();
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                downloadUrl.append("/bin/core/package.download.zip/")
                        .append(pckgDef.get(JcrPackageDefinition.PN_GROUP))
                        .append("/")
                        .append(getFilename(pckg));
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return downloadUrl.toString();
    }

    public static <T> T getDefAttr(JcrPackageDefinition pckgDef, String key, T defaultValue) {
        T value = null;
        if (pckgDef != null) {
            Class type = defaultValue != null ? defaultValue.getClass() : String.class;
            if (Calendar.class.isAssignableFrom(type)) {
                value = (T) pckgDef.getCalendar(key);
            } else {
                value = (T) pckgDef.get(key);
            }
        }
        return value != null ? value : defaultValue;
    }

    public static Calendar getLastModified(JcrPackage pckg) {
        Calendar result = null;
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                result = pckgDef.getLastModified();
            }
            if (result == null) {
                Node node = pckg.getNode();
                if (node != null && (node = node.getNode(JcrConstants.JCR_CONTENT)) != null
                        && node.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
                    Property property = node.getProperty(JcrConstants.JCR_LASTMODIFIED);
                    result = property.getDate();
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    public static String getLastModifiedBy(JcrPackage pckg) {
        String result = null;
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                result = pckgDef.getLastModifiedBy();
            }
            if (result == null) {
                Node node = pckg.getNode();
                if (node != null && (node = node.getNode(JcrConstants.JCR_CONTENT)) != null
                        && node.hasProperty(JcrConstants.JCR_LAST_MODIFIED_BY)) {
                    Property property = node.getProperty(JcrConstants.JCR_LAST_MODIFIED_BY);
                    result = property.getString();
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    public static Calendar getCreated(JcrPackage pckg) {
        Calendar result = null;
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                result = pckgDef.getCreated();
            }
            if (result == null) {
                Node node = pckg.getNode();
                if (node != null && node.hasProperty(JcrConstants.JCR_CREATED)) {
                    Property property = node.getProperty(JcrConstants.JCR_CREATED);
                    result = property.getDate();
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    public static String getCreatedBy(JcrPackage pckg) {
        String result = null;
        try {
            JcrPackageDefinition pckgDef = pckg.getDefinition();
            if (pckgDef != null) {
                result = pckgDef.getCreatedBy();
            }
            if (result == null) {
                Node node = pckg.getNode();
                if (node != null && node.hasProperty(JcrConstants.JCR_CREATED_BY)) {
                    Property property = node.getProperty(JcrConstants.JCR_CREATED_BY);
                    result = property.getString();
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }
}
