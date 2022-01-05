package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.ServiceRestrictions;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component(
        service = ServiceRestrictions.class
)
@Designate(ocd = ServiceRestrictionsImpl.Config.class)
public class ServiceRestrictionsImpl implements ServiceRestrictions {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRestrictionsImpl.class);

    @ObjectClassDefinition(
            name = "Composum Service Restrictions"
    )
    public @interface Config {

        @AttributeDefinition(
                description = "the general on/off switch for the restrictions (default: true)"
        )
        boolean enabled() default true;

        @AttributeDefinition(
                description = "the default permission if no restriction specified for a feature (default: write)"
        )
        Permission defaultPermission() default Permission.write;

        @AttributeDefinition(
                description = "the permission limit for the individual choice in the users session (default: 'write:admin')"
        )
        String userOption() default "write:admin";

        @AttributeDefinition(
                description = "the set of service restrictions"
        )
        String[] restrictions();
    }

    private final Map<Key, Restriction> restrictions = Collections.synchronizedMap(new HashMap<>());

    private final Map<Permission, Set<String>> userOptions = Collections.synchronizedMap(new HashMap<>());

    private Config config;

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(final BundleContext bundleContext, final Config config) {
        this.bundleContext = bundleContext;
        this.config = config;
        restrictions.clear();
        for (String rule : config.restrictions()) {
            addRestriction(rule);
        }
        userOptions.clear();
        for (String rule : StringUtils.split(config.userOption(), ",")) {
            String[] permAuth = StringUtils.split(rule, ":", 2);
            try {
                Permission permission = Permission.valueOf(permAuth[0]);
                Set<String> authorizables = userOptions.computeIfAbsent(permission, k -> new TreeSet<>());
                authorizables.add(permAuth.length > 1 ? permAuth[1] : "");
            } catch (IllegalArgumentException ex) {
                LOG.error(ex.toString());
            }
        }
    }

    @Override
    public boolean isUserOptionAllowed(@NotNull final SlingHttpServletRequest request,
                                       @NotNull final Permission permission) {
        final String userId = request.getResourceResolver().getUserID();
        Authorizable authorizable = null;
        for (Permission option : userOptions.keySet()) {
            if (option.compareTo(permission) >= 0) {
                for (String name : userOptions.get(option)) {
                    if (StringUtils.isBlank(name)) {
                        return true;
                    }
                    if (StringUtils.isNotBlank(userId)) {
                        if (userId.equals(name)) {
                            return true;
                        }
                        if (authorizable == null) {
                            authorizable = getAuthorizable(request);
                        }
                        try {
                            final Iterator<Group> groups;
                            if (authorizable != null && (groups = authorizable.memberOf()) != null) {
                                while (groups.hasNext()) {
                                    if (groups.next().getID().equals(name)) {
                                        return true;
                                    }
                                }
                            }
                        } catch (RepositoryException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Permission getDefaultPermisson() {
        return config.defaultPermission();
    }

    protected void addRestriction(@NotNull final String rule) {
        final String[] keyValue = StringUtils.split(rule, "=", 2);
        if (keyValue.length == 2) {
            final Key key = new Key(keyValue[0]);
            final Restriction restriction = new Restriction(keyValue[1]);
            restrictions.put(key, restriction);
        }
    }

    @NotNull
    protected Restriction getRestriction(@NotNull Key key) {
        Restriction restriction = restrictions.get(key);
        if (restriction == null) {
            String id;
            while (restriction == null && !(key =
                    new Key((id = key.toString()).substring(0, id.lastIndexOf('/') + 1))).isEmpty()) {
                restriction = restrictions.get(key);
            }
            restrictions.put(key, restriction != null ? restriction : (restriction = new Restriction()));
        }
        return restriction;
    }

    public boolean isPermissible(@Nullable final SlingHttpServletRequest request,
                                 @Nullable final Key key, @NotNull final Permission needed) {
        if (config.enabled()) {
            Permission permission = getPermission(key);
            final HttpSession session;
            if (request != null && (session = request.getSession(false)) != null) {
                final Object temporary = session.getAttribute(SA_PERMISSION);
                if (temporary instanceof Permission && isUserOptionAllowed(request, (Permission) temporary)) {
                    permission = (Permission) temporary;
                }
            }
            return permission.matches(needed);
        }
        return true;
    }

    @NotNull
    public Permission getPermission(@Nullable final Key key) {
        if (config.enabled()) {
            final Restriction restriction = key != null ? getRestriction(key) : null;
            return restriction != null && restriction.permission != null
                    ? restriction.permission : config.defaultPermission();
        }
        return Permission.write;
    }

    @Nullable
    public String getRestrictions(@Nullable final Key key) {
        if (config.enabled() && key != null) {
            final Restriction restriction = getRestriction(key);
            return restriction.restrictions;
        }
        return null;
    }

    @Nullable
    public Authorizable getAuthorizable(@NotNull final SlingHttpServletRequest request) {
        final String userId = request.getResourceResolver().getUserID();
        final Session session;
        if (StringUtils.isNotBlank(userId) &&
                (session = request.getResourceResolver().adaptTo(Session.class)) != null) {
            try {
                final UserManager userManager = session instanceof JackrabbitSession
                        ? ((JackrabbitSession) session).getUserManager() : null;
                if (userManager != null) {
                    return userManager.getAuthorizable(userId);
                }
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return null;
    }
}
