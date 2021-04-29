package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;

public class AuthorizablesTree extends AuthorizablesView {

    protected final TreeNode rootNode;

    public AuthorizablesTree(@NotNull final Authorizables.Context context,
                             @Nullable final String selector,
                             @Nullable final String nameQueryPattern,
                             @Nullable final String pathPattern)
            throws RepositoryException {
        this(context, Authorizables.selector(selector), nameQueryPattern,
                StringUtils.isNotBlank(pathPattern) ? new Authorizables.Filter.Path(pathPattern) : null);
    }

    public AuthorizablesTree(@NotNull final Authorizables.Context context,
                             @Nullable final Class<? extends Authorizable> selector,
                             @Nullable final String nameQueryPattern,
                             @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        super(context, selector, nameQueryPattern, filter);
        rootNode = new TreeNode(TreeNode.TYPE_ROOT, "/home");
        for (AuthorizableModel model : nodes.values()) {
            rootNode.addNode(new TreeNode(model));
        }
    }

    public @NotNull TreeNode getRootNode() {
        return rootNode;
    }

    public void toJson(@NotNull final JsonWriter writer, boolean recursive)
            throws IOException {
        getRootNode().toJson(writer, recursive);
    }

    @Override
    public void toJson(@NotNull final JsonWriter writer)
            throws IOException {
        getRootNode().toJson(writer, true);
    }
}
