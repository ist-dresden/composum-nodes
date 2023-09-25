package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.usermanagement.service.ServiceUserWrapper;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.util.Collection;

public class ServiceUserModel extends UserModel {

    protected final String serviceName;
    protected final String serviceInfo;

    private transient Collection<AuthorizableModel> userOf;
    private transient Collection<AuthorizableModel> declaredUserOf;

    public ServiceUserModel(@NotNull final Authorizables.Context context, @NotNull final ServiceUserWrapper service) throws RepositoryException {
        super(context, service);
        serviceName = service.getServiceName();
        serviceInfo = service.getServiceInfo();
    }

    @Override
    protected int getRank() {
        return 2;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public boolean isServiceUser() {
        return true;
    }

    @NotNull
    public Collection<AuthorizableModel> getUserOf()
            throws RepositoryException {
        if (userOf == null) {
            userOf = getModels(context, getMemberOf());
        }
        return userOf;
    }

    @NotNull
    public Collection<AuthorizableModel> getDeclaredUserOf()
            throws RepositoryException {
        if (declaredUserOf == null) {
            declaredUserOf = getModels(context, getDeclaredMemberOf());
        }
        return declaredUserOf;
    }
}
