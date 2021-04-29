package com.composum.sling.core.usermanagement.model;

import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeNode {

    public static final String TYPE_ROOT = "root";
    public static final String TYPE_FOLDER = "folder";

    public static final Map<String, String> PATH_TO_TYPE = new HashMap<String, String>() {{
        put("/home/groups", AuthorizableModel.TYPE_GROUP);
        put("/home/services", AuthorizableModel.TYPE_SERVICE);
        put("/home/users", AuthorizableModel.TYPE_USER);
    }};

    public static final Comparator<TreeNode> NODE_COMPARATOR = new Comparator<TreeNode>() {

        @Override
        public int compare(TreeNode node1, TreeNode node2) {
            return getKey(node1).compareTo(getKey(node2));
        }

        protected String getKey(TreeNode node) {
            return node.getType().charAt(0) + ":" + node.getName();
        }
    };

    protected final String type;
    protected final String name;
    protected final String path;
    protected final AuthorizableModel model;
    protected final Map<String, TreeNode> children = new HashMap<>();

    private transient List<TreeNode> nodes;

    public TreeNode(@NotNull final String type, @NotNull final String path) {
        this.model = null;
        this.type = type;
        this.path = path;
        this.name = getName(path);
    }

    public TreeNode(AuthorizableModel model) {
        this.model = model;
        type = model.getType();
        this.name = model.getId();
        this.path = model.path;
    }

    public boolean isAuthorizable() {
        return getModel() != null;
    }

    public @Nullable AuthorizableModel getModel() {
        return model;
    }

    public @NotNull String getType() {
        return type;
    }

    public @Nullable String getName() {
        return name;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @Nullable TreeNode getNode(@NotNull final String nodePath) {
        String path = getPath();
        if (path.equals(nodePath)) {
            return this;
        }
        if (nodePath.startsWith(path + "/")) {
            String[] segments = StringUtils.split(nodePath.substring(path.length() + 1), "/");
            TreeNode found = this;
            for (String name : segments) {
                if ((found = found.children.get(name)) == null) {
                    break;
                }
            }
            return found;
        }
        return null;
    }

    public void addNode(@NotNull final TreeNode treeNode) {
        String nodePath = treeNode.getPath();
        String path = getPath();
        if (nodePath.startsWith(path + "/")) {
            nodes = null; // reset collection
            String relativePath = nodePath.substring(path.length() + 1);
            int nextPathSep = relativePath.indexOf('/');
            if (nextPathSep < 0) {
                TreeNode current = children.get(relativePath);
                if (current != null) {
                    treeNode.children.putAll(current.children);
                }
                children.put(relativePath, treeNode);
            } else {
                String parentName = relativePath.substring(0, nextPathSep);
                TreeNode parentNode = children.get(parentName);
                if (parentNode == null) {
                    children.put(parentName, parentNode = createFolder(path + "/" + parentName));
                }
                parentNode.addNode(treeNode);
            }
        }
    }

    public Collection<TreeNode> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<>();
            nodes.addAll(children.values());
            nodes.sort(NODE_COMPARATOR);
        }
        return nodes;
    }

    public void toJson(@NotNull final JsonWriter writer, boolean recursive)
            throws IOException {
        AuthorizableModel model = getModel();
        writer.beginObject();
        this.toJsonData(writer, true);
        writer.name("children").beginArray();
        for (TreeNode child : getNodes()) {
            if (recursive) {
                child.toJson(writer, true);
            } else {
                writer.beginObject();
                child.toJsonData(writer, false);
                writer.endObject();
            }
        }
        writer.endArray();
        writer.endObject();
    }

    protected void toJsonData(@NotNull final JsonWriter writer, boolean loaded)
            throws IOException {
        AuthorizableModel model = getModel();
        writer.name("id").value(getPath())
                .name("text").value(getName())
                .name("name").value(getName())
                .name("path").value(getPath())
                .name("type").value(getType());
        if (model instanceof UserModel && !(model instanceof ServiceUserModel)) {
            UserModel user = (UserModel) model;
            writer.name("disabled").value(user.isDisabled())
                    .name("systemUser").value(user.isSystemUser());
        }
        writer.name("state").beginObject().name("loaded").value(loaded).endObject();
    }

    protected @NotNull TreeNode createFolder(@NotNull final String path) {
        String type = PATH_TO_TYPE.get(path);
        return new TreeNode(type != null ? type : TYPE_FOLDER, path);
    }

    @NotNull
    public static String getName(@NotNull final String path) {
        String name = StringUtils.substringAfterLast(path, "/");
        if (StringUtils.isBlank(name)) {
            name = "/";
        }
        return name;
    }
}
