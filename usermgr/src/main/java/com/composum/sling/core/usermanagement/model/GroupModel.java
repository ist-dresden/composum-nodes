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

    public GroupModel(@NotNull final Authorizables.Context context,
                      @NotNull final Group jcrGroup) throws RepositoryException {
        super(context, jcrGroup);
        members = stripIDs(jcrGroup.getMembers());
        declaredMembers = stripIDs(jcrGroup.getDeclaredMembers());
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
    public Collection<UserModel> getUsers(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getUsers(context, getMembers());
    }

    @NotNull
    public Collection<UserModel> getDeclaredUsers(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getUsers(context, getDeclaredMembers());
    }

    @NotNull
    public Collection<GroupModel> getGroups(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getGroups(context, getMembers());
    }

    @NotNull
    public Collection<GroupModel> getDeclaredGroups(@NotNull final Authorizables.Context context)
            throws RepositoryException {
        return getGroups(context, getDeclaredMembers());
    }

    public void toJson(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("type").value("group");
        writer.name("id").value(getId());
        writer.name("name").value(getPrincipalName());
        writer.name("path").value(getPath());
        writer.name("declaredMembers").beginArray();
        for (String id : getDeclaredMembers()) {
            writer.value(id);
        }
        writer.name("Members").beginArray();
        for (String id : getMembers()) {
            writer.value(id);
        }
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
        writer.endObject();
    }
}
