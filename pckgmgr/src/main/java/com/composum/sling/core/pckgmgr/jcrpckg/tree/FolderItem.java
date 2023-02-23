package com.composum.sling.core.pckgmgr.jcrpckg.tree;

import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FolderItem extends LinkedHashMap<String, Object> implements TreeItem {

    public FolderItem(String path, String name) {
        this(path, name, "/".equals(path) ? "root" : "folder");
    }

    public FolderItem(String path, String name, String type) {
        put("id", path);
        put("path", path);
        put("name", name);
        put("text", name);
        put("type", type);
        Map<String, Object> treeState = new LinkedHashMap<>();
        treeState.put("loaded", Boolean.FALSE);
        put("state", treeState);
    }

    @Override
    public String getName() {
        return (String) get("text");
    }

    @Override
    public String getPath() {
        return (String) get("path");
    }

    @Override
    public void toJson(JsonWriter writer) throws IOException {
        JsonUtil.jsonMap(writer, this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TreeItem && getName().equals(((TreeItem) other).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
