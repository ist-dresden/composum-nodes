package com.composum.sling.core.pckgmgr.manager.tree;

import com.google.gson.stream.JsonWriter;

import javax.jcr.RepositoryException;
import java.io.IOException;

public interface TreeItem {

    String getName();

    String getPath();

    void toJson(JsonWriter writer) throws RepositoryException, IOException;
}
