package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.ServiceUserModel;
import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.ServiceUserWrapper;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ServiceUser extends View {

    @Override
    protected @NotNull Class<? extends AuthorizableWrapper> getSelector() {
        return ServiceUserWrapper.class;
    }

    public ServiceUserModel getService() {
        return (ServiceUserModel) getModel();
    }
}
