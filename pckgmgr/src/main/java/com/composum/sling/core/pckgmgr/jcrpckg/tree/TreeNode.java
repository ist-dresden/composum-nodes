package com.composum.sling.core.pckgmgr.jcrpckg.tree;

import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * the tree node implementation for the requested path (folder or package)
 */
public class TreeNode extends ArrayList<TreeItem> {

    private final String path;
    private boolean isLeaf;

    public TreeNode(String path) {
        this.path = path;
    }

    /**
     * adds a package or the appropriate folder to the nodes children if it is a child of this node
     *
     * @param jcrPackage the current package in the iteration
     * @return true, if this package is the nodes target and a leaf - iteration can be stopped
     * @throws RepositoryException
     */
    /* Cases:
     * 1. / , /group/ , /group/parts as superpaths for packages
     * 2. /the/group/package for an overview of all package versions
     * 3. /the/group/package-version.zip the actual package
     * Each of the cases has the next case as children. */
    public boolean addPackage(JcrPackage jcrPackage) throws RepositoryException {
        String groupUri = path.endsWith("/") ? path : path + "/";
        String groupPath = PackageUtil.getGroupPath(jcrPackage);
        String packageName = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_NAME);
        String packagePath = groupPath + packageName + "/"; // case 3
        if (packagePath.startsWith(groupUri)) {
            TreeItem item;
            if (packagePath.equals(groupUri)) {
                // This node is the packages parent - use the package as node child. Case 3.
                item = new JcrPackageItem(jcrPackage);
            } else if (groupPath.equals(groupUri)) {
                item = new FolderItem(StringUtils.removeEnd(packagePath, "/"), packageName);
            } else {
                // this node is a group parent - insert a folder for the subgroup
                String name = groupPath.substring(path.length());
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                int nextDelimiter = name.indexOf("/");
                if (nextDelimiter > 0) {
                    name = name.substring(0, nextDelimiter);
                }
                item = new FolderItem(groupUri + name, name);
            }
            if (!contains(item)) {
                add(item);
            }
            return false;
        } else {
            JcrPackageItem item = new JcrPackageItem(jcrPackage);
            if (path.equals(groupPath + item.getFilename())) {
                // this node (teh path) represents the package itself and is a leaf
                isLeaf = true;
                add(item);
                // we can stop the iteration
                return true;
            }
            return false;
        }
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void sort() {
        Collections.sort(this, new TreeItemComparator());
    }

    public void toJson(JsonWriter writer) throws IOException, RepositoryException {
        if (isLeaf()) {
            get(0).toJson(writer);
        } else {
            int lastPathSegment = path.lastIndexOf("/");
            String name = path.substring(lastPathSegment + 1);
            if (StringUtils.isBlank(name)) {
                name = "Packages ";
            }
            FolderItem myself = new FolderItem(path, name);

            writer.beginObject();
            JsonUtil.jsonMapEntries(writer, myself);
            writer.name("children");
            writer.beginArray();
            for (TreeItem item : this) {
                item.toJson(writer);
            }
            writer.endArray();
            writer.endObject();
        }
    }

    public static class TreeItemComparator implements Comparator<TreeItem> {

        @Override
        public int compare(TreeItem o1, TreeItem o2) {
            if ( o1 instanceof JcrPackageItem && o2 instanceof JcrPackageItem) {
                return ((JcrPackageItem) o1).compareTo((JcrPackageItem) o2);
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }

    }
}
