package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

@SlingServlet(
        resourceTypes = "composum/sling/commons/clientlib",
        extensions = {"js", "css", "link"},
        methods = {"GET"}
)
public class ClientlibServlet extends SlingSafeMethodsServlet {

    public static final String TYPE_CLIENTLIB = "composum/sling/commons/clientlib";

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

            String encoding = null;
            String header;

            header = request.getHeader("Accept-Encoding");
            if (StringUtils.isNotBlank(header) && header.contains("gzip")) {
                encoding = ClientlibService.ENCODING_GZIP;
            }

            header = request.getHeader("Cache-Control");
            if (StringUtils.isNotBlank(header)) {
                if (header.contains("no-cache")) {

                    clientlibService.resetContent(clientlib, encoding);
                }
            }

            Map<String, Object> hints = clientlibService.prepareContent(clientlib, encoding);

            Object value;
            if ((value = hints.get(ResourceUtil.PROP_MIME_TYPE)) != null) {
                response.setContentType(value + "; charset=" + ClientlibProcessor.DEFAULT_CHARSET);
            }
            if ((value = hints.get(ResourceUtil.PROP_ENCODING)) != null) {
                response.setHeader("Content-Encoding", value.toString());
                response.setHeader("Vary", "Accept-Encoding");
            }
            if ((value = hints.get("size")) != null) {
                response.setHeader("Content-Length", value.toString());
            }
            if ((value = hints.get(ResourceUtil.PROP_LAST_MODIFIED)) instanceof Calendar) {
                response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, ((Calendar) value).getTimeInMillis());
            }

            clientlibService.deliverContent(clientlib, response.getWriter(), encoding);

        } catch (RepositoryException | LoginException ex) {
            throw new ServletException(ex);
        }
    }
}
