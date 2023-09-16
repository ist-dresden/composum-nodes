package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.GroupModel;
import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.GroupWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by mzeibig on 17.11.15.
 */
public class Group extends View {

    @Override
    protected @NotNull Class<? extends AuthorizableWrapper> getSelector() {
        return GroupWrapper.class;
    }

    public @NotNull GroupModel getGroup() {
        return (GroupModel) Objects.requireNonNull(getModel());
    }
}
