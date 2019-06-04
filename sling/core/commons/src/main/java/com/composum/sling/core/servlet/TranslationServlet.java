package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.service.TranslationService;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@SlingServlet(
        paths = "/bin/cpm/core/translate",
        methods = {"PUT"}
)
public class TranslationServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationServlet.class);

    public enum Extension {json}

    public enum Operation {object, status}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private TranslationService translationService;

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override
    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.object, new TranslateObject());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.status, new TranslateStatus());
    }

    /**
     * Gets a part of the named temp. outputfile.
     */
    protected class TranslateObject implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nonnull final ResourceHandle resource)
                throws IOException {
            response.setStatus(SC_OK);
            response.setContentType("application/json; charset=UTF-8");
            translationService.translate(request, request.getReader(), response.getWriter());
        }
    }

    /**
     * Gets a part of the named temp. outputfile.
     */
    protected class TranslateStatus implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nonnull final ResourceHandle resource)
                throws IOException {
            Status status = new Status(request, response);
            status.translate(request.getReader());
            status.sendJson(SC_OK);
        }
    }
}
