package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.ServiceRestrictions;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

/**
 * The service to restict the service operations and the Sling POST requests.
 */
@Component(
        service = {ServiceRestrictions.class, Filter.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Service Restrictions",
                "sling.filter.scope=REQUEST"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Designate(ocd = ServiceRestrictionsImpl.Config.class)
public class ServiceRestrictionsImpl implements ServiceRestrictions, Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRestrictionsImpl.class);

    @ObjectClassDefinition(
            name = "Composum Service Restrictions Configuration"
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

        @AttributeDefinition(
                name = "Path Restrictions",
                description = "the mapping of repository path patterns to service keys (e.g. '^/content(/.*)?$=pages/content/edit')"
        )
        String[] restictedPaths();

        @AttributeDefinition(
                name = "Service Ranking",
                description = "the ranking of the service to place the servlet filter at the right place in the filter chain"
        )
        int service_ranking() default 6500;
    }

    private final Map<Key, Restriction> restrictions = Collections.synchronizedMap(new HashMap<>());

    private final Map<Permission, Set<String>> userOptions = Collections.synchronizedMap(new HashMap<>());

    private BundleContext bundleContext;

    private Config config;

    private final Map<Pattern, ServiceRestrictions.Key> restrictedPaths = new LinkedHashMap<>();

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
        this.restrictedPaths.clear();
        for (final String rule : config.restictedPaths()) {
            final String[] keyVal = StringUtils.split(rule, "=", 2);
            if (keyVal.length == 2 && StringUtils.isNotBlank(keyVal[0]) && StringUtils.isNotBlank(keyVal[1])) {
                restrictedPaths.put(Pattern.compile(keyVal[0]), new ServiceRestrictions.Key(keyVal[1]));
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

    public boolean checkAuthorizables(@NotNull final SlingHttpServletRequest request,
                                      @Nullable String restrictions) {
        if (StringUtils.isNotBlank(restrictions) && restrictions.startsWith(AUTHORIZABLE_RESTRICTION_PREFIX)) {
            Authorizable authorizable = getAuthorizable(request);
            if (authorizable instanceof User) {
                try {
                    final String name = authorizable.getID();
                    for (String id : StringUtils.split(restrictions
                            .substring(AUTHORIZABLE_RESTRICTION_PREFIX.length()), ",")) {
                        if (authorizable.getID().equals(id)) {
                            return true;
                        }
                        final Iterator<Group> groups;
                        if ((groups = authorizable.memberOf()) != null) {
                            while (groups.hasNext()) {
                                if (groups.next().getID().equals(id)) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
            return false;
        }
        return true;
    }

    // POST servlet filter

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain chain)
            throws IOException, ServletException {
        if (config.enabled() && this.restrictedPaths.size() > 0
                && servletRequest instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
            if (METHOD_POST.equals(request.getMethod())) {
                final String path = request.getResource().getPath();
                for (Map.Entry<Pattern, ServiceRestrictions.Key> entry : this.restrictedPaths.entrySet()) {
                    if (entry.getKey().matcher(path).matches()
                            && !isPermissible(request, entry.getValue(), ServiceRestrictions.Permission.write)) {
                        final SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
