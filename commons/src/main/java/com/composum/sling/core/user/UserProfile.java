package com.composum.sling.core.user;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static com.composum.sling.core.user.UserProfile.SERVICE_KEY;

/**
 * the user profile bean derived from the AEM user profile approach
 */
@Restricted(key = SERVICE_KEY)
public class UserProfile extends AbstractSlingBean {

    public static final String SERVICE_KEY = "nodes/users/profile";

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

    public static final String NN_PROFILE = "profile";
    public static final String USERS_ROOT = "/home/";

    protected boolean allowed;
    protected ValueMap values;

    private transient String name;
    private transient String address;

    public UserProfile(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public UserProfile(BeanContext context) {
        super(context);
    }

    public UserProfile() {
        super();
    }

    @Override
    public void initialize(BeanContext context, Resource resource) {
        allowed = context.getService(ServiceRestrictions.class).isPermissible(context.getRequest(),
                new ServiceRestrictions.Key(SERVICE_KEY), ServiceRestrictions.Permission.read);
        if (allowed) {
            String path = resource.getPath();
            if (!path.startsWith(USERS_ROOT)) {
                // try to use the requests suffix to determine the profile resource to edit
                String suffix = context.getRequest().getRequestPathInfo().getSuffix();
                if (StringUtils.isNotBlank(suffix) && suffix.startsWith(USERS_ROOT)) {
                    resource = context.getResolver().resolve(suffix);
                    path = resource.getPath();
                }
            }
            if (!path.endsWith("/" + NN_PROFILE)) {
                // use the 'profile' child of the resource if the resource seems to be the user node itself
                resource = context.getResolver().resolve(path + "/" + NN_PROFILE);
            }
            super.initialize(context, resource);
            values = resource.getValueMap();
        } else {
            values = new ValueMapDecorator(Collections.emptyMap());
        }
    }

    public @NotNull ValueMap getValues() {
        return values;
    }

    public boolean isValid() {
        return allowed && !ResourceUtil.isSyntheticResource(resource) && !ResourceUtil.isNonExistingResource(resource);
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
