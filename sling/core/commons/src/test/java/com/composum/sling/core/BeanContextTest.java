package com.composum.sling.core;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** Some tests for {@link BeanContext}. */
public class BeanContextTest {

    @Test
    public void adaptTo() {
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
        };
        SlingHttpServletResponse response = new SlingHttpServletResponseWrapper(createMock(SlingHttpServletResponse
                .class));
        ServletContext servletContext = createMock(ServletContext.class);

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

}
