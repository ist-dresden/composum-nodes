package com.composum.sling.nodes.servlet;

import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.update.SourceUpdateService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Modifies JCR content according to a given XML or ZIP of XMLs while preserving / updating metadata like versioning
 * information and metadata. This is a kind of opposite operation as the {@link SourceServlet}: the nodes like
 * jcr:lastModified, which are removed there, will just be updated here - if they occur in our input, they'll
 * be ignored. This is somewhat in beta stage - use at your own risk.
 * <p>
 * TODO: perhaps keep special nodes like cpp:MetaData (used for statistics) unchanged
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Source Update Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SourceUpdateServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=zip",
                "sling.auth.requirements=" + SourceUpdateServlet.SERVLET_PATH
        }
)
public class SourceUpdateServlet extends SlingAllMethodsServlet {

    public static final String SERVLET_PATH = "/bin/cpm/nodes/sourceupload";

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
        return nodesConfig.isEnabled(this);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (!isEnabled()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        RequestParameterMap parameters = request.getRequestParameterMap();
        RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);
        RequestPathInfo pathInfo = request.getRequestPathInfo();

        try {
            switch (pathInfo.getExtension()) {

                case "zip":
                    sourceUpdateService.updateFromZip(request.getResourceResolver(),
                            file.getInputStream(), XSS.filter(pathInfo.getSuffix()));

                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    break;

                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    break;
            }

        } catch (RepositoryException | TransformerException | RuntimeException | IOException ex) {
            LOG.error("Trouble during update: {}", ex, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}

