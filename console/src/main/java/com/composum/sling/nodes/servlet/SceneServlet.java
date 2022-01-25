package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.scene.Scene;
import com.composum.sling.nodes.scene.SceneConfigurations;
import com.composum.sling.nodes.scene.SceneConfigurations.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

import static com.composum.sling.nodes.servlet.SceneServlet.SERVICE_KEY;

@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Scene Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SceneServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                "sling.auth.requirements=" + SceneServlet.SERVLET_PATH
        }
)
@Restricted(key = SERVICE_KEY)
public class SceneServlet extends AbstractServiceServlet {

    public static final String SERVLET_PATH = "/bin/cpm/nodes/scene";

    public static final String SERVICE_KEY = "nodes/components/scenes";

    public static final String PARAM_SCENE = "scene";

    @Reference
    private ServiceRestrictions restrictions;

    @Reference
    protected NodesConfiguration nodesConfig;

    protected BundleContext bundleContext;

    //
    // Servlet operations
    //

    public enum Extension {json}

    public enum Operation {data, prepare, remove}

    protected ServletOperationSet<SceneServlet.Extension, SceneServlet.Operation> operations = new ServletOperationSet<>(SceneServlet.Extension.json);

    @NotNull
    protected ServletOperationSet<SceneServlet.Extension, SceneServlet.Operation> getOperations() {
        return operations;
    }

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, SceneServlet.Extension.json,
                Operation.data, new SceneDataOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, SceneServlet.Extension.json,
                Operation.prepare, new PrepareSceneOperation());
        operations.setOperation(ServletOperationSet.Method.POST, SceneServlet.Extension.json,
                Operation.remove, new RemoveSceneOperation());
    }

    protected abstract class SceneOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            final Status status = new Status(request, response);
            final String sceneId = request.getParameter(PARAM_SCENE);
            if (resource != null && StringUtils.isNotBlank(sceneId)) {
                try {
                    String sceneKey = StringUtils.substringBefore(sceneId, "/");
                    String toolId = StringUtils.substringAfter(sceneId, "/");
                    final Config sceneConfig =
                            SceneConfigurations.instance(request).getSceneConfig(sceneKey);
                    if (sceneConfig != null) {
                        final BeanContext context = new BeanContext.Servlet(
                                getServletContext(), bundleContext, request, response);
                        final Scene scene = new Scene(context, sceneConfig, resource.getPath());
                        applyScene(status, context, scene, toolId);
                    } else {
                        status.error("scene not available ({})", sceneId);
                    }
                } catch (IOException ex) {
                    status.error("an error has been occured", ex);
                }
            } else {
                status.error("values missed (path={},scene={})",
                        request.getRequestPathInfo().getSuffix(), sceneId);
            }
            status.sendJson();
        }

        protected abstract void applyScene(@NotNull Status status, @NotNull BeanContext context,
                                           @NotNull Scene scene, @NotNull String toolId)
                throws IOException;

        protected void answer(@NotNull final Status status,
                              @NotNull final Scene scene, @NotNull final String toolId) {
            Config config = scene.getConfig();
            Config.Tool tool = config.getTool(toolId);
            status.data("tool").put("name", toolId);
            if (scene.isContentPrepared()) {
                status.data("scene").put("prepared", true);
                status.data("scene").put("contentPath", scene.getContentPath());
                status.data("scene").put("elementPath", scene.getElementPath());
            }
            if (tool != null) {
                status.data("tool").put("frameUrl", scene.getFrameUrl(toolId));
            }
            status.data("config").put("key", config.getKey());
            status.data("config").put("path", config.getPath());
        }
    }

    protected class SceneDataOperation extends SceneOperation {

        @Override
        protected void applyScene(@NotNull final Status status, @NotNull final BeanContext context,
                                  @NotNull final Scene scene, @NotNull final String toolId) {
            answer(status, scene, toolId);
        }
    }

    protected class PrepareSceneOperation extends SceneOperation {

        @Override
        protected void applyScene(@NotNull final Status status, @NotNull final BeanContext context,
                                  @NotNull final Scene scene, @NotNull final String toolId)
                throws IOException {
            final SlingHttpServletRequest request = context.getRequest();
            final boolean reset = RequestUtil.getParameter(request, "reset", Boolean.FALSE);
            final Resource sceneContent = scene.prepareContent(reset);
            if (sceneContent != null) {
                request.getResourceResolver().commit();
                answer(status, scene, toolId);
            } else {
                status.error("no content available ({})", scene.getContentPath());
            }
        }
    }

    protected class RemoveSceneOperation extends SceneOperation {

        @Override
        protected void applyScene(@NotNull final Status status, @NotNull final BeanContext context,
                                  @NotNull final Scene scene, @NotNull final String toolId)
                throws IOException {
            final Resource sceneResource = scene.getContentResource();
            if (!ResourceUtil.isNonExistingResource(sceneResource)) {
                final SlingHttpServletRequest request = context.getRequest();
                ResourceResolver resolver = request.getResourceResolver();
                resolver.delete(sceneResource);
                resolver.commit();
            }
            answer(status, scene, toolId);
        }
    }
}
