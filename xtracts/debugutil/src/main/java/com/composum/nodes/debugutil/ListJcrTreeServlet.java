package com.composum.nodes.debugutil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet enables listing the resources in a JCR subtree - mainly for the publish instance, where there
 * is no CRX. It requires a resource in a published page that has the sling:resourceType cpm/nodes/debug/listtree ,
 * and then lists the suffix when a GET is triggered on that resource.
 * <p>
 * CAUTION: not suitable for production, only for internal testing systems!
 * It does, however, require a configuration, so it won't be active on systems where it is not configured.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil List Jcr Tree Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=cpm/nodes/debug/listtree",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=html"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ListJcrTreeServlet.Configuration.class)
public class ListJcrTreeServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ListJcrTreeServlet.class);

    private transient Configuration config;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.debug("Called on {}", request.getResource().getPath());
        if (config == null || !config.enabled()) {
            super.doGet(request, response);
            return;
        }

        response.setContentType("text/plain");
        try (PrintWriter writer = response.getWriter()) {
            try {
                RequestPathInfo pathInfo = request.getRequestPathInfo();
                String resourcePath = Objects.requireNonNull(pathInfo.getSuffix(), "Suffix required to determine resource.");

                Resource resource = request.getResourceResolver().getResource(resourcePath);
                if (resource == null) {
                    throw new IllegalArgumentException("Not found: " + resourcePath);
                }

                listTree(writer, resource);
            } catch (Exception e) {
                writer.println("Exception: " + e);
                e.printStackTrace(writer);
            }
        }
    }

    /**
     * Just lists the resources of a subtree.
     */
    private void listTree(PrintWriter writer, Resource resource) throws ServletException {
        AtomicInteger maxcount = new AtomicInteger(1000);
        listChildren(writer, resource, maxcount);
        if (maxcount.get() < 0) {
            writer.println("Stopped listing because it's too long");
        }
    }

    private void listChildren(PrintWriter writer, Resource resource, AtomicInteger maxcount) {
        if (maxcount.get() < 0) {
            return;
        }
        if (resource != null) {
            writer.println(resource.getPath());
            maxcount.decrementAndGet();
            for (Resource child : resource.getChildren()) {
                listChildren(writer, child, maxcount);
            }
        }
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        LOG.info("Activated: {}", config.enabled());
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil - Debugutil List Jcr Tree Servlet",
            description = "This servlet enables listing the resources in a JCR subtree - mainly for the publish instance, where there\n" +
                    " is no CRX. It requires a resource in a published page that has the sling:resourceType cpm/nodes/debug/listtree ,\n" +
                    " and then lists the suffix when a GET is triggered on that resource.\n" +
                    " \n" +
                    " CAUTION: not meant for production, only for internal testing systems!\n" +
                    " It does, however, require a configuration, so it won't be active on systems where it is not configured."
    )
    public @interface Configuration {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;
    }

}
