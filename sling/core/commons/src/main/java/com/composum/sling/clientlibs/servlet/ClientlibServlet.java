package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.util.HttpUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

@SlingServlet(
        resourceTypes = Clientlib.RESOURCE_TYPE,
        extensions = {"js", "css"},
        methods = {"GET"}
)
public class ClientlibServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibServlet.class);

    @Reference
    protected ClientlibService clientlibService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            String path = pathInfo.getResourcePath();

            Clientlib.Type type = Clientlib.Type.valueOf(pathInfo.getExtension());

            Clientlib clientlib = new Clientlib(request, path, type);

            if (clientlib.isValid()) {
                deliverClientlib(request, response, clientlib);
            }

        } catch (RepositoryException | LoginException ex) {
            throw new ServletException(ex);
        }
    }

    protected void deliverClientlib(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                    Clientlib clientlib)
            throws RepositoryException, LoginException, IOException {

        String encoding = null;
        String header;

        header = request.getHeader(HttpUtil.HEADER_ACCEPT_ENCODING);
        if (StringUtils.isNotBlank(header) && header.contains("gzip")) {
            encoding = ClientlibService.ENCODING_GZIP;
        }

        header = request.getHeader(HttpUtil.HEADER_CACHE_CONTROL);
        if (StringUtils.isNotBlank(header)) {
            if (header.contains(HttpUtil.VALUE_NO_CACHE)) {

                clientlibService.resetContent(clientlib, encoding);
            }
        }

        Map<String, Object> hints;
        try {
            hints = clientlibService.prepareContent(request, clientlib, encoding);
        } catch (PersistenceException ex) {
            LOG.warn("2nd try for preparation of '" + clientlib.getPath() + "' after exception: " + ex.toString());
            // try it once more on concurrency problems (with a fresh resolver)
            hints = clientlibService.prepareContent(request, clientlib, encoding);
        }

        Object value;
        if ((value = hints.get(ResourceUtil.PROP_MIME_TYPE)) != null) {
            response.setContentType(value + "; charset=" + ClientlibProcessor.DEFAULT_CHARSET);
        }
        if ((value = hints.get(ResourceUtil.PROP_ENCODING)) != null) {
            response.setHeader(HttpUtil.HEADER_CONTENT_ENCODING, value.toString());
            response.setHeader(HttpUtil.HEADER_VARY, HttpUtil.HEADER_ACCEPT_ENCODING);
        }
        if ((value = hints.get("size")) != null) {
            response.setHeader(HttpUtil.HEADER_CONTENT_LENGTH, value.toString());
        }
        if ((value = hints.get(ResourceUtil.PROP_LAST_MODIFIED)) instanceof Calendar) {
            response.setDateHeader(HttpUtil.HEADER_LAST_MODIFIED, ((Calendar) value).getTimeInMillis());
        }

        clientlibService.deliverContent(clientlib, response.getOutputStream(), encoding);
    }
}
