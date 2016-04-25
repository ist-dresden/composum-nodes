package com.composum.sling.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.util.HashMap;
import java.util.Locale;

/**
 * the interface for the different scripting contexts (JSP, Groovy, ...)
 * and the basic implementations for this interface
 */
public interface BeanContext {

    //
    // the attribute names of the main context attributes
    //
    final String ATTR_RESOURCE = "resource";
    final String ATTR_REQUEST = "request";
    final String ATTR_RESPONSE = "response";

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
     * a Map based implementation of the context interface (e.g. for a Groovy script)
     */
    class Map implements BeanContext {

        private java.util.Map<String, Object> pageScopeMap;
        private java.util.Map<String, Object> requestScopeMap;
        private java.util.Map<String, Object> sessionScopeMap;

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

        public Map(java.util.Map<String, Object> pageScopeMap,
                   java.util.Map<String, Object> requestScopeMap,
                   java.util.Map<String, Object> sessionScopeMap) {
            this.pageScopeMap = pageScopeMap;
            this.requestScopeMap = requestScopeMap;
            this.sessionScopeMap = sessionScopeMap;
        }

        @Override
        public Resource getResource() {
            return getAttribute(ATTR_RESOURCE, Resource.class);
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
                    } else {
                        attribute = this.requestScopeMap.get(name);
                        if (attribute == null) {
                            attribute = this.sessionScopeMap.get(name);
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
     * a JSP PageContext based implementation of the context interface
     */
    class Page implements BeanContext {

        private PageContext pageContext;

        public Page(PageContext pageContext) {
            this.pageContext = pageContext;
        }

        public PageContext getPageContext() {
            return pageContext;
        }

        @Override
        public Resource getResource() {
            return getAttribute(ATTR_RESOURCE, Resource.class);
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
    class Servlet implements BeanContext {

        private ServletContext servletContext;
        private SlingHttpServletRequest request;
        private SlingHttpServletResponse response;

        public Servlet(ServletContext servletContext,
                       SlingHttpServletRequest request, SlingHttpServletResponse response) {
            this.servletContext = servletContext;
            this.request = request;
            this.response = response;
        }

        public ServletContext getServletContext() {
            return this.servletContext;
        }

        @Override
        public Resource getResource() {
            return this.request.getResource();
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
    }
}
