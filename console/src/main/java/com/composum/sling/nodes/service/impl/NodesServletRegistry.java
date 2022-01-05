package com.composum.sling.nodes.service.impl;

import com.composum.sling.nodes.service.ServletRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.servlet.Servlet;
import java.util.HashMap;

@Component
public class NodesServletRegistry implements ServletRegistry {

    protected final HashMap<String, ServletData> registeredServlets = new HashMap<>();

    @Reference(
            service = Servlet.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void bindServlet(ServiceReference<Servlet> reference) {
        final ServletData servletData = new ServletData(reference);
        registeredServlets.put(servletData.getServletKey(), servletData);
    }

    protected void unbindServlet(ServiceReference<Servlet> reference) {
        final String servletKey = ServletRegistry.getServletKey(reference);
        registeredServlets.remove(servletKey);
    }

    @Override
    public @Nullable ServletData getServletData(@NotNull String servletKey) {
        return registeredServlets.get(servletKey);
    }
}
