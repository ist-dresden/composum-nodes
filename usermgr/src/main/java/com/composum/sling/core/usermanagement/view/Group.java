package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.GroupModel;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by mzeibig on 17.11.15.
 */
public class Group extends View {

    @Override
    protected @NotNull Class<? extends Authorizable> getSelector() {
        return org.apache.jackrabbit.api.security.user.Group.class;
    }

    public @NotNull GroupModel getGroup() {
        return (GroupModel) Objects.requireNonNull(getModel());
    }
}
