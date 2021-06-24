package com.composum.nodes.debugutil;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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

/**
 * Allocates and frees 1 gigabyte of memory. Caution when enabling this. ;-)
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Memory Hog Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/nodes/debug/memoryhog",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MemoryHogServlet.Configuration.class)
public class MemoryHogServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (config == null || !config.enabled()) {
            throw new IllegalStateException("Not enabled.");
        }
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html><body><h1>Allocating and freeing 1 gigabyte of memory</h1>");
        writer.flush();
        // allocating
        int allocated = 0;
        try {
            byte[][] discardedmemory = new byte[1024][];
            for (int i = 0; i < discardedmemory.length; ++i) {
                discardedmemory[i] = new byte[1024 * 1024];
                allocated++;
                if (allocated % 10 == 0) {
                    writer.println("Allocated " + allocated + " MB");
                    writer.flush();
                }
            }
            // release it
            discardedmemory = null;
        } catch (OutOfMemoryError e) {
            writer.println("OOM at " + allocated + " : " + e);
        }
        writer.println("Allocation done: " + allocated);
        writer.flush();
        writer.println("<hr/></body></html>");
    }

    protected Configuration config;

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Memory Hog Servlet",
            description = "Allocates and frees 1 gigabyte of memory. Caution when enabling this. ;-) \n" +
                    "Path: /bin/cpm/nodes/debug/memoryhog"
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
}
