package com.composum.sling.core.usermanagement.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface Authorizables {

    Map<String, Class<? extends Authorizable>> SELECTORS = new HashMap<String, Class<? extends Authorizable>>() {{
        put("user", User.class);
        put("group", Group.class);
    }};

    interface Filter {

        boolean accept(Authorizable authorizable) throws RepositoryException;

        class Path implements Filter {

            protected final Pattern pattern;

            public Path(@NotNull final String pattern) {
                this(Pattern.compile(pattern));
            }

            public Path(@NotNull final Pattern pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean accept(Authorizable authorizable) {
                try {
                    return pattern.matcher(authorizable.getPath()).find();
                } catch (RepositoryException ignore) {
                }
                return false;
            }
        }
    }

    @Nullable
    static UserManager getUserManager(@NotNull final ResourceResolver resolver)
            throws RepositoryException {
        final Session session = resolver.adaptTo(Session.class);
        return session instanceof JackrabbitSession ? ((JackrabbitSession) session).getUserManager() : null;
    }

    @Nullable
    static Class<? extends Authorizable> selector(@Nullable final String key) {
        return StringUtils.isNotBlank(key) ? SELECTORS.get(key.toLowerCase()) : null;
    }

    @NotNull
    static Iterator<Authorizable> findAuthorizables(@NotNull final ResourceResolver resolver,
                                                    @Nullable final Class<? extends Authorizable> selector,
                                                    @Nullable final String nameQueryPattern)
            throws RepositoryException {
        return findAuthorizables(getUserManager(resolver), selector, nameQueryPattern);
    }

    @NotNull
    static Iterator<Authorizable> findAuthorizables(@Nullable final UserManager userManager,
                                                    @Nullable final Class<? extends Authorizable> selector,
                                                    @Nullable final String nameQueryPattern)
            throws RepositoryException {
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
        return Collections.emptyIterator();
    }

    @NotNull
    static Set<Authorizable> findAuthorizables(@Nullable final UserManager userManager,
                                               @Nullable final Class<? extends Authorizable> selector,
                                               @Nullable final String nameQueryPattern,
                                               @Nullable final Filter filter)
            throws RepositoryException {
        Set<Authorizable> result = new HashSet<>();
        if (userManager != null) {
            Iterator<Authorizable> iterator = findAuthorizables(userManager, selector, nameQueryPattern);
            while (iterator.hasNext()) {
                Authorizable authorizable = iterator.next();
                if (filter == null || filter.accept(authorizable)) {
                    result.add(authorizable);
                }
            }
        }
        return result;
    }

    @NotNull
    static <T extends Authorizable> Collection<T> loadAuthorizables(@Nullable final UserManager userManager,
                                                                    @NotNull final Class<T> selector,
                                                                    @NotNull final Set<String> idSet)
            throws RepositoryException {
        List<T> result = new ArrayList<>();
        if (userManager != null) {
            for (String id : idSet) {
                Authorizable authorizable = userManager.getAuthorizable(id);
                if (selector.isInstance(authorizable)) {
                    result.add(selector.cast(authorizable));
                }
            }
        }
        return result;
    }
}
