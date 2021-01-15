package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.service.RepositorySetupService;
import com.composum.sling.core.service.impl.CoreRepositorySetupService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The service servlet to execute setup operations.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Setup Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SetupServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                "sling.auth.requirements=" + SetupServlet.SERVLET_PATH
        }
)
public class SetupServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SetupServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/nodes/setup";

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private RepositorySetupService setupService;

    //
    // Servlet operations
    //

    public enum Extension {txt}

    public enum Operation {run}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.txt);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.txt, Operation.run, new RunSetupScripts());
    }

    public class RunSetupScripts implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws ServletException, IOException {
            response.setContentType("text/plain;charset=utf-8");
            final PrintWriter writer = response.getWriter();
            try {
                String[] scripts = request.getParameterValues("script");
                if (scripts != null) {
                    ResourceResolver resolver = request.getResourceResolver();
                    Session session = resolver.adaptTo(Session.class);
                    if (session != null) {
                        CoreRepositorySetupService.TRACKER.set(new CoreRepositorySetupService.Tracker() {

                            @Override
                            public void info(String message) {
                                writer.println("I " + message);
                            }

                            @Override
                            public void warn(String message) {
                                writer.println("W " + message);
                            }

                            @Override
                            public void error(String message) {
                                writer.println("E " + message);
                            }
                        });
                        try {
                            for (String script : scripts) {
                                writer.println("running script '" + script + "'...");
                                setupService.addJsonAcl(session, script, null);
                                writer.println("done script '" + script + "'.");
                            }
                        } finally {
                            CoreRepositorySetupService.TRACKER.set(null);
                        }
                    }
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                writer.println("ERROR: " + ex.toString());
            }
            writer.flush();
        }
    }
}
