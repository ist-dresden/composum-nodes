package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.util.HttpUtil;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Contains common functionality for the clientlib servlets.
 */
@Component(componentAbstract = true)
public abstract class AbstractClientlibServlet extends SlingSafeMethodsServlet {

    private final Logger LOG = getLogger(AbstractClientlibServlet.class);

    @Reference
    protected ClientlibService service;

    @Reference
    protected ClientlibConfiguration configuration;

    /**
     * @param get           if false we serve a HEAD request, if true we serve a GET request
     * @param requestedHash the hash embedded in the URL that gives the requested version
     */
    protected void deliverClientlib(boolean get, SlingHttpServletRequest request, SlingHttpServletResponse response,
                                    ClientlibRef clientlibRef, String requestedHash, boolean minified) throws
            RepositoryException, IOException {

        try {

            String encoding = null;
            boolean refreshCache = false;
            String header;

            header = request.getHeader(HttpUtil.HEADER_ACCEPT_ENCODING);
            if (StringUtils.isNotBlank(header) && header.contains("gzip")) {
                encoding = ClientlibService.ENCODING_GZIP;
            }

            header = request.getHeader(HttpUtil.HEADER_CACHE_CONTROL);
            if (StringUtils.isNotBlank(header) && configuration.getRerenderOnNocache()) {
                refreshCache = header.contains(HttpUtil.VALUE_NO_CACHE);
            }

            long ifModifiedSince = request.getDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE);

            ClientlibService.ClientlibInfo hints;
            try {
                hints = service.prepareContent(request, clientlibRef, minified, encoding, refreshCache, requestedHash,
                        ifModifiedSince);
            } catch (PersistenceException ex) {
                LOG.warn("2nd try for preparation of '" + clientlibRef + "' after exception: " + ex);
                // try it once more on concurrency problems (with a fresh resolver)
                hints = service.prepareContent(request, clientlibRef, minified, encoding, refreshCache, requestedHash,
                        ifModifiedSince);
            }

            if (null == hints) {
                LOG.warn("Not found: " + request.getRequestURI());
                throw new FileNotFoundException(request.getRequestURI());
            }

            if (hints.hash.equals(requestedHash)) {
                boolean notModified = false;
                if (hints.mimeType != null) {
                    response.setContentType(hints.mimeType + "; charset=" + ClientlibProcessor.DEFAULT_CHARSET);
                }
                if (hints.encoding != null) {
                    response.setHeader(HttpUtil.HEADER_CONTENT_ENCODING, hints.encoding);
                    response.setHeader(HttpUtil.HEADER_VARY, HttpUtil.HEADER_ACCEPT_ENCODING);
                }
                if (hints.size != null) {
                    response.setHeader(HttpUtil.HEADER_CONTENT_LENGTH, hints.size.toString());
                }
                if (hints.lastModified != null) {
                    long timeInMillis = hints.lastModified.getTimeInMillis();
                    response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, timeInMillis);
                    if (get) {
                        if (!HttpUtil.isModifiedSince(ifModifiedSince, timeInMillis)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Skipping retransmission because " + HttpConstants.HEADER_IF_MODIFIED_SINCE +
                                        "={} but modified {}", ifModifiedSince, timeInMillis);
                            }
                            notModified = true;
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Transmitting because " + HttpConstants.HEADER_IF_MODIFIED_SINCE +
                                        "={} but modified {}", ifModifiedSince, timeInMillis);
                            }
                        }
                    }
                }

                if (notModified) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                } else {
                    if (get) {
                        service.deliverContent(request.getResourceResolver(), clientlibRef, minified, response
                                .getOutputStream(), encoding);
                    }
                }
            } else {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                String url = makeUrl(request, hints.link, minified);
                response.setHeader(HttpUtil.HEADER_LOCATION, url);
                LOG.debug("Redirecting because of hash other than {} to {}", hints.hash, url);
            }

        } catch (FileNotFoundException e) { // thrown for unaccessible / not existent libs
            LOG.info("Could not deliver {} : {}", request.getRequestURI(), e.toString());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected String makeUrl(SlingHttpServletRequest request, ClientlibLink link, boolean minified) {
        String uri = makeUri(minified, link);
        String url;
        if (configuration.getMapClientlibURLs()) {
            url = LinkUtil.getUrl(request, uri);
        } else {
            url = LinkUtil.getUnmappedUrl(request, uri);
        }
        return url;
    }

    protected abstract String makeUri(boolean minified, ClientlibLink link);

    protected boolean isMinified(String selectors) {
        boolean minified = false;
        if (StringUtils.isNotBlank(selectors)) {
            minified = "min".equals(selectors);
            if (!minified) {
                LOG.warn("Cannot understand selectorstring {}", selectors);
            }
        }
        return minified;
    }

    protected boolean dropRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String uri = request.getRequestURI();
        if (uri.endsWith(".map")) { // empty response for maps to avoid error log entries on client
            DropMapServlet.drop(request, response);
            return true;
        }
        return false;
    }
}
