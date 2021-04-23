package com.composum.sling.core.usermanagement.service.impl;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.usermanagement.service.ServiceUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.serviceusermapping.Mapping;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AuthorizablesImpl implements Authorizables {

    @Reference
    protected ServiceUserMapper serviceUserMapper;

    @Nullable
    public Authorizable getAuthorizable(@NotNull final Context context, @NotNull final String id)
            throws RepositoryException {
        Authorizable authorizable = context.getAuthorizables().get(id);
        if (authorizable == null) {
            UserManager userManager = context.getUserManager();
            if (userManager != null) {
                authorizable = userManager.getAuthorizable(id);
            }
            if (authorizable == null) {
                authorizable = getServiceUser(context, id);
            }
            if (authorizable != null) {
                context.getAuthorizables().put(id, authorizable);
            }
            if (authorizable instanceof ServiceUser) {
                ((ServiceUser) authorizable).initialize(context);
            }
        }
        return authorizable;
    }

    @Override
    @NotNull
    public Set<Authorizable> findAuthorizables(@NotNull final Context context,
                                               @Nullable final Class<? extends Authorizable> selector,
                                               @Nullable final String nameQueryPattern,
                                               @Nullable final Filter filter)
            throws RepositoryException {
        Set<Authorizable> result = new HashSet<>();
        UserManager userManager = context.getUserManager();
        if (userManager != null) {
            Iterator<Authorizable> iterator = findAuthorizables(context, selector, nameQueryPattern);
            while (iterator.hasNext()) {
                Authorizable authorizable = iterator.next();
                if (filter == null || filter.accept(authorizable)) {
                    result.add(authorizable);
                }
            }
        }
        for (Authorizable authorizable : findServiceUsers(context, selector, nameQueryPattern)) {
            if (filter == null || filter.accept(authorizable)) {
                result.add(authorizable);
            }
        }
        return result;
    }

    @Override
    @NotNull
    public <T extends Authorizable> Collection<T> loadAuthorizables(@NotNull final Context context,
                                                                    @NotNull final Class<T> selector,
                                                                    @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<T> result = new ArrayList<>();
        UserManager userManager = context.getUserManager();
        for (String id : idSet) {
            Authorizable authorizable = getAuthorizable(context, id);
            if (selector.isInstance(authorizable)) {
                result.add(selector.cast(authorizable));
            }
        }
        return result;
    }

    @NotNull
    protected Iterator<Authorizable> findAuthorizables(@NotNull final Context context,
                                                       @Nullable final Class<? extends Authorizable> selector,
                                                       @Nullable final String nameQueryPattern)
            throws RepositoryException {
        if (selector == null || !selector.equals(ServiceUser.class)) {
            UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final Query authorizableQuery = new Query() {
                    @Override
                    public <T> void build(final QueryBuilder<T> builder) {
                        builder.setCondition(builder
                                .nameMatches(StringUtils.isNotBlank(nameQueryPattern) ? nameQueryPattern : "%"));
                        builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                        if (selector != null) {
                            builder.setSelector(selector);
                        }
                    }
                };
                return userManager.findAuthorizables(authorizableQuery);
            }
        }
        return Collections.emptyIterator();
    }

    @NotNull
    protected List<ServiceUser> findServiceUsers(@NotNull final Context context,
                                                 @Nullable final Class<? extends Authorizable> selector,
                                                 @Nullable final String nameQueryPattern)
            throws RepositoryException {
        List<ServiceUser> serviceUsers = new ArrayList<>();
        if (selector == null || selector.equals(ServiceUser.class)) {
            Pattern namePattern = StringUtils.isNotBlank(nameQueryPattern)
                    ? Pattern.compile("^" + nameQueryPattern.replaceAll("%", ".*") + "$")
                    : null;
            List<Mapping> mappings = serviceUserMapper.getActiveMappings();
            for (Mapping mapping : mappings) {
                ServiceUser service = new ServiceUser(context, mapping);
                if (namePattern == null || namePattern.matcher(service.getID()).matches()) {
                    service.initialize(context);
                    serviceUsers.add(service);
                }
            }
        }
        return serviceUsers;
    }

    @Nullable
    protected ServiceUser getServiceUser(@NotNull final Context context, @NotNull final String id)
            throws RepositoryException {
        List<Mapping> mappings = serviceUserMapper.getActiveMappings();
        for (Mapping mapping : mappings) {
            ServiceUser service = new ServiceUser(context, mapping);
            if (id.equals(service.getID())) {
                return service;
            }
        }
        return null;
    }
}
