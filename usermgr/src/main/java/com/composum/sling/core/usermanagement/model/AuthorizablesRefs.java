package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.Authorizables;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.util.HashSet;

public class AuthorizablesRefs extends AuthorizablesMap {

    public AuthorizablesRefs(@NotNull final Authorizables.Context context,
                             @Nullable final Class<? extends AuthorizableWrapper> selector,
                             @Nullable final String nameQueryPattern)
            throws RepositoryException {
        super(context, selector, nameQueryPattern, null);
    }

    protected void scanRelations(@Nullable final Class<? extends AuthorizableWrapper> selector,
                                 @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        if (singleFocus != null) {
            addSourceRelations(selector, filter, singleFocus, new HashSet<>());
        }
    }
}
