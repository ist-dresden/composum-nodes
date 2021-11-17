package com.composum.sling.core;

import com.composum.sling.core.service.ServiceRestrictions;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Locale;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for {@link BeanContext}.
 *
 * @author Hans-Peter Stoerr
 */
public class BeanContextTest {

    public static final ServiceRestrictions.Key SERVICE_KEY = new ServiceRestrictions.Key("bean/context/test");

    final ResourceResolver resolver = createMock(ResourceResolver.class);
    final Resource resource = new SyntheticResource(resolver, "/whatever", "type");
    SlingHttpServletRequest request = new SlingHttpServletRequestWrapper(createMock(SlingHttpServletRequest
            .class)) {
        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return resolver;
        }

        @Override public Object getAttribute(String name) {
            if ("locale".equals(name)) return Locale.GERMANY;
            return super.getAttribute(name);
        }
    };
    SlingHttpServletResponse response = new SlingHttpServletResponseWrapper(createMock(SlingHttpServletResponse
            .class));
    ServletContext servletContext = createMock(ServletContext.class);

    BeanContext context = new BeanContext.Servlet(servletContext, createMock
            (BundleContext.class), request, response).withLocale(Locale.GERMANY);

    @Test
    public void adaptTo() {
        BeanContext.Servlet context = new BeanContext.Servlet(servletContext, createMock
                (BundleContext.class), request, response);

        assertSame(context, context.adaptTo(BeanContext.class));
        assertSame(context, context.adaptTo(context.getClass()));

        assertSame(request, context.adaptTo(ServletRequest.class));
        assertSame(request, context.adaptTo(SlingHttpServletRequest.class));
        assertSame(request, context.adaptTo(request.getClass()));

        assertSame(response, context.adaptTo(ServletResponse.class));
        assertSame(response, context.adaptTo(SlingHttpServletResponse.class));
        assertSame(response, context.adaptTo(response.getClass()));

        assertSame(resource, context.adaptTo(Resource.class));
        assertSame(resource, context.adaptTo(resource.getClass()));

        assertSame(resolver, context.adaptTo(ResourceResolver.class));
        assertSame(resolver, context.adaptTo(resolver.getClass()));

        assertSame(servletContext, context.adaptTo(ServletContext.class));
        assertSame(servletContext, context.adaptTo(servletContext.getClass()));

        assertNull(context.adaptTo(Object.class));
    }

    @Test
    public void copy() {
        Resource freshResource = new SyntheticResource(null, "/fresh", "other");
        BeanContext copy = context.withResource(freshResource);
        assertSame(copy.getRequest(), context.getRequest());
        assertSame(copy.getResponse(), context.getResponse());
        assertSame(copy.getResolver(), context.getResolver());
        assertSame(copy.getLocale(), context.getLocale());
        assertSame(copy.getResource(), freshResource);
    }

    @Test
    public void instantiateSlingBean() {
        assertNull(context.adaptTo(SlingBean.class));
        assertNull(context.adaptTo(AbstractSlingBean.class));
        TestingSlingBean testBean = context.adaptTo(TestingSlingBean.class);
        assertNotNull(testBean);
        assertTrue(testBean.initialized);
    }

    public static class TestingSlingBean extends AbstractSlingBean {
        boolean initialized;

        @Override
        public void initialize(BeanContext context) {
            initialized = true;
        }

        @NotNull
        public ServiceRestrictions.Key getServiceKey() {
            return SERVICE_KEY;
        }
    }
}
