package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.usermanagement.model.UserModel;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.util.Objects;

/**
 * Created by mzeibig on 16.11.15.
 */
public class User extends View {

    public static final String NN_PROFILE = "profile";

    public class Profile {

        public static final String PN_GENDER = "gender";
        public static final String PN_TITLE = "title";
        public static final String PN_GIVEN_NAME = "givenName";
        public static final String PN_FAMILY_NAME = "familyName";
        public static final String PN_JOB_TITLE = "jobTitle";
        public static final String PN_EMAIL = "email";
        public static final String PN_PHONE_NUMBER = "phoneNumber";
        public static final String PN_STREET = "street";
        public static final String PN_POSTAL_CODE = "postalCode";
        public static final String PN_CITY = "city";
        public static final String PN_COUNTRY = "country";
        public static final String PN_ABOUT = "aboutMe";

        protected final Resource resource;
        protected final ValueMap values;

        private transient String name;
        private transient String address;

        public Profile(@NotNull final Resource resource) {
            this.resource = resource;
            values = resource.getValueMap();
        }

        public @NotNull ValueMap getValues() {
            return values;
        }

        public boolean isValid() {
            return !ResourceUtil.isSyntheticResource(resource);
        }

        public @NotNull String getName() {
            if (name == null) {
                StringBuilder builder = new StringBuilder();
                String value;
                if (StringUtils.isNotBlank(value = values.get(PN_TITLE, String.class))) {
                    builder.append(value);
                }
                if (StringUtils.isNotBlank(value = values.get(PN_GIVEN_NAME, String.class))) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(value);
                }
                if (StringUtils.isNotBlank(value = values.get(PN_FAMILY_NAME, String.class))) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(value);
                }
                name = builder.toString();
            }
            return name;
        }

        public @NotNull String getEmail() {
            return values.get(PN_EMAIL, "");
        }

        public @NotNull String getPhoneNumber() {
            return values.get(PN_PHONE_NUMBER, "");
        }

        public @NotNull String getAddress() {
            if (address == null) {
                StringBuilder builder = new StringBuilder();
                String value;
                if (StringUtils.isNotBlank(value = values.get(PN_STREET, String.class))) {
                    builder.append(value);
                }
                if (StringUtils.isNotBlank(value = values.get(PN_POSTAL_CODE, String.class))) {
                    if (builder.length() > 0) {
                        builder.append("<br/>");
                    }
                    builder.append(value);
                    if (StringUtils.isNotBlank(value = values.get(PN_CITY, String.class))) {
                        builder.append(' ').append(value);
                    }
                } else {
                    if (StringUtils.isNotBlank(value = values.get(PN_CITY, String.class))) {
                        if (builder.length() > 0) {
                            builder.append("<br/>");
                        }
                        builder.append(value);
                    }
                }
                if (StringUtils.isNotBlank(value = values.get(PN_COUNTRY, String.class))) {
                    if (builder.length() > 0) {
                        builder.append("<br/>");
                    }
                    builder.append(value);
                }
                address = builder.toString();
            }
            return address;
        }

        public @NotNull String getAbout() {
            return values.get(PN_ABOUT, "");
        }
    }

    private transient Boolean isAdmin;
    private transient Profile profile;

    @Override
    protected @NotNull Class<? extends Authorizable> getSelector() {
        return org.apache.jackrabbit.api.security.user.User.class;
    }

    public @NotNull UserModel getUser() {
        return (UserModel) Objects.requireNonNull(getModel());
    }

    public boolean getHasProfile() {
        return getProfile().isValid();
    }

    public @NotNull Profile getProfile() {
        if (profile == null) {
            String profilePath = getPath() + "/" + NN_PROFILE;
            Resource profileRes = getResolver().getResource(profilePath);
            profile = new Profile(profileRes != null ? profileRes
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
