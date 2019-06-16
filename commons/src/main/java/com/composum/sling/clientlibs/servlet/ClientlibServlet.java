package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component(service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + Clientlib.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_HEAD,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=css",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=js"
        })
public class ClientlibServlet extends AbstractClientlibServlet {

    private static final Logger LOG = getLogger(ClientlibServlet.class);

    protected static final Pattern FILENAME_PATTERN = Pattern.compile("[^/]*+$");
    protected static final Pattern HASHSUFFIX_PATTERN = Pattern.compile("/?([0-9a-zA-Z_-]++)/" + FILENAME_PATTERN.pattern());

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
                    String path = pathInfo.getResourcePath();
                    String hash = parseHashFromSuffix(pathInfo.getSuffix());

                    ClientlibRef ref = new ClientlibRef(type, path, false, null);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("deliver: {} ({})", ref.path, request.getRequestURI());
                    }
                    deliverClientlib(get, request, response, ref, hash, isMinified(selectors));

                } else {
                    LOG.error("no extension found ({})", request.getRequestURL().toString());
                }
            } catch (RepositoryException ex) {
                throw new ServletException(ex);
            }
        }
    }

    /** Creates an path that is rendered by this servlet containing the given parameters. */
    public static String makePath(String path, Clientlib.Type type, boolean minified, String hash) {
        StringBuilder builder = new StringBuilder(path);
        if (minified) builder.append(".min");
        if (!path.endsWith("." + type.name()) && type != Clientlib.Type.img && type != Clientlib.Type.link) {
            builder.append('.').append(type.name()); // relevant for categories
        }
        return appendHashSuffix(builder.toString(), hash);
    }

    @Override
    protected String makeUri(boolean minified, ClientlibLink link) {
        return makePath(link.path, link.type, minified, link.hash);
    }

    /**
     * Appends a suffix containing the hash code, if given. The file name is repeated to satisfy browsers
     * with the correct type and file name, though it is not used by the servlet.
     *
     * @param url  an url to which we append the suffix
     * @param hash optional, the hash code
     * @return the url with suffix /{hash}/{filename} appended, where {filename} is the last part of a / separated url.
     */
    public static String appendHashSuffix(String url, String hash) {
        if (null == hash) return url;
        Matcher matcher = FILENAME_PATTERN.matcher(url);
        String fname = "";
        if (matcher.find()) fname = matcher.group(0);
        return url + "/" + hash + "/" + fname;
    }

    /**
     * Does the inverse to {@link #appendHashSuffix(String, String)}: extracts the hash from the generated suffix.
     *
     * @param suffix the suffix generated by {@link #appendHashSuffix(String, String)} , nullable
     * @return the hash if it could be extracted, otherwise null.
     */
    public static String parseHashFromSuffix(String suffix) {
        if (StringUtils.isBlank(suffix)) return null;
        Matcher matcher = HASHSUFFIX_PATTERN.matcher(suffix);
        if (matcher.matches()) {
            return matcher.group(1);
        } else LOG.warn("Could not parse hash suffix {}", suffix);
        return null;
    }

}
