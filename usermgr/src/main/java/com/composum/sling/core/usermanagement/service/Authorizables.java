package com.composum.sling.core.usermanagement.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@ProviderType
public interface Authorizables {

    Map<String, Class<? extends Authorizable>> SELECTORS = new HashMap<String, Class<? extends Authorizable>>() {{
        put("user", User.class);
        put("group", Group.class);
        put("service", Service.class);
    }};

    static @Nullable Class<? extends Authorizable> selector(@Nullable final String key) {
        return StringUtils.isNotBlank(key) ? SELECTORS.get(key.toLowerCase()) : null;
    }

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

    class Context {

        private static final Logger LOG = LoggerFactory.getLogger(Authorizables.class);

        protected final Authorizables service;
        protected final SlingHttpServletRequest request;
        protected final SlingHttpServletResponse response;
        protected final ResourceResolver resolver;

        private transient UserManager userManager;

        public Context(@NotNull final Authorizables service,
                       @NotNull final SlingHttpServletRequest request,
                       @NotNull final SlingHttpServletResponse response) {
            this.service = service;
            this.request = request;
            this.response = response;
            resolver = request.getResourceResolver();
        }

        public @NotNull Authorizables getService() {
            return service;
        }

        public @NotNull ResourceResolver getResolver() {
            return resolver;
        }

        public @NotNull SlingHttpServletRequest getRequest() {
            return request;
        }

        public @NotNull SlingHttpServletResponse getResponse() {
            return response;
        }

        public @Nullable UserManager getUserManager() {
            if (userManager == null) {
                try {
                    final Session session = resolver.adaptTo(Session.class);
                    userManager = session instanceof JackrabbitSession ? ((JackrabbitSession) session).getUserManager() : null;
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
            return userManager;
        }
    }

    @Nullable Authorizable getAuthorizable(@NotNull final Context context, @NotNull final String id)
            throws RepositoryException;

    @NotNull Set<Authorizable> findAuthorizables(@NotNull Context context,
                                                 @Nullable Class<? extends Authorizable> selector,
                                                 @Nullable String nameQueryPattern,
                                                 @Nullable Filter filter)
            throws RepositoryException;

    @NotNull <T extends Authorizable> Collection<T> loadAuthorizables(@NotNull Context context,
                                                                      @NotNull Class<T> selector,
                                                                      @NotNull Set<String> idSet)
            throws RepositoryException;
}
