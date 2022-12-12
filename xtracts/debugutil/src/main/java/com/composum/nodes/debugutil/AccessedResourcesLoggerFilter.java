package com.composum.nodes.debugutil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries to log most resources that are accessed during a request.
 */
@Component(
        service = {Filter.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Resource Access Logging Filter",
                "sling.filter.scope=REQUEST",
                "service.ranking:Integer=" + 4910
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = AccessedResourcesLoggerFilter.Configuration.class)
public class AccessedResourcesLoggerFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(AccessedResourcesLoggerFilter.class);

    protected static final Pattern INTERESTING_PATHS = Pattern.compile("^/(libs|apps|content|conf|var|public|preview).*");

    @Reference
    private ServletResolver servletResolver;

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain) throws IOException, ServletException {
        if (config != null && config.enabled() && LOG.isInfoEnabled()) {
            SlingHttpServletRequest request = determineRequestImpl(rawRequest);
            ResourceResolver resourceResolver = request.getResourceResolver();
            LoggingResourceResolver loggingResourceResolver = new LoggingResourceResolver(resourceResolver);
            try {
                switchResolver(request, loggingResourceResolver);
                chain.doFilter(rawRequest, rawResponse);
            } finally {
                String accesses = loggingResourceResolver.resourceAccesses();
                if (StringUtils.isNotBlank(accesses)) {
                    LOG.info("Accessed resources for {}:\n{}", request.getRequestURI(), accesses);
                }
                switchResolver(request, resourceResolver);
            }
        } else {
            chain.doFilter(rawRequest, rawResponse);
        }
    }

    private SlingHttpServletRequest determineRequestImpl(ServletRequest request) {
        while (request instanceof SlingHttpServletRequestWrapper) {
            request = ((SlingHttpServletRequestWrapper) request).getSlingRequest();
        }
        return request instanceof SlingHttpServletRequest
                ? (SlingHttpServletRequest) request
                : null;
    }

    protected void switchResolver(SlingHttpServletRequest slingRequestImpl, ResourceResolver resourceResolver) throws ServletException {
        try {
            // if we try to cache this method object, we had to synchronize it - so leave it for now
            // org.apache.sling.engine.impl.SlingHttpServletRequestImpl.getRequestData()
            final Method getRequestData = slingRequestImpl.getClass().getMethod("getRequestData");
            final Object requestData = getRequestData.invoke(slingRequestImpl);

            // if we try to cache this method object, we had to synchronize it - so leave it for now
            // org.apache.sling.engine.impl.request.RequestData.initResource(ResourceResolver resourceResolver)
            final Method initResource = requestData.getClass()
                    .getMethod("initResource", ResourceResolver.class);
            final Object resource = initResource.invoke(requestData, resourceResolver);

            // if we try to cache this method object, we had to synchronize it - so leave it for now
            // org.apache.sling.engine.impl.request.RequestData.initServlet(Resource resource, ServletResolver sr)
            final Method initServlet = requestData.getClass()
                    .getMethod("initServlet", Resource.class, ServletResolver.class);
            initServlet.invoke(requestData, resource, servletResolver);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOG.error("can not change ResourceResolver: ", e);
            throw new ServletException("Error switching ResourceResolver");
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    protected Configuration config;

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Resource Access Logging Filter",
            description = "Tries to log most resources that are accessed during a request. This is logged at INFO level for com.composum.nodes.debugutil.AccessedResourcesLoggerFilter . We log only the leaves - accesses to parent resources are omitted to reduce the amount of logging. (They are implicitly accessed, anyway.)"
    )
    public @interface Configuration {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    private static class LoggingResourceResolver extends ResourceResolverWrapper {

        private final Set<String> paths = new TreeSet<>();

        public LoggingResourceResolver(ResourceResolver resourceResolver) {
            super(resourceResolver);
        }

        public String resourceAccesses() {
            for (String path : new ArrayList<String>(paths)) {
                String parent = ResourceUtil.getParent(path);
                paths.remove(parent);
            }
            return paths.stream().map(p -> "    " + p).collect(Collectors.joining("\n"));
        }

        protected Resource log(Resource resource) {
            if (resource != null && INTERESTING_PATHS.matcher(resource.getPath()).matches()) {
                paths.add(resource.getPath());
            }
            return resource;
        }

        protected Iterator<Resource> log(Iterator<Resource> listChildren) {
            if (listChildren != null) {
                listChildren = IteratorUtils.transformedIterator(listChildren, this::log);
            }
            return listChildren;
        }

        @Override
        public Resource resolve(HttpServletRequest request, String absPath) {
            return log(super.resolve(request, absPath));
        }

        @Override
        public Resource resolve(String absPath) {
            return log(super.resolve(absPath));
        }

        @Override
        public Resource resolve(HttpServletRequest request) {
            return log(super.resolve(request));
        }

        @Override
        public Resource getResource(String path) {
            return log(super.getResource(path));
        }

        @Override
        public Resource getResource(Resource base, String path) {
            return log(super.getResource(base, path));
        }

        @Override
        public Iterator<Resource> listChildren(Resource parent) {
            return log(super.listChildren(parent));
        }

        @Override
        public Resource getParent(Resource child) {
            return log(super.getParent(child));
        }

        @Override
        public Iterable<Resource> getChildren(Resource parent) {
            return IterableUtils.transformedIterable(super.getChildren(parent), this::log);
        }

        @Override
        public Iterator<Resource> findResources(String query, String language) {
            return log(super.findResources(query, language));
        }

        @Override
        public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
            return new LoggingResourceResolver(super.clone(authenticationInfo));
        }

        @Override
        public void delete(Resource resource) throws PersistenceException {
            LOG.info("Deleted: {}", resource != null ? resource.getPath() : null);
            super.delete(log(resource));
        }

        @Override
        public Resource create(Resource parent, String name, Map<String, Object> properties) throws PersistenceException {
            LOG.info("Created: {}/{}", parent != null ? parent.getPath() : null, name);
            return log(super.create(parent, name, properties));
        }

        @Override
        public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
            LOG.info("Moved: {} -> {}", srcAbsPath, destAbsPath);
            return log(super.move(srcAbsPath, destAbsPath));
        }

    }
}
