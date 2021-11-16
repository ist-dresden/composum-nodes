package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.RepositorySetupService;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.impl.CoreRepositorySetupService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashMap;
import java.util.Map;

import static com.composum.sling.core.servlet.SetupServlet.SERVICE_KEY;

/**
 * The service servlet to execute setup operations.
 */
@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Setup Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SetupServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                "sling.auth.requirements=" + SetupServlet.SERVLET_PATH
        }
)
@Restricted(key = SERVICE_KEY)
public class SetupServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SetupServlet.class);

    public static final String SERVICE_KEY = "core/setup/execution";

    public static final String SERVLET_PATH = "/bin/cpm/nodes/setup";

    public static final String PN_WARNINGS = "warnings";
    public static final String PN_ERRORS = "errors";

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
    @NotNull
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
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
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response,
                         final ResourceHandle resource)
                throws ServletException, IOException {
            response.setContentType("text/plain;charset=utf-8");
            final PrintWriter writer = response.getWriter();
            int sumErrors = 0;
            int sumWarnings = 0;
            try {
                String[] scripts = request.getParameterValues("script");
                if (scripts != null) {
                    ResourceResolver resolver = request.getResourceResolver();
                    Session session = resolver.adaptTo(Session.class);
                    if (session != null) {
                        for (String script : scripts) {
                            writer.println("running script '" + script + "'...");
                            final Map<String, Object> status = new HashMap<String, Object>() {{
                                put(PN_WARNINGS, 0);
                                put(PN_ERRORS, 0);
                            }};
                            try {
                                CoreRepositorySetupService.TRACKER.set(new CoreRepositorySetupService.Tracker() {

                                    @Override
                                    public void info(String message) {
                                        writer.println("I " + message);
                                    }

                                    @Override
                                    public void warn(String message) {
                                        status.put(PN_WARNINGS, ((int) status.get(PN_WARNINGS)) + 1);
                                        writer.println("W " + message);
                                    }

                                    @Override
                                    public void error(String message) {
                                        status.put(PN_ERRORS, ((int) status.get(PN_ERRORS)) + 1);
                                        writer.println("E " + message);
                                    }
                                });
                                setupService.addJsonAcl(session, script, null);
                            } finally {
                                CoreRepositorySetupService.TRACKER.set(null);
                            }
                            session.save();
                            int warnings = (int) status.get(PN_WARNINGS);
                            int errors = (int) status.get(PN_ERRORS);
                            sumWarnings += warnings;
                            sumErrors += errors;
                            writer.println("done script '" + script +
                                    "' (warnings: " + warnings + ", errors: " + errors + ").");
                        }
                    }
                }
            } catch (Exception ex) {
                sumErrors++;
                LOG.error(ex.getMessage(), ex);
                writer.println("ERROR: " + ex);
            }
            writer.println("finished (warnings: " + sumWarnings + ", errors: " + sumErrors + ").");
            writer.flush();
        }
    }
}
