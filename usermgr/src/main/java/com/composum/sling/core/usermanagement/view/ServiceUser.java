package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.ServiceUserModel;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ServiceUser extends View {

    @Override
    protected @NotNull Class<? extends Authorizable> getSelector() {
        return com.composum.sling.core.usermanagement.service.ServiceUser.class;
    }

    public ServiceUserModel getService(){
        return (ServiceUserModel) getModel();
    }
}
