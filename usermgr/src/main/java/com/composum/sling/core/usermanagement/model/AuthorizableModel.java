package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.*;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public abstract class AuthorizableModel implements Serializable, Comparable<AuthorizableModel> {

    public static final String TYPE_GROUP = "group";
    public static final String TYPE_USER = "user";
    public static final String TYPE_SERVICE = "service";

    public static final Map<String, String> TYPE_TO_ICON = new HashMap<String, String>() {{
        put(TYPE_GROUP, "users");
        put(TYPE_USER, "user");
        put("system", "user-o");
        put(TYPE_SERVICE, "cog");
    }};

    protected final String type;
    protected final String id;
    protected final String path;
    protected final String principalName;

    protected final Set<String> memberOf;
    protected final Set<String> declaredMemberOf;

    protected final Authorizables.Context context;

    private transient Collection<GroupModel> modelOf;
    private transient Collection<GroupModel> declaredModelOf;

    protected AuthorizableModel(@NotNull final Authorizables.Context context,
                                @NotNull final AuthorizableWrapper authorizable) throws RepositoryException {
        this.context = context;
        type = getType(authorizable);
        id = authorizable.getID();
        path = authorizable.getPath();
        principalName = authorizable.getPrincipal().getName();
        memberOf = stripIDs(authorizable.memberOf());
        declaredMemberOf = stripIDs(authorizable.declaredMemberOf());
    }

    public abstract boolean isGroup();

    public void toJson(JsonWriter writer) throws IOException {
        writer.beginObject();
        toJsonData(writer);
        writer.endObject();
    }

    protected void toJsonData(JsonWriter writer) throws IOException {
        writer.name("type").value(getType());
        writer.name("id").value(getId());
        writer.name("name").value(getPrincipalName());
        writer.name("path").value(getPath());
        writer.name("declaredMemberOf").beginArray();
        for (String id : getDeclaredMemberOf()) {
            writer.value(id);
        }
        writer.endArray();
        writer.name("memberOf").beginArray();
        for (String id : getMemberOf()) {
            writer.value(id);
        }
        writer.endArray();
        // for backwards compatibility
        writer.name("principalName").value(getPrincipalName());
        writer.name("isGroup").value(isGroup());
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull String getTypeIcon() {
        return TYPE_TO_ICON.get(getType());
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getPrincipalName() {
        return principalName;
    }

    @NotNull
    public Set<String> getMemberOf() {
        return memberOf;
    }

    @NotNull
    public Set<String> getDeclaredMemberOf() {
        return declaredMemberOf;
    }

    @NotNull
    public Collection<GroupModel> getModelOf()
            throws RepositoryException {
        if (modelOf == null) {
            modelOf = getGroups(context, getMemberOf());
        }
        return modelOf;
    }

    @NotNull
    public Collection<GroupModel> getDeclaredModelOf()
            throws RepositoryException {
        if (declaredModelOf == null) {
            declaredModelOf = getGroups(context, getDeclaredMemberOf());
        }
        return declaredModelOf;
    }

    @Override
    public boolean equals(Object other) {
        AuthorizableModel otherModel;
        return other instanceof AuthorizableModel &&
                getId().equals((otherModel = (AuthorizableModel) other).getId()) && isGroup() == otherModel.isGroup();
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public int compareTo(AuthorizableModel other) {
        return getKey().compareTo(other.getKey());
    }

    protected String getKey() {
        return getRank() + ':' + getId();
    }

    protected abstract int getRank();

    @NotNull
    public static Collection<UserModel> getUsers(@NotNull final Authorizables.Context context,
                                                 @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<UserModel> result = new ArrayList<>();
        for (UserWrapper jcrUser : context.getService().loadAuthorizables(context, UserWrapper.class, idSet)) {
            result.add(new UserModel(context, jcrUser));
        }
        return result;
    }

    @NotNull
    public static Collection<GroupModel> getGroups(@NotNull final Authorizables.Context context,
                                                   @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<GroupModel> result = new ArrayList<>();
        for (GroupWrapper jcrGroup : context.getService().loadAuthorizables(context, GroupWrapper.class, idSet)) {
            result.add(new GroupModel(context, jcrGroup));
        }
        return result;
    }

    @NotNull
    public static Collection<AuthorizableModel> getModels(@NotNull final Authorizables.Context context,
                                                          @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<AuthorizableModel> result = new ArrayList<>();
        for (AuthorizableWrapper authorizable : context.getService().loadAuthorizables(context, AuthorizableWrapper.class, idSet)) {
            if (authorizable instanceof GroupWrapper) {
                result.add(new GroupModel(context, (GroupWrapper) authorizable));
            } else if (authorizable instanceof UserWrapper) {
                result.add(new UserModel(context, (UserWrapper) authorizable));
            } else if (authorizable instanceof ServiceUserWrapper) {
                result.add(new ServiceUserModel(context, (ServiceUserWrapper) authorizable));
            }
        }
        return result;
    }

    @NotNull
    protected static Set<String> stripIDs(@NotNull final Iterator<? extends AuthorizableWrapper> authorizableIterator)
            throws RepositoryException {
        Set<String> idSet = new TreeSet<>();
        while (authorizableIterator.hasNext()) {
            AuthorizableWrapper authorizable = authorizableIterator.next();
            idSet.add(authorizable.getID());
        }
        return idSet;
    }

    protected static String getType(@NotNull final AuthorizableWrapper authorizable) {
        if (authorizable instanceof GroupWrapper) {
            return TYPE_GROUP;
        } else if (authorizable instanceof ServiceUserWrapper) {
            return TYPE_SERVICE;
        } else {
            return TYPE_USER;
        }
    }
}
