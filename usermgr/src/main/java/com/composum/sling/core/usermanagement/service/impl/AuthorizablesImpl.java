package com.composum.sling.core.usermanagement.service.impl;

import com.composum.sling.core.usermanagement.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.*;
import org.apache.sling.serviceusermapping.Mapping;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class AuthorizablesImpl implements Authorizables {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizablesImpl.class);

    @Reference
    protected ServiceUserMapper serviceUserMapper;

    private boolean incompatibleServiceMapper = false;

    @Activate
    protected void activate() {
        incompatibleServiceMapper = false;
    }

    @Nullable
    public AuthorizableWrapper getAuthorizable(@NotNull final Context context, @NotNull final String id)
            throws RepositoryException {
        AuthorizableWrapper authorizable = context.getAuthorizables().get(id);
        if (authorizable == null) {
            UserManager userManager = context.getUserManager();
            if (userManager != null) {
                authorizable = getAuthorizableWrapper(id, userManager);
            }
            if (authorizable == null) {
                authorizable = getServiceUser(id);
            }
            if (authorizable != null) {
                context.getAuthorizables().put(id, authorizable);
            }
            if (authorizable instanceof ServiceUserWrapper) {
                ((ServiceUserWrapper) authorizable).initialize(context);
            }
        }
        return authorizable;
    }

    private static AuthorizableWrapper getAuthorizableWrapper(@NotNull String id, UserManager userManager) throws RepositoryException {
        AuthorizableWrapper wrapper = null;
        Authorizable jcrAuthorizable = userManager.getAuthorizable(id);
        if (jcrAuthorizable != null) {
            if (jcrAuthorizable instanceof User) {
                wrapper = new UserWrapper((User) jcrAuthorizable);

            } else if (jcrAuthorizable instanceof Group) {
                wrapper = new GroupWrapper((Group) jcrAuthorizable);
            } else {
                wrapper = new AuthorizableWrapper(jcrAuthorizable);
            }
        }
        return wrapper;
    }

    @Override
    @NotNull
    public Set<? extends AuthorizableWrapper> findAuthorizables(@NotNull final Context context,
                                                                @Nullable final Class<? extends AuthorizableWrapper> selector,
                                                                @Nullable String nameQueryPattern,
                                                                @Nullable final Filter filter)
            throws RepositoryException {
        Set<AuthorizableWrapper> result = new HashSet<>();
        if (StringUtils.isNotBlank(nameQueryPattern)) {
            nameQueryPattern = nameQueryPattern
                    .replaceAll("\\.\\*", "%")
                    .replace('*', '%');
        }
        UserManager userManager = context.getUserManager();
        if (userManager != null) {
            Iterator<AuthorizableWrapper> iterator = findAuthorizables(context, selector, nameQueryPattern);
            while (iterator.hasNext()) {
                AuthorizableWrapper authorizable = iterator.next();
                if (filter == null || filter.accept(authorizable)) {
                    result.add(authorizable);
                }
            }
        }
        for (ServiceUserWrapper serviceUser : findServiceUsers(context, selector, nameQueryPattern)) {
            if (filter == null || filter.accept(serviceUser)) {
                result.add(serviceUser);
            }
        }
        return result;
    }

    @Override
    @NotNull
    public <T extends AuthorizableWrapper> Collection<T> loadAuthorizables(@NotNull final Context context,
                                                                           @NotNull final Class<T> selector,
                                                                           @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<T> result = new ArrayList<>();
        for (String id : idSet) {
            AuthorizableWrapper authorizable = getAuthorizable(context, id);
            if (selector.isInstance(authorizable)) {
                result.add(selector.cast(authorizable));
            }
        }
        return result;
    }

    @NotNull
    protected Iterator<AuthorizableWrapper> findAuthorizables(@NotNull final Context context,
                                                              @Nullable final Class<? extends AuthorizableWrapper> selector,
                                                              @Nullable final String nameQueryPattern)
            throws RepositoryException {
        if (selector == null || !selector.equals(ServiceUserWrapper.class)) {
            UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final Query authorizableQuery = new Query() {
                    @Override
                    public <T> void build(final QueryBuilder<T> builder) {
                        builder.setCondition(builder
                                .nameMatches(StringUtils.isNotBlank(nameQueryPattern) ? nameQueryPattern : "%"));
                        builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                        if (selector != null) {
                            if (UserWrapper.class.isAssignableFrom(selector)) {
                                builder.setSelector(User.class);
                            } else if (GroupWrapper.class.isAssignableFrom(selector)) {
                                builder.setSelector(Group.class);
                            }
                        }
                    }
                };
                return getAuthorizableWrapperIterator(userManager, authorizableQuery);
            }
        }
        return Collections.emptyIterator();
    }

    @NotNull
    private static Iterator<AuthorizableWrapper> getAuthorizableWrapperIterator(UserManager userManager, Query authorizableQuery) throws RepositoryException {
        Iterator<Authorizable> jcrAuthorizableIterator = userManager.findAuthorizables(authorizableQuery);
        List<AuthorizableWrapper> result = new ArrayList<>();
        while (jcrAuthorizableIterator.hasNext()) {
            Authorizable jcrAuthorizable = jcrAuthorizableIterator.next();
            if (jcrAuthorizable instanceof User) {
                result.add(new UserWrapper((User) jcrAuthorizable));
            } else if (jcrAuthorizable instanceof Group) {
                result.add(new GroupWrapper((Group) jcrAuthorizable));
            } else {
                result.add(new AuthorizableWrapper(jcrAuthorizable));
            }
        }
        return result.iterator();
    }

    @NotNull
    protected List<ServiceUserWrapper> findServiceUsers(@NotNull final Context context,
                                                        @Nullable final Class<? extends AuthorizableWrapper> selector,
                                                        @Nullable final String nameQueryPattern)
            throws RepositoryException {
        List<ServiceUserWrapper> serviceUsers = new ArrayList<>();
        if (!incompatibleServiceMapper && (selector == null || selector.equals(ServiceUserWrapper.class))) {
            Pattern namePattern = StringUtils.isNotBlank(nameQueryPattern)
                    ? Pattern.compile("^" + nameQueryPattern.replaceAll("%", ".*") + "$")
                    : null;
            try {
                List<Mapping> mappings = serviceUserMapper.getActiveMappings();
                for (Mapping mapping : mappings) {
                    ServiceUserWrapper service = new ServiceUserWrapper(mapping);
                    if (namePattern == null || namePattern.matcher(service.getID()).matches()) {
                        service.initialize(context);
                        serviceUsers.add(service);
                    }
                }
            } catch (NoSuchMethodError nsme) { // ensure compatibility to AEM <6.5
                incompatibleServiceMapper = true;
                LOG.warn("incompatible ServiceUserMapper - no service user support (" + nsme + ")");
            }
        }
        return serviceUsers;
    }

    @Nullable
    protected ServiceUserWrapper getServiceUser(@NotNull final String id) {
        List<Mapping> mappings = serviceUserMapper.getActiveMappings();
        for (Mapping mapping : mappings) {
            ServiceUserWrapper service = new ServiceUserWrapper(mapping);
            if (id.equals(service.getID())) {
                return service;
            }
        }
        return null;
    }
}
