package com.composum.sling.core.pckgmgr.manager.tree;

import com.composum.sling.core.pckgmgr.manager.util.PackageUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class JcrPackageItem implements TreeItem {

    private static final Logger LOG = LoggerFactory.getLogger(JcrPackageItem.class);

    private final JcrPackage jcrPackage;
    private final JcrPackageDefinition definition;

    public JcrPackageItem(JcrPackage jcrPackage) throws RepositoryException {
        this.jcrPackage = jcrPackage;
        definition = jcrPackage.getDefinition();
    }

    @Override
    public String getName() {
        return definition.get(JcrPackageDefinition.PN_NAME);
    }

    @Override
    public String getPath() {
        try {
            String name = getFilename();
            String groupPath = PackageUtil.getGroupPath(jcrPackage);
            String path = groupPath + name;
            return path;
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return "";
    }

    public JcrPackageDefinition getDefinition() {
        return definition;
    }

    @Override
    public void toJson(JsonWriter writer) throws RepositoryException, IOException {
        String name = getFilename();
        String path = getPath();
        Map<String, Object> treeState = new LinkedHashMap<>();
        treeState.put("loaded", Boolean.TRUE);
        Map<String, Object> additionalAttributes = new LinkedHashMap<>();
        additionalAttributes.put("id", path);
        additionalAttributes.put("path", path);
        additionalAttributes.put("name", name);
        additionalAttributes.put("text", name);
        additionalAttributes.put("type", "package");
        additionalAttributes.put("state", treeState);
        additionalAttributes.put("file", getFilename());
        PackageUtil.toJson(writer, jcrPackage, additionalAttributes);
    }

    public String getFilename() {
        return PackageUtil.getFilename(jcrPackage);
    }

    public Calendar getLastModified() {
        Calendar lastModified = PackageUtil.getLastModified(jcrPackage);
        if (lastModified != null) {
            return lastModified;
        }
        return PackageUtil.getCreated(jcrPackage);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JcrPackageItem &&
                getName().equals(((JcrPackageItem) other).getName()) &&
                definition.get(JcrPackageDefinition.PN_VERSION).equals(((JcrPackageItem) other).definition.get(JcrPackageDefinition.PN_VERSION));
    }

    @Override
    public int hashCode() {
        return 31 * getName().hashCode() + definition.get(JcrPackageDefinition.PN_VERSION).hashCode();
    }

}
