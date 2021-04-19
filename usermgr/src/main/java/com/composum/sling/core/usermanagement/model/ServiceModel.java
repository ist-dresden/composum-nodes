package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.usermanagement.service.Service;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.io.IOException;

public class ServiceModel extends UserModel {

    public ServiceModel(@NotNull final Authorizables.Context context, @NotNull final Service service)
            throws RepositoryException {
        super(context, service);
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public boolean isServiceUser() {
        return true;
    }

    @Override
    public void toJson(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("type").value("service");
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
        writer.endObject();
    }
}
