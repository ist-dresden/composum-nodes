package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.usermanagement.service.ServiceUser;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AuthorizablesView {

    public interface NodeUrlBuilder {

        @Nullable
        String buildUrl(@NotNull AuthorizableModel node);
    }

    public interface PathUrlBuilder {

        @Nullable
        String buildUrl(@NotNull AuthorizableModel node, @NotNull String path);
    }

    protected int index = 0;
    protected final Map<String, Integer> indexes = new LinkedHashMap<>();
    protected final Map<String, AuthorizableModel> nodes = new TreeMap<>();
    protected final AuthorizableModel singleFocus;

    protected final Authorizables.Context context;

    public AuthorizablesView(@NotNull final Authorizables.Context context,
                             @Nullable final String selector,
                             @Nullable final String nameQueryPattern,
                             @Nullable final String pathPattern)
            throws RepositoryException {
        this(context, Authorizables.selector(selector), nameQueryPattern,
                StringUtils.isNotBlank(pathPattern) ? new Authorizables.Filter.Path(pathPattern) : null);
    }

    public AuthorizablesView(@NotNull final Authorizables.Context context,
                             @Nullable final Class<? extends Authorizable> selector,
                             @Nullable final String nameQueryPattern,
                             @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        this.context = context;
        Set<Authorizable> authorizables = context.getService().findAuthorizables(
                context, selector, nameQueryPattern, filter);
        if (authorizables.size() < 1 && StringUtils.isNotBlank(nameQueryPattern)) {
            authorizables = context.getService().findAuthorizables(
                    context, null, nameQueryPattern, null);
        }
        for (Authorizable authorizable : authorizables) {
            addNode(authorizable);
        }
        singleFocus = nodes.size() == 1 ? nodes.values().iterator().next() : null;
    }

    public AuthorizableModel getSingleFocus() {
        return singleFocus;
    }

    protected AuthorizableModel addNode(@Nullable final Authorizable authorizable)
            throws RepositoryException {
        AuthorizableModel result = null;
        if (authorizable != null) {
            result = nodes.get(authorizable.getID());
            if (result == null) {
                result = createNode(authorizable);
                if (result != null) {
                    nodes.put(result.getId(), result);
                    indexes.put(result.getId(), ++index);
                }
            }
        }
        return result;
    }

    @Nullable
    protected AuthorizableModel createNode(@NotNull final Authorizable authorizable)
            throws RepositoryException {
        return authorizable instanceof Group
                ? new GroupModel(context, (Group) authorizable)
                : authorizable instanceof User
                ? new UserModel(context, (User) authorizable)
                : authorizable instanceof ServiceUser
                ? new ServiceUserModel(context, (ServiceUser) authorizable)
                : null;
    }

    public void toJson(@NotNull final JsonWriter writer) throws IOException {
        writer.beginObject();
        if (singleFocus != null) {
            writer.name("focus");
            singleFocus.toJson(writer);
        } else {
            writer.name("nodes").beginArray();
            for (AuthorizableModel node : nodes.values()) {
                node.toJson(writer);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    protected String getNodeClass(AuthorizableModel node) {
        StringBuilder builder = new StringBuilder();
        if (node.isGroup()) {
            builder.append("group");
        } else {
            UserModel user = (UserModel) node;
            if (user.isDisabled()) {
                builder.append("disabled");
            } else if (user.isAdmin()) {
                builder.append("admin");
            } else if (user.isServiceUser()) {
                builder.append("service");
            } else if (user.isSystemUser()) {
                builder.append("system");
            } else {
                builder.append("user");
            }
        }
        if (singleFocus != null && singleFocus.getId().equals(node.getId())) {
            builder.append(" focus");
        }
        return builder.toString();
    }
}
