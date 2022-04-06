package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.SlingUrl;
import com.composum.sling.nodes.service.ServletRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.settings.SlingSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionManager;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.composum.sling.nodes.components.MergedModel.isMergedResource;

public interface Condition {

    String KEY_RESOURCE_TYPE = "resourceType";
    String KEY_PRIMARY_TYPE = "primaryType";
    String KEY_VERSIONABLE = "versionable";
    String KEY_MERGED = "merged";
    String KEY_ACL = "acl";
    String KEY_JCR = "jcr";
    String KEY_CLASS = "class";
    String KEY_SERVLET = "servlet";
    String KEY_HTTP = "http";
    String KEY_RUNMODE = "runmode";
    String KEY_RESOURCE = "resource";
    String KEY_RESTRICTIONS = "restrictions";

    /**
     * check the configured condition for the given resource
     */
    boolean accept(@NotNull BeanContext context, @NotNull Resource resource);

    abstract class Set implements Condition {

        protected final List<Condition> conditions = new ArrayList<>();

        protected Set(Condition... conditions) {
            this.conditions.addAll(Arrays.asList(conditions));
        }

        protected Set(String... rules) {
            if (rules != null) {
                for (String rule : rules) {
                    Condition condition = DEFAULT.getCondition(rule);
                    if (condition != null) {
                        conditions.add(condition);
                    }
                }
            }
        }

        protected Set() {
        }

        protected static Condition fromProperties(@NotNull final Set set, @NotNull final Map<String, Object> properties) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                Condition condition = DEFAULT.getCondition(entry.getKey(), entry.getValue());
                if (condition != null) {
                    set.conditions.add(condition);
                }
            }
            return set;
        }
    }

    class And extends Set {

        public And(Condition... conditions) {
            super(conditions);
        }

        public And(String... conditions) {
            super(conditions);
        }

        public And() {
            super();
        }

        @Nullable
        public static Condition fromResource(@Nullable final Resource resource) {
            return resource != null ? fromProperties(resource.getValueMap()) : null;
        }

        @NotNull
        public static Condition fromProperties(@NotNull final Map<String, Object> properties) {
            return fromProperties(new And(), properties);
        }

        @Override
        public boolean accept(@NotNull BeanContext context, @NotNull Resource resource) {
            for (Condition condition : conditions) {
                if (!condition.accept(context, resource)) {
                    return false;
                }
            }
            return true;
        }
    }

    class Or extends Set {

        public Or(Condition... conditions) {
            super(conditions);
        }

        public Or(String... conditions) {
            super(conditions);
        }

        public Or() {
            super();
        }

        @Nullable
        public static Condition fromResource(@Nullable final Resource resource) {
            return resource != null ? fromProperties(resource.getValueMap()) : null;
        }

        public static Condition fromProperties(@NotNull final Map<String, Object> properties) {
            return fromProperties(new Or(), properties);
        }

        @Override
        public boolean accept(@NotNull BeanContext context, @NotNull Resource resource) {
            for (Condition condition : conditions) {
                if (condition.accept(context, resource)) {
                    return true;
                }
            }
            return conditions.isEmpty();
        }
    }

    // implementations

    /**
     * check the avaiability of a servlet registered fo a given resource type
     */
    class RunmodePermission implements Condition {

        protected final List<String> alternatives;

        public RunmodePermission(@NotNull final String pattern) {
            alternatives = Arrays.asList(StringUtils.split(pattern, ","));
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            final SlingSettingsService settings = context.getService(SlingSettingsService.class);
            final java.util.Set<String> runmodes = settings.getRunModes();
            for (final String pattern : alternatives) {
                if ((!pattern.startsWith("!") && runmodes.contains(pattern))
                        || (pattern.startsWith("!") && !runmodes.contains(pattern.substring(1)))) {
                    return true;
                }
            }
            return alternatives.size() == 0;
        }
    }

    /**
     * check the avaiability of a servlet registered for a given resource type
     */
    class ServletPermission implements Condition {

        protected final String servletKey;

        public ServletPermission(@NotNull final String pattern) {
            servletKey = pattern;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            final ServletRegistry registry = context.getService(ServletRegistry.class);
            return registry.getServletData(servletKey) != null;
        }
    }

    /**
     * check the permissions of a given service key (feature)
     */
    class NodesRestrictions implements Condition {

        protected final ServiceRestrictions.Key key;
        protected final ServiceRestrictions.Permission permission;

        public NodesRestrictions(@NotNull final String pattern) {
            final String[] keyValue = StringUtils.split(pattern, "=", 2);
            key = new ServiceRestrictions.Key(keyValue[0]);
            ServiceRestrictions.Permission perm = null;
            if (keyValue.length > 1) {
                try {
                    perm = ServiceRestrictions.Permission.valueOf(keyValue[1]);
                } catch (IllegalArgumentException ignore) {
                    perm = ServiceRestrictions.Permission.none;
                }
            }
            this.permission = perm != null ? perm : ServiceRestrictions.Permission.write;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            final ServiceRestrictions restrictions = context.getService(ServiceRestrictions.class);
            return restrictions != null && restrictions.isPermissible(context.getRequest(), key, permission)
                    && restrictions.checkAuthorizables(context.getRequest(), restrictions.getRestrictions(key));
        }
    }

    /**
     * check the avaiability of a resource (readable)
     */
    class ResourcePermission implements Condition {

        protected final String path;

        public ResourcePermission(@NotNull final String pattern) {
            path = pattern;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            String absolutePath = path.startsWith("/") ? path
                    : context.getService(CoreConfiguration.class).getComposumBase() + path;
            return context.getResolver().getResource(absolutePath) != null;
        }
    }

    /**
     * check the availability of a class as a precondition for a console module
     */
    class ClassAvailability implements Condition {

        public static final Logger LOG = LoggerFactory.getLogger(ClassAvailability.class);

        protected final String pattern;

        public ClassAvailability(@NotNull final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            boolean classAvailable = false;
            try {
                context.getType(pattern);
                classAvailable = true;
            } catch (Exception ex) {
                LOG.warn("precondition check failed: " + ex.getMessage());
            }
            return classAvailable;
        }
    }

    /**
     * check the availability of an HTTP service
     */
    class HttpStatus implements Condition {

        public static final Logger LOG = LoggerFactory.getLogger(HttpStatus.class);

        protected final int expectedStatus;
        protected final String serviceUrl;

        public HttpStatus(@NotNull final String pattern) {
            final String[] parts = StringUtils.split(pattern, ":", 2);
            if (parts.length < 2) {
                this.expectedStatus = HttpURLConnection.HTTP_OK;
                this.serviceUrl = parts[0];
            } else {
                this.expectedStatus = Integer.parseInt(parts[0]);
                this.serviceUrl = parts[1];
            }
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            boolean serviceAvailable = false;
            if (StringUtils.isNotBlank(serviceUrl))
                try {
                    final SlingHttpServletRequest request = context.getRequest();
                    final String relativeUrl = new SlingUrl(request).fromUrl(serviceUrl).getUrl();
                    final String absoluteUrl = LinkUtil.getAbsoluteUrl(request, relativeUrl);
                    final HttpHead httpMethod = new HttpHead(absoluteUrl);
                    httpMethod.addHeader("Cookie", request.getHeader("Cookie"));
                    final HttpClient httpClient = HttpClientBuilder.create().build();
                    final int status = httpClient.execute(httpMethod).getStatusLine().getStatusCode();
                    serviceAvailable = (status == expectedStatus);
                } catch (Exception ex) {
                    LOG.warn("precondition check failed: " + ex.getMessage());
                }
            return serviceAvailable;
        }
    }

    /**
     * checks that the resource is a JCR resource
     */
    class JcrResource implements Condition {

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return !ResourceUtil.isSyntheticResource(resource) && resource.adaptTo(Node.class) != null;
        }
    }

    /**
     * checks the ability to manage ACLs at the resource
     */
    class CanHaveAcl extends JcrResource {
    }

    /**
     * checks the ability to manage versions at the resource
     */
    class Versionable extends JcrResource {

        public static final Logger LOG = LoggerFactory.getLogger(Versionable.class);

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            if (super.accept(context, resource)) {
                final ResourceResolver resolver = resource.getResourceResolver();
                final Session session = resolver.adaptTo(Session.class);
                if (session != null) {
                    try {
                        final VersionManager versionManager = session.getWorkspace().getVersionManager();
                        versionManager.getBaseVersion(resource.getPath());
                        return true;
                    } catch (UnsupportedRepositoryOperationException ignore) {
                        // OK - node is simply not versionable.
                    } catch (RepositoryException ex) {
                        LOG.error("unexpected exception checking '" + resource.getPath() + "'", ex);
                    }
                }
            }
            return false;
        }
    }

    /**
     * checks that the resources primary type matches the pattern
     */
    class PrimaryType implements Condition {

        protected final String pattern;

        public PrimaryType(@NotNull final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return StringUtils.isBlank(pattern)
                    || pattern.equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class));
        }
    }

    /**
     * checks that the resources primary type matches the pattern
     */
    class ResourceType implements Condition {

        protected final String pattern;

        public ResourceType(@NotNull final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return StringUtils.isBlank(pattern) || resource.isResourceType(pattern);
        }
    }

    /**
     * checks that the resources primary type matches the pattern
     */
    class MergedResource implements Condition {

        @Override
        public boolean accept(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return isMergedResource(resource);
        }
    }

    Condition MERGED_RESOURCE = new MergedResource();
    Condition VERSIONABLE = new Versionable();
    Condition CAN_HAVE_ACL = new CanHaveAcl();
    Condition JCR_RESOURCE = new JcrResource();

    // factory

    interface Factory {

        @Nullable
        Condition getCondition(@NotNull String key, @Nullable Object pattern);
    }

    class Options implements Factory {

        private final Map<String, Factory> factorySet = new HashMap<>();

        @Override
        @Nullable
        public Condition getCondition(@NotNull final String key, @Nullable final Object pattern) {
            final Factory factory = factorySet.get(key);
            return factory != null ? factory.getCondition(key.toLowerCase(), pattern) : null;
        }

        @Nullable
        public Condition getCondition(@Nullable final String rule) {
            if (StringUtils.isNotBlank(rule)) {
                final String[] parts = StringUtils.split(rule, ":", 2);
                return getCondition(parts[0], parts.length > 1 ? parts[1] : null);
            }
            return null;
        }

        @NotNull
        public Options addFactory(@NotNull final String key, @NotNull final Factory factory) {
            factorySet.put(key, factory);
            return this;
        }
    }

    Options DEFAULT = new Options()
            .addFactory(KEY_RESOURCE_TYPE, (key, pattern) ->
                    pattern instanceof String ? new ResourceType((String) pattern) : null)
            .addFactory(KEY_PRIMARY_TYPE, (key, pattern) ->
                    pattern instanceof String ? new PrimaryType((String) pattern) : null)
            .addFactory(KEY_MERGED, (key, pattern) -> MERGED_RESOURCE)
            .addFactory(KEY_VERSIONABLE, (key, pattern) -> VERSIONABLE)
            .addFactory(KEY_ACL, (key, pattern) -> CAN_HAVE_ACL)
            .addFactory(KEY_JCR, (key, pattern) -> JCR_RESOURCE)
            .addFactory(KEY_RESTRICTIONS, (key, pattern) ->
                    pattern instanceof String ? new NodesRestrictions((String) pattern) : null)
            .addFactory(KEY_RESOURCE, (key, pattern) ->
                    pattern instanceof String ? new ResourcePermission((String) pattern) : null)
            .addFactory(KEY_RUNMODE, (key, pattern) ->
                    pattern instanceof String ? new RunmodePermission((String) pattern) : null)
            .addFactory(KEY_SERVLET, (key, pattern) ->
                    pattern instanceof String ? new ServletPermission((String) pattern) : null)
            .addFactory(KEY_CLASS, (key, pattern) ->
                    pattern instanceof String ? new ClassAvailability((String) pattern) : null)
            .addFactory(KEY_HTTP, (key, pattern) ->
                    pattern instanceof String ? new HttpStatus((String) pattern) : null);
}
