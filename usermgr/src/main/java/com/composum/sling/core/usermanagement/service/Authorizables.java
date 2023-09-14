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
        put("service", ServiceUser.class);
    }};

    static @Nullable Class<? extends Authorizable> selector(@Nullable final String key) {
        return StringUtils.isNotBlank(key) ? SELECTORS.get(key.toLowerCase()) : null;
    }

    interface Filter {

        boolean accept(Authorizable authorizable) throws RepositoryException;

        class Path implements Filter {

            protected final Pattern pattern;

            public Path(@NotNull final String pattern) {
                this(createPattern(pattern));
            }

            public Path(@NotNull final Pattern pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean accept(Authorizable authorizable) {
                try {
                    return pattern.matcher(authorizable.getPath()).matches();
                } catch (RepositoryException ignore) {
                }
                return false;
            }
        }

        static Pattern createPattern(@NotNull final String pattern) {
            return Pattern.compile(pattern
                    .replaceAll("\\*", ".*")
                    .replaceAll("\\.\\.\\*", ".*")
                    .replaceAll("%", ".*"));
        }
    }

    class Context {

        private static final Logger LOG = LoggerFactory.getLogger(Authorizables.class);

        protected final Authorizables service;
        protected final SlingHttpServletRequest request;
        protected final SlingHttpServletResponse response;
        protected final ResourceResolver resolver;
        protected final Session session;

        private transient UserManager userManager;

        // the map to cache authorizables during requst execution
        protected final Map<String, Authorizable> authorizables = new HashMap<>();

        public Context(@NotNull final Authorizables service,
                       @NotNull final SlingHttpServletRequest request,
                       @NotNull final SlingHttpServletResponse response) {
            this.service = service;
            this.request = request;
            this.response = response;
            resolver = request.getResourceResolver();
            session = resolver.adaptTo(Session.class);
        }

        public void commit() throws RepositoryException {
            if (session != null) {
                session.save();
            }
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

        public Session getSession() {
            return session;
        }

        public @Nullable UserManager getUserManager() {
            if (userManager == null && session != null) {
                try {
                    userManager = session instanceof JackrabbitSession ? ((JackrabbitSession) session).getUserManager() : null;
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
            return userManager;
        }

        public Map<String, Authorizable> getAuthorizables() {
            return authorizables;
        }
    }

    @Nullable Authorizable getAuthorizable(@NotNull final Context context, @NotNull final String id)
            throws RepositoryException;

    @NotNull Set<? extends Authorizable> findAuthorizables(@NotNull Context context,
                                                 @Nullable Class<? extends Authorizable> selector,
                                                 @Nullable String nameQueryPattern,
                                                 @Nullable Filter filter)
            throws RepositoryException;

    @NotNull <T extends Authorizable> Collection<T> loadAuthorizables(@NotNull Context context,
                                                                      @NotNull Class<T> selector,
                                                                      @NotNull Set<String> idSet)
            throws RepositoryException;
}
