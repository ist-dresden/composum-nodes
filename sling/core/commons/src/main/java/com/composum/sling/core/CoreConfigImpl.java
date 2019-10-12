package com.composum.sling.core;

import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.JobControlServlet;
import com.composum.sling.core.servlet.SystemServlet;
import com.composum.sling.core.servlet.TranslationServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * The configuration service for all servlets in the core bundle.
 */
@SuppressWarnings("FieldCanBeLocal")
@Component(
        label = "Composum Core Configuration",
        description = "the configuration service for all servlets in the core bundle",
        immediate = true,
        metatype = true
)
@Service
public class CoreConfigImpl implements CoreConfiguration {

    protected static final String ERRORPAGE_STATUS = "errorpage.status";
    protected static final String ERRORPAGES_PATH = "errorpages.path";
    protected static final String DEFAULT_ERRORPAGES = "errorpages.default";

    protected static final String TREE_INTERMEDIATE_FILTER_KEY = "tree.intermediate.filter";

    protected static final String SYSTEM_SERVLET_ENABLED = "system.servlet.enabled";

    protected static final String TRANSLATION_SERVLET_ENABLED = "validation.servlet.enabled";

    protected static final String FORWARDED_SSL_PORT = "network.forward.ssl.port";

    protected static final String LOGOUT_URL = "logouturl";
    protected static final String DEFAULT_LOGOUTURL = "/system/sling/logout.html?logout=true&GLO=true";
    protected static final String LOGGEDOUT_URL = "loggedouturl";
    protected static final String DEFAULT_LOGGEDOUTURL = "/system/sling/form/login.html";
    protected static final String LOGIN_URL = "loginurl";
    protected static final String DEFAULT_LOGINURL = "/system/sling/form/login.html";

    private static final Logger LOG = LoggerFactory.getLogger(CoreConfigImpl.class);

    public static final int DEFAULT_FORWARDED_SSL_PORT = 80; // cerrently delivered by the Felix HTTP module

    @Property(
            name = FORWARDED_SSL_PORT,
            label = "Forwarded SSL Port",
            description = "the port number which has to be used to determin the default port nnumber",
            intValue = DEFAULT_FORWARDED_SSL_PORT
    )
    private int forwardedSslPort;

    @Property(
            name = ERRORPAGES_PATH,
            label = "Errorpages",
            description = "the path to the errorpages; e.g. 'meta/errorpages' for searching errorpages along the requested path"
    )
    private String errorpagesPath;

    @Property(
            name = DEFAULT_ERRORPAGES,
            label = "Default Errorpages",
            description = "the path to the systems default error pages"
    )
    private String defaultErrorpages;

    public static final String JOBCONTROL_SERVLET_ENABLED = "jobcontrol.servlet.enabled";
    @Property(
            name = "jobcontrol.servlet.enabled",
            label = "Jobcontrol Servlet",
            description = "the general on/off switch for the services of the Jobcontrol Servlet",
            boolValue = true
    )
    private boolean jobcontrolServletEnabled;

    @Property(
            name = SYSTEM_SERVLET_ENABLED,
            label = "System Servlet",
            description = "the general on/off switch for the services of the System Servlet",
            boolValue = true
    )
    private boolean systemServletEnabled;

    @Property(
            name = TRANSLATION_SERVLET_ENABLED,
            label = "Translation Servlet",
            description = "the general on/off switch for the services of the Translation Servlet",
            boolValue = true
    )
    private boolean translationServletEnabled;

    @Property(
            name = LOGOUT_URL,
            label = "Logout URL",
            description = "logout URL for the system",
            value = DEFAULT_LOGOUTURL
    )
    private String logoutUrl;

    @Property(
            name = LOGGEDOUT_URL,
            label = "Logged out URL",
            description = "URL for the system to redirect to when the user was logged out",
            value = DEFAULT_LOGGEDOUTURL
    )
    private String loggedoutUrl;

    @Property(
            name = LOGIN_URL,
            label = "Login URL",
            description = "URL for the system to redirect / link to when the user should be logged in",
            value = DEFAULT_LOGINURL
    )
    private String loginUrl;


    private Map<String, Boolean> enabledServlets;


    @Override
    public int getForwardedSslPort() {
        return forwardedSslPort;
    }

    @Override
    public boolean isEnabled(AbstractServiceServlet servlet) {
        Boolean result = enabledServlets.get(servlet.getClass().getSimpleName());
        return result != null ? result : false;
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
            if (errorpage == null && StringUtils.isNotBlank(defaultErrorpages)) {
                // use the default page if no custom error page found
                errorpage = resolver.getResource(defaultErrorpages + "/" + status);
            }

        } catch (Throwable ex) {
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
    public String getLogoutUrl() {
        return logoutUrl;
    }

    @Override
    public String getLoggedoutUrl() {
        return loggedoutUrl;
    }

    @Override
    public String getLoginUrl() {
        return loginUrl;
    }

    protected Dictionary properties;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.properties = context.getProperties();
        forwardedSslPort = PropertiesUtil.toInteger(properties.get(FORWARDED_SSL_PORT), DEFAULT_FORWARDED_SSL_PORT);
        errorpagesPath = StringUtils.removeEnd((String) properties.get(ERRORPAGES_PATH), "/");
        defaultErrorpages = StringUtils.removeEnd((String) properties.get(DEFAULT_ERRORPAGES), "/");
        enabledServlets = new HashMap<>();
        enabledServlets.put(SystemServlet.class.getSimpleName(), systemServletEnabled =
                (Boolean) properties.get(SYSTEM_SERVLET_ENABLED));
        enabledServlets.put(TranslationServlet.class.getSimpleName(), translationServletEnabled =
                (Boolean) properties.get(TRANSLATION_SERVLET_ENABLED));
        enabledServlets.put(JobControlServlet.class.getSimpleName(), jobcontrolServletEnabled =
                (Boolean) properties.get(JOBCONTROL_SERVLET_ENABLED));
        logoutUrl = StringUtils.defaultIfBlank(StringUtils.trim((String) properties.get(LOGOUT_URL)),
                DEFAULT_LOGOUTURL);
        loggedoutUrl = StringUtils.defaultIfBlank(StringUtils.trim((String) properties.get(LOGGEDOUT_URL)),
                DEFAULT_LOGGEDOUTURL);
        loginUrl = StringUtils.defaultIfBlank(StringUtils.trim((String) properties.get(LOGIN_URL)),
                DEFAULT_LOGINURL);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.properties = null;
    }
}
