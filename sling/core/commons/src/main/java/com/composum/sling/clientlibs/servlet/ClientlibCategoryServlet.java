package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves the clientlibs of a whole category with all embedded stuff. The path /bin/cpm/clientlib.{type}/{hash}/{categoryname}.{type}
 * needs the type as extension and the hash, category and type as suffix.
 */
@SlingServlet(
        methods = {HttpConstants.METHOD_GET, HttpConstants.METHOD_HEAD},
        paths = ClientlibCategoryServlet.PATH,
        extensions = {"js", "css", "map"}
)
public class ClientlibCategoryServlet extends AbstractClientlibServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibCategoryServlet.class);

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
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        serve(true, request, response);
    }

    @Override
    protected void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        serve(false, request, response);
    }

    private void serve(boolean get, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        if (!dropRequest(request, response)) {
            try {
                RequestPathInfo pathInfo = request.getRequestPathInfo();
                String selectors = pathInfo.getSelectorString();
                Clientlib.Type type = Clientlib.Type.valueOf(pathInfo.getExtension().toLowerCase());
                Pair<String, String> categoryAndHash = parseCategoryAndHashFromSuffix(pathInfo.getSuffix());

                ClientlibRef ref = ClientlibRef.forCategory(type, categoryAndHash.getLeft(), false, null);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("deliver: {} ({})", ref.category, request.getRequestURI());
                }
                deliverClientlib(get, request, response, ref, categoryAndHash.getRight(), isMinified(selectors));

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
