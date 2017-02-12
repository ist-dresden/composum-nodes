package com.composum.sling.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * the interface for the different scripting contexts (JSP, Groovy, ...)
 * and the basic implementations for this interface
 */
public interface BeanContext {

    //
    // the attribute names of the main context attributes
    //
    String ATTR_RESOURCE = "resource";
    String ATTR_RESOLVER = "resourceResolver";
    String ATTR_REQUEST = "request";
    String ATTR_RESPONSE = "response";

    /**
     * the Scope enumeration according to the JSPs PageContext
     */
    enum Scope {

        page(PageContext.PAGE_SCOPE),
        request(PageContext.REQUEST_SCOPE),
        session(PageContext.SESSION_SCOPE),
        application(PageContext.APPLICATION_SCOPE);

        public final int value;

        Scope(int value) {
            this.value = value;
        }
    }

    /**
     * Returns the resource declared in the context.
     */
    Resource getResource();

    /**
     * Returns the resolver declared in the context.
     */
    ResourceResolver getResolver();

    /**
     * Returns the request declared in the context.
     */
    SlingHttpServletRequest getRequest();

    /**
     * Returns the response declared in the context.
     */
    SlingHttpServletResponse getResponse();

    /**
     * Returns the locale declared determined using the context.
     */
    Locale getLocale();

    /**
     * Returns an attribute value from the context.
     */
    <T> T getAttribute(String name, Class<T> T);

    /**
     * Stores an attribute in the context in th given scope.
     */
    void setAttribute(String name, Object value, Scope scope);

    /**
     * retrieves a service implementation using the 'sling' script helper
     */
    <T> T getService(Class<T> type);

    /**
     * retrieves a set of services appropriate to the filter
     */
    <T> T[] getServices(Class<T> serviceType, String filter) throws InvalidSyntaxException;

    /**
     * retrieves a class using the Slings DynamicClassLoaderManager implementation
     */
    Class<?> getType(String className) throws ClassNotFoundException;

    /**
     * the base class of the context interface with general methods
     */
    abstract class AbstractContext implements BeanContext {

        protected abstract <T> T retrieveService(Class<T> type);

        public <T> T getService(Class<T> type) {
            String typeKey = type.getName();
            T service = getAttribute(typeKey, type);
            if (service == null) {
                service = retrieveService(type);
                setAttribute(typeKey, service, Scope.request);
            }
            return service;
        }

        public Class<?> getType(String className) throws ClassNotFoundException {
            Class<?> type = null;
            // use Sling DynamicClassLoader
            DynamicClassLoaderManager dclm = getService(DynamicClassLoaderManager.class);
            if (dclm != null) {
                type = dclm.getDynamicClassLoader().loadClass(className);
            }
            // fallback to default ClassLoader
            if (type == null) {
                type = Class.forName(className);
            }
            return type;
        }
    }

    /**
     * the enhancement of the base for scripting contexts
     */
    abstract class AbstractScriptContext extends AbstractContext {

        protected SlingBindings slingBindings;
        protected SlingScriptHelper scriptHelper;

        public <T> T retrieveService(Class<T> type) {
            return getScriptHelper().getService(type);
        }

        public <T> T[] getServices(Class<T> serviceType, String filter) {
            return getScriptHelper().getServices(serviceType, filter);
        }

        public SlingScriptHelper getScriptHelper() {
            if (scriptHelper == null) {
                scriptHelper = getSlingBindings().getSling();
            }
            return scriptHelper;
        }

        public SlingBindings getSlingBindings() {
            if (slingBindings == null) {
                slingBindings = getAttribute(SlingBindings.class.getName(), SlingBindings.class);
            }
            return slingBindings;
        }
    }

    /**
     * a Map based implementation of the context interface (e.g. for a Groovy script)
     */
    class Map extends AbstractScriptContext {

        private java.util.Map<String, Object> pageScopeMap;
        private java.util.Map<String, Object> requestScopeMap;
        private java.util.Map<String, Object> sessionScopeMap;

        private transient SlingHttpServletRequest request;
        private transient Resource resource;
        protected transient ResourceResolver resolver;

        public Map() {
            this(new HashMap<String, Object>());
        }

        public Map(java.util.Map<String, Object> pageScopeMap) {
            this(pageScopeMap, new HashMap<String, Object>());
        }

        public Map(java.util.Map<String, Object> pageScopeMap,
                   java.util.Map<String, Object> requestScopeMap) {
            this(pageScopeMap, requestScopeMap, new HashMap<String, Object>());
        }

        public Map(java.util.Map<String, Object> pageScopeMap, SlingHttpServletRequest request) {
            this(pageScopeMap, null, new HashMap<String, Object>());
            this.request = request;
        }

        public Map(java.util.Map<String, Object> pageScopeMap,
                   java.util.Map<String, Object> requestScopeMap,
                   java.util.Map<String, Object> sessionScopeMap) {
            this.pageScopeMap = pageScopeMap;
            this.requestScopeMap = requestScopeMap;
            this.sessionScopeMap = sessionScopeMap;
        }

        @Override
        public Resource getResource() {
            if (resource == null) {
                resource = getAttribute(ATTR_RESOURCE, Resource.class);
            }
            return resource;
        }

        @Override
        public ResourceResolver getResolver() {
            if (resolver == null) {
                resolver = getAttribute(ATTR_RESOLVER, ResourceResolver.class);
            }
            return resolver;
        }

        @Override
        public SlingHttpServletRequest getRequest() {
            return getAttribute(ATTR_REQUEST, SlingHttpServletRequest.class);
        }

        @Override
        public SlingHttpServletResponse getResponse() {
            return getAttribute(ATTR_RESPONSE, SlingHttpServletResponse.class);
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public <T> T getAttribute(String name, Class<T> T) {
            Object attribute = null;
            if (StringUtils.isNotBlank(name)) {
                attribute = pageScopeMap.get(name);
                if (attribute == null) {
                    if (requestScopeMap != null) {
                        attribute = this.requestScopeMap.get(name);
                        if (attribute == null) {
                            attribute = this.sessionScopeMap.get(name);
                        }
                    } else {
                        SlingHttpServletRequest request = getRequest();
                        if (request != null) {
                            attribute = request.getAttribute(name);
                            if (attribute == null) {
                                HttpSession session = request.getSession();
                                if (session != null) {
                                    attribute = session.getAttribute(name);
                                } else {
                                    attribute = this.sessionScopeMap.get(name);
                                }
                            }
                        }
                    }
                }
            }
            return (T) attribute;
        }

        @Override
        public void setAttribute(String name, Object value, Scope scope) {
            if (scope == Scope.page) {
                this.pageScopeMap.put(name, value);
            } else {
                SlingHttpServletRequest request = getRequest();
                if (request != null) {
                    if (scope == Scope.request) {
                        request.setAttribute(name, value);
                    } else {
                        HttpSession session = request.getSession();
                        if (session != null) {
                            // session and application scope stored in the session
                            session.setAttribute(name, value);
                        } else {
                            // fallback to local map id no session found
                            this.sessionScopeMap.put(name, value);
                        }
                    }
                } else {
                    if (scope == Scope.request) {
                        this.requestScopeMap.put(name, value);
                    } else {
                        this.sessionScopeMap.put(name, value);
                    }
                }
            }
        }
    }

    /**
     * a Map based implementation for a background service or a job execution
     */
    class Service extends Map {

        public Service(ResourceResolver resolver) {
            setAttribute(ATTR_RESOLVER, this.resolver = resolver, Scope.request);
        }
    }

    /**
     * a JSP PageContext based implementation of the context interface
     */
    class Page extends AbstractScriptContext {

        private PageContext pageContext;

        private transient Resource resource;
        private transient ResourceResolver resolver;

        public Page(PageContext pageContext) {
            this.pageContext = pageContext;
        }

        public PageContext getPageContext() {
            return pageContext;
        }

        @Override
        public Resource getResource() {
            if (resource == null) {
                resource = getAttribute(ATTR_RESOURCE, Resource.class);
            }
            return resource;
        }

        @Override
        public ResourceResolver getResolver() {
            if (resolver == null) {
                resolver = getRequest().getResourceResolver();
            }
            return resolver;
        }

        @Override
        public SlingHttpServletRequest getRequest() {
            return (SlingHttpServletRequest) this.pageContext.getRequest();
        }

        @Override
        public SlingHttpServletResponse getResponse() {
            return (SlingHttpServletResponse) this.pageContext.getResponse();
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public <T> T getAttribute(String name, Class<T> T) {
            Object attribute = null;
            if (StringUtils.isNotBlank(name)) {
                attribute = this.pageContext.findAttribute(name);
            }
            return (T) attribute;
        }

        @Override
        public void setAttribute(String name, Object value, Scope scope) {
            pageContext.setAttribute(name, value, scope.value);
        }
    }

    /**
     * a servlet API based implementation of the context interface for Beans in a Servlet context
     */
    class Servlet extends AbstractContext {

        private ServletContext servletContext;
        protected BundleContext bundleContext;
        private SlingHttpServletRequest request;
        private SlingHttpServletResponse response;

        private transient Resource resource;
        private transient ResourceResolver resolver;

        public Servlet(ServletContext servletContext, BundleContext bundleContext,
                       SlingHttpServletRequest request, SlingHttpServletResponse response) {
            this.servletContext = servletContext;
            this.bundleContext = bundleContext;
            this.request = request;
            this.response = response;
        }

        @Override
        public Resource getResource() {
            if (resource == null) {
                resource = this.request.getResource();
            }
            return resource;
        }

        @Override
        public ResourceResolver getResolver() {
            if (resolver == null) {
                resolver = getRequest().getResourceResolver();
            }
            return resolver;
        }

        @Override
        public SlingHttpServletRequest getRequest() {
            return this.request;
        }

        @Override
        public SlingHttpServletResponse getResponse() {
            return this.response;
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public <T> T getAttribute(String name, Class<T> T) {
            T attribute = null;
            if (StringUtils.isNotBlank(name)) {
                attribute = (T) this.request.getAttribute(name);
                if (attribute == null) {
                    HttpSession session = this.request.getSession();
                    if (session != null) {
                        attribute = (T) session.getAttribute(name);
                    }
                    if (attribute == null) {
                        if (this.servletContext != null) {
                            attribute = (T) this.servletContext.getAttribute(name);
                        }
                    }
                }
            }
            return attribute;
        }

        @Override
        public void setAttribute(String name, Object value, Scope scope) {
            switch (scope) {
                case application:
                    if (this.servletContext != null) {
                        this.servletContext.setAttribute(name, value);
                        break;
                    }
                    // fallback to session if no servlet context present
                case session:
                    HttpSession session = this.request.getSession();
                    if (session != null) {
                        session.setAttribute(name, value);
                        break;
                    }
                    // fallback to request if no session present
                case page:
                    // use request for all attributes in the page scope
                case request:
                    // storing in request scope is the default
                default:
                    request.setAttribute(name, value);
                    break;
            }
        }

        public <T> T retrieveService(Class<T> type) {
            ServiceReference<T> reference = bundleContext.getServiceReference(type);
            return bundleContext.getService(reference);
        }

        public <T> T[] getServices(Class<T> type, String filter) throws InvalidSyntaxException {
            List<T> services = new ArrayList<>();
            Collection<ServiceReference<T>> references;
            references = bundleContext.getServiceReferences(type, filter);
            for (ServiceReference<T> reference : references) {
                services.add(bundleContext.getService(reference));
            }
            return (T[]) services.toArray();
        }
    }
}
