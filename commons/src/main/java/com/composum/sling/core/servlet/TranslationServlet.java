package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.logging.Message;
import com.composum.sling.core.service.TranslationService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.*;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Translation Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + TranslationServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                "sling.auth.requirements=" + TranslationServlet.SERVLET_PATH
        }
)
public class TranslationServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/core/translate";

    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String WARNING = "warning";

    public static final String TITLE = "title";
    public static final String MESSAGES = "messages";
    public static final String LEVEL = "level";
    public static final String CONTEXT = "context";
    public static final String LABEL = "label";
    public static final String TEXT = "text";
    public static final String HINT = "hint";

    public static final String DATA = "data";

    public static final String BOOTSTRAP_ERROR = "danger";

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
     * Translates (i18n) all strings in a given JSON.
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
     * Translates (i18n) status-like information into a Status.
     */
    protected class TranslateStatus implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nonnull final ResourceHandle resource)
                throws IOException {
            Status status = new Status(request, response);
            readFrom(request.getReader(), status);
            status.sendJson(SC_OK); // the normal i18n of Status and its Messages takes care of the translation.
        }

        /** Parses the request format. */
        protected void readFrom(BufferedReader reader, Status status) {
            Map<String, Object> data = status.getGson().fromJson(reader, Map.class);
            Object value;
            if ((value = data.get(TITLE)) != null) {
                status.setTitle(value.toString());
            }
            boolean success = false;
            if ((value = data.get(SUCCESS)) instanceof Boolean) {
                success = (Boolean) value;
                if (success) {
                    status.setStatus(SC_OK);
                } else {
                    status.setStatus(SC_BAD_REQUEST);
                    if (!status.hasTitle()) {
                        status.setTitle("Error");
                    }
                }
            }
            if ((value = data.get(WARNING)) instanceof Boolean) {
                status.setWarning((Boolean) value);
                if (success) {
                    status.setStatus(SC_ACCEPTED);
                }
                if (!status.hasTitle()) {
                    status.setTitle("Warning");
                }
            }
            if ((value = data.get(STATUS)) != null) {
                status.setStatus(value instanceof Integer ? (Integer) value : Integer.parseInt(value.toString()));
            }
            if ((value = data.get(MESSAGES)) instanceof Collection) {
                for (Object val : ((Collection<?>) value)) {
                    if (val instanceof Map) {
                        //noinspection rawtypes
                        status.addMessage(parseMessage((Map) val));
                    }
                }
            }

        }

        /** Reads a message from the input format. */
        protected Message parseMessage(Map<String, Object> data) {
            Object value;
            Object hint = data.get(HINT);
            Message.Level level = (value = data.get(LEVEL)) != null ? levelOf(value.toString()) : Message.Level.info;
            String context = (value = data.get(CONTEXT)) != null ? value.toString() : null;
            String label = (value = data.get(LABEL)) != null ? value.toString() : null;
            String text = (value = data.get(TEXT)) != null ? value.toString() : null;
            Message message = new Message(level, text, hint).setContext(context).setLabel(label);
            return message;
        }

        public static final String BOOTSTRAP_ERROR = "danger";

        @Nonnull
        public Message.Level levelOf(@Nonnull String name) {
            if (BOOTSTRAP_ERROR.equalsIgnoreCase(name)) {
                name = Message.Level.error.name();
            }
            return Message.Level.valueOf(name);
        }

    }
}
