package com.composum.sling.core.usermanagement.model;

import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    protected AuthorizableModel(Authorizable authorizable) throws RepositoryException {
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
    public Collection<GroupModel> getGroupsOf(@Nullable final UserManager userManager)
            throws RepositoryException {
        return getGroups(userManager, getMemberOf());
    }

    @NotNull
    public Collection<GroupModel> getDeclaredGroupsOf(@Nullable final UserManager userManager)
            throws RepositoryException {
        return getGroups(userManager, getDeclaredMemberOf());
    }

    @NotNull
    public static Collection<UserModel> getUsers(@Nullable final UserManager userManager,
                                                 @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<UserModel> result = new ArrayList<>();
        for (User jcrUser : Authorizables.loadAuthorizables(userManager, User.class, idSet)) {
            result.add(new UserModel(jcrUser));
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
    public static Collection<GroupModel> getGroups(@Nullable final UserManager userManager,
                                                   @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<GroupModel> result = new ArrayList<>();
        for (Group jcrGroup : Authorizables.loadAuthorizables(userManager, Group.class, idSet)) {
            result.add(new GroupModel(jcrGroup));
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
