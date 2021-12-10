package com.composum.sling.nodes.servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * maps a Sling resource type to an existing servlet service implementation (e.g. for forwarding to webconsole plugins)
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=" + ServletResourceType.SERVLET_LABEL
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ServletResourceType.Config.class, factory = true)
public class ServletResourceType extends GenericServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ServletResourceType.class);

    public static final String SERVLET_LABEL = "Composum Servlet Resource Type";

    public static final String WEBCONSOLE_TYPE = "org.apache.felix.inventory.impl.WebConsolePlugin";

    @ObjectClassDefinition(name = SERVLET_LABEL)
    public @interface Config {

        @AttributeDefinition(
                name = "Resource Type",
                description = "the resource type which has to be mapped to the servlet"
        )
        String sling_servlet_resourceTypes(); // ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES

        @AttributeDefinition(
                name = "Service Type",
                description = "the full qualified service type (class name) to forward to"
        )
        String forward_servlet_serviceType();

        @AttributeDefinition(
                name = "Service Filter",
                description = "the filter options to find the designated service (webconsole plugin)"
        )
        String forward_servlet_serviceFilter();

        @AttributeDefinition(
                name = "Webconsole Plugin"
        )
        boolean webconsole_plugin();

        @AttributeDefinition(
                name = "Webconsole Plugin Title"
        )
        String webconsole_plugin_title();

        @AttributeDefinition()
        String webconsole_configurationFactory_nameHint() default
                "'{sling.servlet.resourceTypes}' > '{forward.servlet.serviceType}' [{forward.servlet.serviceFilter}]";
    }

    protected class ServletWrapper {

        protected final Servlet wrappedServlet;

        public ServletWrapper(@Nonnull final Servlet servlet) {
            wrappedServlet = servlet;
        }

        void service(@NotNull HttpServletRequest request,
                     @NotNull HttpServletResponse response)
                throws ServletException, IOException {
            wrappedServlet.service(request, response);
        }
    }

    protected class WebconsoleWrapper extends ServletWrapper {

        public static final String PLUGIN_LABEL = "felix.webconsole.label";
        public static final String PLUGIN_TITLE = "felix.webconsole.title";
        public static final String PLUGIN_CSS_REFERENCES = "felix.webconsole.css";
        public static final String ATTR_APP_ROOT = "felix.webconsole.appRoot";
        public static final String ATTR_PLUGIN_ROOT = "felix.webconsole.pluginRoot";
        public static final String ATTR_LABEL_MAP = "felix.webconsole.labelMap";

        public static final String WEBCONSOLE_CLASS = "org.apache.felix.webconsole.internal.servlet.OsgiManager";
        public static final String ATTR_LABEL_MAP_OLD = WEBCONSOLE_CLASS + ".appRoot";
        public static final String ATTR_APP_ROOT_OLD = WEBCONSOLE_CLASS + ".labelMap";

        public WebconsoleWrapper(@Nonnull final Servlet servlet) {
            super(servlet);
        }

        void service(@NotNull HttpServletRequest request,
                     @NotNull HttpServletResponse response)
                throws ServletException, IOException {

            final String label = getLabel(request);
            final String title = config.webconsole_plugin_title();
            final Map<String, String> labelMap = Collections.singletonMap(label,
                    StringUtils.isNotBlank(title) ? title : label);

            // the official request attributes
            request.setAttribute(ATTR_LABEL_MAP, labelMap);
            request.setAttribute(ATTR_APP_ROOT, request.getContextPath() + request.getServletPath());
            request.setAttribute(ATTR_PLUGIN_ROOT, request.getContextPath() + request.getServletPath() + '/' + label);

            // deprecated request attributes
            request.setAttribute(ATTR_LABEL_MAP_OLD, labelMap);
            request.setAttribute(ATTR_APP_ROOT_OLD, request.getContextPath() + request.getServletPath());

            super.service(request, response);
        }

        protected String getLabel(@NotNull HttpServletRequest request) {
            final String label = request.getPathInfo();
            int slash = label.indexOf("/", 1);
            if (slash < 2) {
                slash = label.length();
            }
            return label.substring(1, slash);
        }
    }

    @Reference
    DynamicClassLoaderManager classLoaderManager;

    @Reference
    ServletResolver servletResolver;

    private ComponentContext context;

    private Config config;

    private ServletWrapper forwardServlet;

    @Activate
    protected void activate(ComponentContext context, Config config) {
        this.context = context;
        this.config = config;
    }

    protected ServletWrapper wrapServlet(@Nonnull final Servlet servlet) {
        return config.webconsole_plugin() ? new WebconsoleWrapper(servlet) : new ServletWrapper(servlet);
    }

    public void service(@NotNull ServletRequest request,
                        @NotNull ServletResponse response)
            throws ServletException, IOException {
        if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
            final ServletWrapper forward = getForwardServlet();
            if (forward != null) {
                forward.service((HttpServletRequest) request, (HttpServletResponse) response);
            } else {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } else {
            throw new ServletException("Not an HTTP request/response");
        }
    }

    private ServletWrapper getForwardServlet() {
        if (forwardServlet == null) {
            try {
                final String serviceTypeName = config.forward_servlet_serviceType();
                final String serviceFilter = config.forward_servlet_serviceFilter();
                final Class<?> serviceType = getType(serviceTypeName);
                final BundleContext bundleContext = context.getBundleContext();
                final Collection<ServiceReference<Servlet>> candidates = bundleContext
                        .getServiceReferences(Servlet.class, StringUtils.isNotBlank(serviceFilter) ? serviceFilter : null);
                if (candidates.size() == 1) {
                    forwardServlet = wrapServlet(bundleContext.getService(candidates.iterator().next()));
                } else if (StringUtils.isNotBlank(serviceTypeName)) {
                    for (ServiceReference<Servlet> candidate : candidates) {
                        final Servlet servlet = bundleContext.getService(candidate);
                        if (serviceType != null) {
                            if (serviceType.isInstance(servlet)) {
                                forwardServlet = wrapServlet(servlet);
                                break;
                            }
                        } else if (servlet != null && serviceTypeName.equals(servlet.getClass().getName())) {
                            forwardServlet = wrapServlet(servlet);
                            break;
                        }
                    }
                }
                if (forwardServlet == null) {
                    LOG.error("no matching service found for '{}'", serviceTypeName);
                }
            } catch (InvalidSyntaxException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return forwardServlet;
    }

    private Class<?> getType(@Nullable final String className) {
        try {
            if (StringUtils.isNotBlank(className)) {
                return classLoaderManager.getDynamicClassLoader().loadClass(className);
            }
        } catch (ClassNotFoundException ignore) {
        }
        return null;
    }
}
