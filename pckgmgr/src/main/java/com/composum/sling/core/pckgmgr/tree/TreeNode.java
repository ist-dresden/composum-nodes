package com.composum.sling.core.pckgmgr.tree;

import com.composum.sling.core.pckgmgr.util.PackageUtil;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;

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
    public boolean addPackage(JcrPackage jcrPackage) throws RepositoryException {
        String groupUri = path.endsWith("/") ? path : path + "/";
        String groupPath = PackageUtil.getGroupPath(jcrPackage);
        if (groupPath.startsWith(groupUri)) {
            TreeItem item;
            if (groupPath.equals(groupUri)) {
                // this node is the packages parent - use the package as node child
                item = new JcrPackageItem(jcrPackage);
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
        Collections.sort(this, new Comparator<TreeItem>() {

            @Override
            public int compare(TreeItem o1, TreeItem o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
    }

    public void toJson(JsonWriter writer) throws IOException, RepositoryException {
        if (isLeaf()) {
            get(0).toJson(writer);
        } else {
            int lastPathSegment = path.lastIndexOf("/");
            String name = path.substring(lastPathSegment + 1);
            if (StringUtils.isBlank(name)) {
                name = "packages ";
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
}
