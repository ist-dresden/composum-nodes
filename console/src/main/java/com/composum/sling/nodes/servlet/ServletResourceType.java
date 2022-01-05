package com.composum.sling.nodes.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.RequestBundle;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.TagFilteringWriter;
import com.composum.sling.core.util.ValueEmbeddingWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.apache.sling.commons.osgi.PropertiesUtil.toStringArray;

/**
 * maps a Sling resource type to an existing servlet service implementation (e.g. for forwarding to webconsole plugins)
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=" + ServletResourceType.SERVLET_LABEL,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
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
                name = "Permission",
                description = "the necessary permission to use this service"
        )
        String forward_servlet_permission();

        @AttributeDefinition(
                name = "Webconsole Plugin",
                description = "marks a servlet as a Webconsole plugin implementation"
        )
        boolean webconsole_plugin() default false;

        @AttributeDefinition(
                name = "Webconsole Plugin Title",
                description = "the title of the rendered Webconsole plugin view"
        )
        String webconsole_plugin_title() default "";

        @AttributeDefinition(
                name = "Webconsole App Path",
                description = "the path (URI) to use during Webconsole plugin rendering"
        )
        String webconsole_app_path() default "";

        @AttributeDefinition(
                name = "Filter Content",
                description = "if selected the generated HTML is filtered"
        )
        boolean content_filter_on() default false;

        @AttributeDefinition(
                name = "Translation",
                description = "if selected all placeholders are used for translations if not a value"
        )
        boolean content_translation_on() default false;

        @AttributeDefinition()
        String webconsole_configurationFactory_nameHint() default
                "'{sling.servlet.resourceTypes}' > '{forward.servlet.serviceType}' [{forward.servlet.serviceFilter}]";
    }

    protected class SuffixRequest extends SlingHttpServletRequestWrapper {

        protected final String suffix;

        public SuffixRequest(@NotNull final SlingHttpServletRequest request, @NotNull final String suffix) {
            super(request);
            this.suffix = suffix;
        }

        @Override
        public String getPathInfo() {
            return suffix;
        }

        @Override
        public String getRequestURI() {
            return suffix;
        }
    }

    protected class ServletResponseWrapper extends HttpServletResponseWrapper {

        protected final PrintWriter wrappedWriter;
        protected final ValueEmbeddingWriter valuesWriter;
        protected final Writer transformingWriter;
        protected final PrintWriter printWriter;

        public ServletResponseWrapper(@NotNull final HttpServletRequest request,
                                      @NotNull final HttpServletResponse response,
                                      @NotNull final Map<String, Object> values)
                throws IOException {
            super(response);
            final Locale locale = request.getLocale();
            final ResourceBundle i18n = config.content_translation_on() && request instanceof SlingHttpServletRequest
                    ? RequestBundle.get((SlingHttpServletRequest) request) : null;
            wrappedWriter = response.getWriter();
            valuesWriter = new ValueEmbeddingWriter(wrappedWriter, values, locale, getClass(), i18n);
            transformingWriter = config.content_filter_on() ? new TagFilteringWriter(valuesWriter) : valuesWriter;
            printWriter = new PrintWriter(transformingWriter);
        }

        @Override
        public PrintWriter getWriter() {
            return printWriter;
        }

        public void flush() {
            try {
                printWriter.flush();
                transformingWriter.flush();
                wrappedWriter.flush();
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
    }

    protected class ServletWrapper {

        protected final Servlet wrappedServlet;

        public ServletWrapper(@NotNull final Servlet servlet) {
            wrappedServlet = servlet;
        }

        public void service(@NotNull HttpServletRequest request,
                            @NotNull HttpServletResponse response)
                throws ServletException, IOException {
            wrappedServlet.service(request, response);
        }
    }

    protected class WebconsoleWrapper extends ServletWrapper {

        public static final String PLUGIN_CATEGORY = "felix.webconsole.category";
        public static final String PLUGIN_LABEL = "felix.webconsole.label";
        public static final String PLUGIN_TITLE = "felix.webconsole.title";
        public static final String PLUGIN_CSS_REFERENCES = "felix.webconsole.css";
        public static final String ATTR_APP_ROOT = "felix.webconsole.appRoot";
        public static final String ATTR_PLUGIN_ROOT = "felix.webconsole.pluginRoot";
        public static final String ATTR_LABEL_MAP = "felix.webconsole.labelMap";

        public static final String WEBCONSOLE_CLASS = "org.apache.felix.webconsole.internal.servlet.OsgiManager";
        public static final String ATTR_LABEL_MAP_OLD = WEBCONSOLE_CLASS + ".appRoot";
        public static final String ATTR_APP_ROOT_OLD = WEBCONSOLE_CLASS + ".labelMap";

        public static final String WEBCONSOLE_PATH = "/system/console";

        public static final String PLUGIN_TOOL_PATH = "composum/nodes/system/tools/webconsole/plugin";

        public final String[] CSS_FILES = new String[]{
                "/css/reset-min.css",
                "/css/jquery-ui.css",
                "/css/webconsole.css",
                "/css/admin_compat.css",
                "/css/corrections.css"
        };
        public final String[] JS_FILES = new String[]{
                "/js/jquery-3.3.1.js",
                "/js/jquery-migrate-3.0.0.js",
                "/js/jquery-ui-1.12.1.js",
                "/js/jquery-ui-i18n-1.12.1.js",
                "/js/jquery.cookies-2.2.0.js",
                "/js/jquery.tablesorter-2.0.3.js",
                "/js/autosize.min.js",
                "/js/support.js"
        };

        protected final ValueMap properties;
        protected final String[] cssReferences;

        public WebconsoleWrapper(@NotNull final Servlet servlet, @NotNull final Map<String, Object> properties) {
            super(servlet);
            this.properties = new ValueMapDecorator(properties);
            this.cssReferences = toStringArray(properties.get(PLUGIN_CSS_REFERENCES));
        }

        protected class ActionResponseWrapper extends HttpServletResponseWrapper {

            protected ServletOutputStream outputStream = new ServletOutputStream() {

                @Override
                public void write(int b) {
                    // drop it;
                }
            };

            protected PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            public ActionResponseWrapper(HttpServletResponse response) {
                super(response);
            }

            public ServletOutputStream getOutputStream() {
                return outputStream;
            }

            public PrintWriter getWriter() throws IOException {
                return writer;
            }
        }

        public void service(@NotNull HttpServletRequest request,
                            @NotNull HttpServletResponse response)
                throws ServletException, IOException {

            if ("GET".equalsIgnoreCase(request.getMethod())) {
                renderPlugin(request, response);
            } else {
                if (request instanceof SlingHttpServletRequest &&
                        isEnabled((SlingHttpServletRequest) request, ServiceRestrictions.Permission.write)) {
                    final ActionResponseWrapper responseWrapper = new ActionResponseWrapper(response);
                    super.service(request, responseWrapper);
                }
                renderPlugin(request, response);
            }
        }

        protected void renderPlugin(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response)
                throws ServletException, IOException {

            if (request instanceof SlingHttpServletRequest) {
                final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
                final RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();
                final String suffix = pathInfo.getSuffix();
                if (StringUtils.isNotBlank(suffix)) {
                    final SuffixRequest requestWrapper = new SuffixRequest(slingRequest, WEBCONSOLE_PATH + suffix);
                    super.service(requestWrapper, response);
                    return;
                }
            }

            final ValueMap values = new ValueMapDecorator(new HashMap<>(properties));
            final String category = values.get(PLUGIN_CATEGORY, "generic");
            final String label = values.get(PLUGIN_LABEL, "generic");
            final String cssClass = "webconsole-" + category.toLowerCase() + "-" + label.toLowerCase();
            final String title = config.webconsole_plugin_title();
            final Map<String, String> labelMap = Collections.singletonMap(label,
                    StringUtils.isNotBlank(title) ? title : label);

            String appPath = config.webconsole_app_path();
            if (StringUtils.isBlank(appPath)) {
                appPath = request.getContextPath() + request.getPathInfo();
            }
            final String appRoot = appPath.startsWith(WEBCONSOLE_PATH) ? WEBCONSOLE_PATH : appPath;

            // the official request attributes
            request.setAttribute(ATTR_LABEL_MAP, labelMap);
            request.setAttribute(ATTR_APP_ROOT, appRoot);
            request.setAttribute(ATTR_PLUGIN_ROOT, appPath);
            values.put(ATTR_LABEL_MAP, labelMap);
            values.put(ATTR_APP_ROOT, appRoot);
            values.put(ATTR_PLUGIN_ROOT, appPath);

            // deprecated request attributes
            request.setAttribute(ATTR_LABEL_MAP_OLD, labelMap);
            request.setAttribute(ATTR_APP_ROOT_OLD, WEBCONSOLE_PATH);
            values.put(ATTR_LABEL_MAP_OLD, labelMap);
            values.put(ATTR_APP_ROOT_OLD, WEBCONSOLE_PATH);

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                    "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                    "<head>\n" +
                    "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n" +
                    "    <title>").append(title).append("</title>\n");
            for (final String cssFile : CSS_FILES) {
                appendCssLink(request, writer, coreConfig.getComposumBase() + PLUGIN_TOOL_PATH, cssFile);
            }
            if (cssReferences != null) {
                for (final String cssRef : cssReferences) {
                    appendCssLink(request, writer, appRoot, cssRef.startsWith("/") ? cssRef : ("/" + cssRef));
                }
            }
            writer.append("    <script type=\"text/javascript\">\n" +
                    "        // <![CDATA[\n" +
                    "        appRoot = \"").append(appRoot).append("\";\n" +
                    "        pluginRoot = \"").append(appPath).append("\";\n" +
                    "        // ]]>\n" +
                    "    </script>\n");
            for (final String jsFile : JS_FILES) {
                appendJsLink(request, writer, jsFile);
            }
            writer.append("</head>\n" +
                    "<body class=\"ui-widget webconsole-plugin\">\n" +
                    "<div id=\"main\" class=\"").append(cssClass).append("\">\n");
            if (StringUtils.isNotBlank(title)) {
                writer.append("    <h2 id=\"title\" class=\"webconsole-plugin_title\">").append(title).append("</h2>\n");
            }
            writer.append("    <div id=\"content\" class=\"webconsole-plugin_content\">\n");
            ServletResponseWrapper responseWrapper = new ServletResponseWrapper(request, response, values);
            super.service(request, responseWrapper);
            responseWrapper.flush();
            writer.append("    </div>\n" +
                    "</div>\n" +
                    "</body>\n");
        }

        protected void appendCssLink(@NotNull final HttpServletRequest request, @NotNull final PrintWriter writer,
                                     @NotNull final String root, @NotNull final String path) {
            writer.append("    <link href=\"")
                    .append(request.getContextPath()).append(root).append(path)
                    .append("\" rel=\"stylesheet\" type=\"text/css\">\n");
        }

        protected void appendJsLink(@NotNull final HttpServletRequest request, @NotNull final PrintWriter writer,
                                    @NotNull final String path) {
            writer.append("    <script src=\"")
                    .append(request.getContextPath()).append(coreConfig.getComposumBase())
                    .append(PLUGIN_TOOL_PATH).append(path).append("\" type=\"text/javascript\"></script>\n");
        }
    }

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private ServiceRestrictions serviceRestrictions;

    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    @Reference
    private ServletResolver servletResolver;

    private ComponentContext context;

    private Config config;
    private ServiceRestrictions.Key permissionKey;

    private ServletWrapper forwardServlet;

    @Activate
    protected void activate(ComponentContext context, Config config) {
        this.context = context;
        this.config = config;
        permissionKey = new ServiceRestrictions.Key(config.forward_servlet_permission());
    }

    protected boolean isEnabled(@NotNull final SlingHttpServletRequest request,
                                @NotNull final ServiceRestrictions.Permission needed) {
        return permissionKey.isEmpty() || serviceRestrictions.isPermissible(request, permissionKey, needed);
    }

    protected ServletWrapper wrapServlet(@NotNull final BundleContext bundleContext,
                                         @NotNull final ServiceReference<Servlet> reference) {
        final Map<String, Object> properties = new HashMap<>();
        for (String key : reference.getPropertyKeys()) {
            properties.put(key, reference.getProperty(key));
        }
        return wrapServlet(bundleContext.getService(reference), properties);
    }

    protected ServletWrapper wrapServlet(@NotNull final Servlet servlet,
                                         @NotNull final Map<String, Object> properties) {
        return config.webconsole_plugin() ? new WebconsoleWrapper(servlet, properties) : new ServletWrapper(servlet);
    }

    public void service(@NotNull ServletRequest servletRequest,
                        @NotNull ServletResponse response)
            throws ServletException, IOException {
        if ((servletRequest instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
            if (isEnabled(request, ServiceRestrictions.Permission.read)) {
                final ServletWrapper forward = getForwardServlet();
                if (forward != null) {
                    forward.service((HttpServletRequest) servletRequest, (HttpServletResponse) response);
                } else {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            } else {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
        } else {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_GATEWAY);
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
                    forwardServlet = wrapServlet(bundleContext, candidates.iterator().next());
                } else if (StringUtils.isNotBlank(serviceTypeName)) {
                    for (ServiceReference<Servlet> candidate : candidates) {
                        final Servlet servlet = bundleContext.getService(candidate);
                        if (serviceType != null) {
                            if (serviceType.isInstance(servlet)) {
                                forwardServlet = wrapServlet(servlet, new HashMap<>());
                                break;
                            }
                        } else if (servlet != null && serviceTypeName.equals(servlet.getClass().getName())) {
                            forwardServlet = wrapServlet(servlet, new HashMap<>());
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
