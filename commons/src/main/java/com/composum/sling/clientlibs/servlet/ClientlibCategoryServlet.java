package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.util.XSS;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves the clientlibs of a whole category with all embedded stuff. The path /bin/cpm/clientlib.{type}/{hash}/{categoryname}.{type}
 * needs the type as extension and the hash, category and type as suffix.
 */
@Component(service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + ClientlibCategoryServlet.PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_HEAD,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=css",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=js"
        })
public class ClientlibCategoryServlet extends AbstractClientlibServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibCategoryServlet.class);

    @Reference
    protected ClientlibService service;

    @Reference
    protected ClientlibConfiguration configuration;

    protected ClientlibService getClientlibService() {
        return service;
    }

    protected ClientlibConfiguration.Config getConfig() {
        return configuration.getConfig();
    }

    /** The path at which this servlet is deployed. */
    public static final String PATH = "/bin/public/clientlibs";

    protected static final Pattern HASHSUFFIX_PATTERN = Pattern.compile("/?([0-9a-zA-Z_-]++)?/([" + Clientlib.CATEGORYNAME_CHARS + "]+)[.][a-z]+");

    /** Creates an path that is rendered by this servlet containing the given parameters. */
    public static String makePath(String category, Clientlib.Type type, boolean minified, String hash) {
        StringBuilder buf = new StringBuilder(PATH);
        if (minified) buf.append(".min");
        buf.append(".").append(type.name());
        if (null != hash) {
            buf.append("/").append(hash);
        }
        buf.append("/").append(Clientlib.sanitizeCategory(category));
        buf.append(".").append(type.name());
        return buf.toString();
    }

    @Override
    protected String makeUri(boolean minified, ClientlibLink link) {
        return makePath(link.path, link.type, minified, link.hash);
    }

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        serve(true, request, response);
    }

    @Override
    protected void doHead(@Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        serve(false, request, response);
    }

    private void serve(boolean get,
                       @Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response)
            throws IOException, ServletException {
        if (usefulRequest(request, response)) {
            try {
                RequestPathInfo pathInfo = request.getRequestPathInfo();
                String extension = pathInfo.getExtension();
                if (StringUtils.isNotBlank(extension)) {
                    String selectors = pathInfo.getSelectorString();
                    Clientlib.Type type = Clientlib.Type.valueOf(extension.toLowerCase());
                    Pair<String, String> categoryAndHash = parseCategoryAndHashFromSuffix(XSS.filter(pathInfo.getSuffix()));

                    ClientlibRef ref = ClientlibRef.forCategory(type, categoryAndHash.getLeft(), false, null);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("deliver: {} ({})", ref.category, request.getRequestURI());
                    }
                    deliverClientlib(get, request, response, ref, categoryAndHash.getRight(), isMinified(selectors));

                } else {
                    LOG.error("no extension found ({})", request.getRequestURL().toString());
                }
            } catch (RepositoryException ex) {
                throw new ServletException(ex);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("dropped: {}", request.getRequestURI());
            }
        }
    }

    public static Pair<String, String> parseCategoryAndHashFromSuffix(String suffix) {
        Matcher matcher = HASHSUFFIX_PATTERN.matcher(suffix);
        if (!matcher.matches()) throw new IllegalArgumentException("Could not parse suffix " + suffix);
        return Pair.of(matcher.group(2), matcher.group(1));
    }
}
