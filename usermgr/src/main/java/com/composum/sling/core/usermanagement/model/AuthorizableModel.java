package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class AuthorizableModel implements Serializable, Comparable<AuthorizableModel> {

    protected final String id;
    protected final String path;
    protected final String principalName;

    protected final Set<String> memberOf;
    protected final Set<String> declaredMemberOf;

    protected final Authorizables.Context context;

    protected AuthorizableModel(@NotNull final Authorizables.Context context,
                                @NotNull final Authorizable authorizable) throws RepositoryException {
        this.context = context;
        id = authorizable.getID();
        path = authorizable.getPath();
        principalName = authorizable.getPrincipal().getName();
        memberOf = stripIDs(authorizable.memberOf());
        declaredMemberOf = stripIDs(authorizable.declaredMemberOf());
    }

    public abstract boolean isGroup();

    public abstract void toJson(JsonWriter writer) throws IOException;

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
    public Collection<GroupModel> getGroupsOf(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getGroups(context, getMemberOf());
    }

    @NotNull
    public Collection<GroupModel> getDeclaredGroupsOf(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getGroups(context, getDeclaredMemberOf());
    }

    @NotNull
    public static Collection<UserModel> getUsers(@NotNull final Authorizables.Context context,
                                                 @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<UserModel> result = new ArrayList<>();
        for (User jcrUser : context.getService().loadAuthorizables(context, User.class, idSet)) {
            result.add(new UserModel(context, jcrUser));
        }
        return result;
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
        return (isGroup() ? "0:" : "1:") + getId();
    }

    @NotNull
    public static Collection<GroupModel> getGroups(@NotNull final Authorizables.Context context,
                                                   @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<GroupModel> result = new ArrayList<>();
        for (Group jcrGroup : context.getService().loadAuthorizables(context, Group.class, idSet)) {
            result.add(new GroupModel(context, jcrGroup));
        }
        return result;
    }

    @NotNull
    protected static Set<String> stripIDs(@NotNull final Iterator<? extends Authorizable> authorizableIterator)
            throws RepositoryException {
        Set<String> idSet = new TreeSet<>();
        while (authorizableIterator.hasNext()) {
            Authorizable authorizable = authorizableIterator.next();
            idSet.add(authorizable.getID());
        }
        return idSet;
    }
}
