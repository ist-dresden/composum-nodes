package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.UserModel;
import com.composum.sling.core.user.UserProfile;
import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.UserWrapper;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.util.Objects;

import static com.composum.sling.core.user.UserProfile.NN_PROFILE;

/**
 * Created by mzeibig on 16.11.15.
 */
public class User extends View {

    private transient Boolean isAdmin;
    private transient UserProfile profile;

    @Override
    protected @NotNull Class<? extends AuthorizableWrapper> getSelector() {
        return UserWrapper.class;
    }

    public @NotNull UserModel getUser() {
        return (UserModel) Objects.requireNonNull(getModel());
    }

    public boolean getHasProfile() {
        return getProfile().isValid();
    }

    public @NotNull UserProfile getProfile() {
        if (profile == null) {
            String profilePath = getPath() + "/" + NN_PROFILE;
            Resource profileRes = getResolver().getResource(profilePath);
            profile = new UserProfile(context, profileRes != null ? profileRes
                    : new SyntheticResource(getResolver(), profilePath, null));
        }
        return profile;
    }

    public String getUserLabel() {
        if (isAdmin()) {
            return "Administrator";
        } else if (isSystemUser()) {
            return "System User";
        } else {
            return "User";
        }
    }

    public boolean isSystemUser() {
        return getUser().isSystemUser();
    }

    public boolean isAdmin() {
        return getUser().isAdmin();
    }

    public boolean isDisabled() {
        return getUser().isDisabled();
    }

    public String getDisabledReason() {
        return getUser().getDisabledReason();
    }

    /**
     * Returns true if the current request user is the admin user.
     */
    public boolean isCurrentUserAdmin() throws RepositoryException {
        if (isAdmin == null) {
            isAdmin = false;
            final JackrabbitSession session = (JackrabbitSession) getSession();
            final UserManager userManager = session.getUserManager();
            Authorizable a = userManager.getAuthorizable(getRequest().getUserPrincipal());
            if (a instanceof org.apache.jackrabbit.api.security.user.User) {
                isAdmin = ((org.apache.jackrabbit.api.security.user.User) a).isAdmin();
            }
        }
        return isAdmin;
    }
}
