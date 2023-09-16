package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.usermanagement.service.UserWrapper;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class UserModel extends AuthorizableModel {

    private static final Logger LOG = LoggerFactory.getLogger(UserModel.class);

    protected final ValueMap values;

    protected final boolean admin;
    protected final boolean systemUser;
    protected final boolean disabled;
    protected final String disabledReason;

    private transient Collection<AuthorizableModel> serviceUsers;

    public UserModel(@NotNull final Authorizables.Context context,
                     @NotNull final UserWrapper jcrUser)
            throws RepositoryException {
        this(context, (AuthorizableWrapper) jcrUser);
    }

    protected UserModel(@NotNull final Authorizables.Context context,
                        @NotNull final AuthorizableWrapper authorizable)
            throws RepositoryException {
        super(context, authorizable);
        if (authorizable instanceof UserWrapper) {
            UserWrapper userWrapper = (UserWrapper) authorizable;
            admin = userWrapper.isAdmin();
            systemUser = userWrapper.isSystemUser();
            disabled = userWrapper.isDisabled();
            disabledReason = userWrapper.getDisabledReason();
        } else {
            admin = false;
            systemUser = true;
            disabled = false;
            disabledReason = null;
        }
        Map<String, Object> properties = new TreeMap<>();
        Iterator<String> propertyNames = authorizable.getPropertyNames();
        while (propertyNames.hasNext()) {
            String name = propertyNames.next();
            Value[] property = authorizable.getProperty(name);
            String[] vs = new String[property.length];
            for (int i = 0; i < property.length; i++) {
                vs[i] = property[i].getString();
            }
            properties.put(name, vs);
        }
        values = new ValueMapDecorator(properties);
    }

    @Override
    protected int getRank() {
        return 1;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public @NotNull String getTypeIcon() {
        return isSystemUser() && !isServiceUser() ? "user-o" : TYPE_TO_ICON.get(getType());
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public boolean isServiceUser() {
        return false;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public @Nullable String getDisabledReason() {
        return disabledReason;
    }

    public Collection<AuthorizableModel> getServiceUsers() {
        if (serviceUsers == null) {
            serviceUsers = new ArrayList<>();
            try {
                AuthorizablesRefs refs = new AuthorizablesRefs(context, null, getId());
                for (AuthorizablesMap.Relation relation : refs.sourceRelations) {
                    serviceUsers.add(relation.source);
                }
            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return serviceUsers;
    }

    @Override
    protected void toJsonData(JsonWriter writer) throws IOException {
        super.toJsonData(writer);
        writer.name("admin").value(isAdmin());
        writer.name("system").value(isSystemUser());
        writer.name("disabled").value(isDisabled());
        if (StringUtils.isNotBlank(getDisabledReason())) {
            writer.name("reason").value(getDisabledReason());
        }
        for (Map.Entry<String, Object> property : values.entrySet()) {
            Object value = property.getValue();
            if (value != null) {
                writer.name(property.getKey()).value(value.toString());
            }
        }
        // for backwards compatibility
        writer.name("systemUser").value(isSystemUser());
    }
}
