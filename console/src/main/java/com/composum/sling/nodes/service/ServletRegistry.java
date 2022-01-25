package com.composum.sling.nodes.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.ServiceReference;

import javax.servlet.Servlet;
import java.util.HashMap;
import java.util.Set;

import static org.osgi.framework.Constants.OBJECTCLASS;

public interface ServletRegistry {

    String[] SERVLET_KEY_PROPERTIES = new String[]{
            ServletResolverConstants.SLING_SERVLET_NAME,
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            ServletResolverConstants.SLING_SERVLET_PATHS,
    };

    class ServletData {

        protected final String servletKey;
        protected final ValueMap properties = new ValueMapDecorator(new HashMap<>());

        public ServletData(@NotNull final ServiceReference<Servlet> reference) {
            servletKey = ServletRegistry.getServletKey(reference);
            for (String key : reference.getPropertyKeys()) {
                properties.put(key, reference.getProperty(key));
            }
        }

        public @NotNull Set<String> getProperyKeys() {
            return properties.keySet();
        }

        public @NotNull <T> T getPropery(@NotNull final String key, @NotNull final T defaultValue) {
            return properties.get(key, defaultValue);
        }

        public @Nullable <T> T getPropery(@NotNull final String key, @NotNull final Class<T> type) {
            return properties.get(key, type);
        }

        public @NotNull String getServletKey() {
            return servletKey;

        }
    }

    @Nullable ServletData getServletData(@NotNull final String servletKey);

    static @NotNull String getServletKey(@NotNull final ServiceReference<Servlet> reference) {
        Object servletKey;
        for (final String propertyKey : SERVLET_KEY_PROPERTIES) {
            servletKey = reference.getProperty(propertyKey);
            if (servletKey instanceof String && StringUtils.isNotBlank((String) servletKey)) {
                return (String) servletKey;
            }
        }
        return ((String[]) reference.getProperty(OBJECTCLASS))[0];
    }
}
