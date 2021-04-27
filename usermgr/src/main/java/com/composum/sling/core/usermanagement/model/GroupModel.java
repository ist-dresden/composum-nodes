package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class GroupModel extends AuthorizableModel {

    protected final Set<String> members;
    protected final Set<String> declaredMembers;

    private transient Collection<UserModel> users;
    private transient Collection<UserModel> declaredUsers;
    private transient Collection<GroupModel> groups;
    private transient Collection<GroupModel> declaredGroups;

    public GroupModel(@NotNull final Authorizables.Context context,
                      @NotNull final Group jcrGroup) throws RepositoryException {
        super(context, jcrGroup);
        members = stripIDs(jcrGroup.getMembers());
        declaredMembers = stripIDs(jcrGroup.getDeclaredMembers());
    }

    @Override
    protected int getRank() {
        return 3;
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    @NotNull
    public Set<String> getMembers() {
        return members;
    }

    @NotNull
    public Set<String> getDeclaredMembers() {
        return declaredMembers;
    }

    @NotNull
    public Collection<UserModel> getUsers()
            throws RepositoryException {
        if (users == null) {
            users = getUsers(context, getMembers());
        }
        return users;
    }

    @NotNull
    public Collection<UserModel> getDeclaredUsers()
            throws RepositoryException {
        if (declaredUsers == null) {
            declaredUsers = getUsers(context, getDeclaredMembers());
        }
        return declaredUsers;
    }

    @NotNull
    public Collection<GroupModel> getGroups()
            throws RepositoryException {
        if (groups == null) {
            groups = getGroups(context, getMembers());
        }
        return groups;
    }

    @NotNull
    public Collection<GroupModel> getDeclaredGroups()
            throws RepositoryException {
        if (declaredGroups == null) {
            declaredGroups = getGroups(context, getDeclaredMembers());
        }
        return declaredGroups;
    }

    @Override
    protected void toJsonData(JsonWriter writer) throws IOException {
        super.toJsonData(writer);
        writer.name("declaredMembers").beginArray();
        for (String id : getDeclaredMembers()) {
            writer.value(id);
        }
        writer.endArray();
        writer.name("members").beginArray();
        for (String id : getMembers()) {
            writer.value(id);
        }
        writer.endArray();
    }
}
