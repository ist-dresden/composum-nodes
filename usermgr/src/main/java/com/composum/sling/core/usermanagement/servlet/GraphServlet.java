package com.composum.sling.core.usermanagement.servlet;

import com.composum.sling.core.usermanagement.model.AuthorizableModel;
import com.composum.sling.core.usermanagement.model.AuthorizablesGraph;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSFilter;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * a servlet to render authorizable graphs built from scratch to support extraction for external resuse
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Authorizable Graph Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + GraphServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=html",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                "sling.auth.requirements=" + GraphServlet.SERVLET_PATH
        }
)
public class GraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServlet.class);

    public static final String MANAGER_PATH = "/bin/users.html";
    public static final String SERVLET_PATH = "/bin/cpm/users/graph";
    public static final String COMPONENT_BASE = "/libs/composum/nodes/usermgnt/graph/";

    public static final URLCodec URL_CODEC = new URLCodec();

    @Reference
    protected XSSFilter xssFilter;

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        ResourceResolver resolver = request.getResourceResolver();
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String ext = pathInfo.getExtension();
        if (StringUtils.isNotBlank(ext)) {
            switch (ext) {
                default:
                case "html":
                    final String[] selectors = pathInfo.getSelectors();
                    String option = (selectors.length > 0 ? selectors[0] : "").toLowerCase();
                    switch (option) {
                        case "graphviz":
                            try {
                                final String context = selectors.length > 1 ? selectors[1] : "page";
                                Resource config = resolver.getResource(COMPONENT_BASE + context + "/" + JcrConstants.JCR_CONTENT);
                                final AuthorizablesGraph graph = getGraph(request);
                                response.setContentType("text/html;charset=UTF-8");
                                graph.toGraphviz(response.getWriter(), config, node -> nodeUrl(request, context, node));
                            } catch (RepositoryException ex) {
                                LOG.error(ex.getMessage(), ex);
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                            }
                            return;
                        default:
                            option = "page";
                        case "page":
                        case "view":
                            final RequestDispatcherOptions options = new RequestDispatcherOptions();
                            options.setForceResourceType("composum/nodes/usermgnt/graph/" + option);
                            final RequestDispatcher dispatcher =
                                    request.getRequestDispatcher(request.getResource(), options);
                            if (dispatcher != null) {
                                dispatcher.forward(request, response);
                                return;
                            }
                            break;
                    }
                case "json":
                    try {
                        final AuthorizablesGraph graph = getGraph(request);
                        response.setContentType("text/html;charset=UTF-8");
                        graph.toJson(new JsonWriter(response.getWriter()));
                    } catch (RepositoryException ex) {
                        LOG.error(ex.getMessage(), ex);
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                    }
                    return;
            }
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    protected AuthorizablesGraph getGraph(@NotNull final SlingHttpServletRequest request)
            throws RepositoryException {
        String type = xssFilter.filter(request.getParameter("type"));
        String name = xssFilter.filter(request.getParameter("name"));
        String path = xssFilter.filter(request.getParameter("path"));
        return new AuthorizablesGraph(request.getResourceResolver(), type, name, path);
    }

    protected String nodeUrl(@NotNull final SlingHttpServletRequest request,
                             @NotNull final String option, @NotNull final AuthorizableModel node) {
        StringBuilder url = new StringBuilder();
        try {
            StringBuilder params = new StringBuilder(request.getContextPath());
            switch (option) {
                default:
                case "page":
                    String type = xssFilter.filter(request.getParameter("type"));
                    if (StringUtils.isNotBlank(type)) {
                        params.append(params.length() < 1 ? '?' : '&').append("type=").append(URL_CODEC.encode(type));
                    }
                    params.append(params.length() < 1 ? '?' : '&').append("name=").append(URL_CODEC.encode(node.getId()));
                    String path = xssFilter.filter(request.getParameter("path"));
                    if (StringUtils.isNotBlank(path)) {
                        params.append(params.length() < 1 ? '?' : '&').append("path=").append(URL_CODEC.encode(path));
                    }
                    url.append(SERVLET_PATH).append(".page.html");
                    url.append(params);
                    break;
                case "view":
                    url.append(MANAGER_PATH).append(node.getPath());
                    break;
            }
        } catch (EncoderException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return url.toString();
    }
}
