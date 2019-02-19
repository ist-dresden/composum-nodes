package com.composum.sling.nodes.servlet;

import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.update.SourceUpdateService;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;


/**
 * Modifies JCR content according to a given XML or ZIP of XMLs while preserving / updating metadata like versioning
 * information and metadata. This is a kind of opposite operation as the {@link SourceServlet}: the nodes like
 * jcr:lastModified, which are removed there, will just be updated here - if they occur in our input, they'll
 * be ignored.
 * <p>
 * FIXME correct path? (Is it neccesary to set permissions somewhere?)
 */
@SlingServlet(
        paths = "/bin/cpm/nodes/debug/sourceupload",
        methods = {HttpConstants.METHOD_GET, HttpConstants.METHOD_POST},
        extensions = "xml"
)
public class SourceUpdateServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SourceUpdateServlet.class);

    @Reference
    protected NodesConfiguration nodesConfig;

    @Reference
    protected SourceUpdateService sourceUpdateService;

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected boolean isEnabled() {
        // FIXME implement
        // return nodesConfig.isEnabled(this);
        return true;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        LOG.error("DOGET");
        throw new UnsupportedOperationException("Not implemented yet: SourceUpdateServlet.doGet");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.error("DOPOST");

        if (!isEnabled()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        Resource resource = null;
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String resourcePath = pathInfo.getSuffix();
        if (StringUtils.isNotBlank(resourcePath)) {
            ResourceResolver resolver = request.getResourceResolver();
            resource = resolver.getResource(resourcePath);
        }

        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {

            try {
                String name = resource.getName();
                switch (pathInfo.getExtension()) {

                    // a single page or a node in its XML source representation
                    case "xml":
                        RequestParameterMap parameters = request.getRequestParameterMap();
                        RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);

                        sourceUpdateService.updateFromXml(resource, file.getInputStream());

                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        break;

                    default:
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        break;
                }

            } catch (RepositoryException | SAXException | ParserConfigurationException | TransformerException ex) {
                throw new ServletException(ex);
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
