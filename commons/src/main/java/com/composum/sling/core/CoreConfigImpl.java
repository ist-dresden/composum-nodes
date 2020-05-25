package com.composum.sling.core;

import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.JobControlServlet;
import com.composum.sling.core.servlet.SystemServlet;
import com.composum.sling.core.servlet.TranslationServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The configuration service for all servlets in the core bundle.
 */
@Component(
        service = CoreConfiguration.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=the configuration service for all servlets in the core bundle"
        },
        immediate = true
)
@Designate(ocd = CoreConfigImpl.Configuration.class)
public class CoreConfigImpl implements CoreConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CoreConfigImpl.class);

    protected static final String DEFAULT_LOGOUTURL = "/system/sling/logout.html?logout=true&GLO=true";
    protected static final String DEFAULT_LOGGEDOUTURL = "/system/sling/form/login.html";
    protected static final String DEFAULT_LOGINURL = "/system/sling/form/login.html";


    /**
     * The configuration object for CoreConfigImpl. Naming of the methods is for backward compatibility.
     */
    @ObjectClassDefinition(name = "Composum Nodes (Core) Configuration", description = "the configuration service for all servlets in the core bundle")
    public @interface Configuration {

        @AttributeDefinition(
                name = "Errorpages",
                description = "the path to the errorpages; e.g. 'meta/errorpages' for searching errorpages along the requested path"
        )
        String errorpages_path();

        @AttributeDefinition(
                name = "Default Errorpages",
                description = "the path to the systems default error pages"
        )
        String errorpages_default();

        @AttributeDefinition(
                name = "Jobcontrol Servlet",
                description = "the general on/off switch for the services of the Jobcontrol Servlet"
        )
        boolean jobcontrol_servlet_enabled() default true;

        @AttributeDefinition(
                name = "System Servlet",
                description = "the general on/off switch for the services of the System Servlet"
        )
        boolean system_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Translation Servlet",
                description = "the general on/off switch for the services of the Translation Servlet"
        )
        boolean translation_servlet_enabled() default true;

        @AttributeDefinition(
                name = "Logout URL",
                description = "logout URL for the system"
        )
        String logouturl() default DEFAULT_LOGOUTURL;

        @AttributeDefinition(
                name = "Logged out URL",
                description = "URL for the system to redirect to when the user was logged out"
        )
        String loggedouturl() default DEFAULT_LOGGEDOUTURL;

        @AttributeDefinition(
                name = "Login URL",
                description = "URL for the system to redirect / link to when the user should be logged in"
        )
        String loginurl() default DEFAULT_LOGINURL;
    }

    private volatile Configuration config;

    private volatile Map<String, Boolean> enabledServlets;

    private volatile Dictionary properties;

    @Nonnull
    private Configuration getConfig() {
        return Objects.requireNonNull(config, "CoreConfigImpl is not active");
    }

    @Override
    public boolean isEnabled(AbstractServiceServlet servlet) {
        return enabledServlets != null && Boolean.TRUE.equals(enabledServlets.get(servlet.getClass().getSimpleName()));
    }

    /**
     * Determines the error page corresponding to the requested path.
     * Is searching upwards beginning with the requested path for an error page
     * using the path pattern: {requested path}/{errorpagesPath}/{status code}.
     * If nothing found the pattern: {defaultErrorpages}/{status code} is used if existing.
     *
     * @return the error page found; <code>null</code> if no error page available
     */
    @Override
    public Resource getErrorpage(SlingHttpServletRequest request, int status) {
        Resource errorpage = null;

        try {
            ResourceResolver resolver = request.getResourceResolver();

            RequestPathInfo pathInfo;
            String errorpagesPath = StringUtils.removeEnd( getConfig().errorpages_path(), "/");
            if (StringUtils.isNotBlank(errorpagesPath) &&
                    "html".equalsIgnoreCase((pathInfo = request.getRequestPathInfo()).getExtension())) {

                if (errorpagesPath.startsWith("/")) {
                    // if the configured path is an absolute path use this path only
                    errorpage = resolver.getResource(errorpagesPath + "/" + status);

                } else {
                    String path = pathInfo.getResourcePath();
                    Resource resource = resolver.resolve(request, path);
                    path = resource.getPath(); // switch from path info to resource

                    // skip non existing resource paths in the requested path
                    int lastSlash;
                    while ((resource = resolver.getResource(path)) == null
                            && (lastSlash = path.lastIndexOf('/')) > 2) {
                        path = path.substring(0, lastSlash);
                    }

                    // scan upwards for an appropriate error page
                    while (errorpage == null && resource != null) {
                        path = resource.getPath();
                        if ("/".equals(path)) {
                            break;
                        }
                        errorpage = resolver.getResource(path + "/" + errorpagesPath + "/" + status);
                        if (errorpage == null) {
                            resource = resource.getParent();
                        }
                    }
                }
            }
            String defaultErrorpages = StringUtils.removeEnd(getConfig().errorpages_default(), "/");
            if (errorpage == null && StringUtils.isNotBlank(defaultErrorpages)) {
                // use the default page if no custom error page found
                errorpage = resolver.getResource(defaultErrorpages + "/" + status);
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return errorpage;
    }

    @Override
    public boolean forwardToErrorpage(SlingHttpServletRequest request,
                                      SlingHttpServletResponse response, int status)
            throws ServletException, IOException {
        Resource errorpage = getErrorpage(request, status);
        if (errorpage != null) {
            request.setAttribute(ERRORPAGE_STATUS, status); // hint for the custom page
            RequestDispatcher dispatcher = request.getRequestDispatcher(errorpage);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return true;
            }
        }
        return false;
    }

    @Override
    public Dictionary getProperties() {
        return properties;
    }

    @Override
    public String getLoginUrl() {
        return StringUtils.defaultIfBlank( getConfig().loginurl(), DEFAULT_LOGINURL);
    }

    @Override
    public String getLogoutUrl() {
        return StringUtils.defaultIfBlank(getConfig().logouturl(), DEFAULT_LOGOUTURL);
    }

    @Override
    public String getLoggedoutUrl() {
        return StringUtils.defaultIfBlank( getConfig().loggedouturl(), DEFAULT_LOGGEDOUTURL);
    }

    @Activate
    @Modified
    protected void activate(ComponentContext context, Configuration configuration) {
        this.properties = context.getProperties();
        this.config = configuration;
        Map<String, Boolean> theEnabledServlets = new HashMap<>();
        theEnabledServlets.put(SystemServlet.class.getSimpleName(), configuration.system_servlet_enabled());
        theEnabledServlets.put(TranslationServlet.class.getSimpleName(), configuration.translation_servlet_enabled());
        theEnabledServlets.put(JobControlServlet.class.getSimpleName(), configuration.jobcontrol_servlet_enabled());
        enabledServlets = theEnabledServlets;
    }

    @Deactivate
    protected void deactivate() {
        this.properties = null;
    }
}
