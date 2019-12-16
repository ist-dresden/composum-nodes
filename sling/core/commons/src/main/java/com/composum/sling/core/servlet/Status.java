package com.composum.sling.core.servlet;

import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.LoggerFormat;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.sling.api.resource.ResourceUtil.isSyntheticResource;

/**
 * The standardised answer object of a servlet request to fill the response output.
 * It allows adding generic objects and lists with the {@link #data(String)} / {@link #list(String)} methods.
 * Can be serialized and deserialized with {@link Gson}, so it is possible to extend this and add your own attributes, too.
 * (Caution: for deserializing it with Gson you might run into trouble if you put arbitrary objects into the data /
 * list - it's probably easier to extend Status and have attributes with the specific types needed.)
 */
public class Status {

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

    public enum Level {
        info, warn, error;

        @Nonnull
        public static Level levelOf(@Nonnull String name) {
            if (BOOTSTRAP_ERROR.equalsIgnoreCase(name)) {
                name = error.name();
            }
            return valueOf(name);
        }
    }

    public class Message {

        @Nonnull
        public final Level level;

        /** In validation messages: context of the validation, for instance the dialog tab. */
        @Nullable
        public final String context;

        /** In validation messages: label of the field which the message is about. */
        @Nullable
        public final String label;

        /** The text of the message, can contain {@literal {}} placeholders. */
        @Nonnull
        public final String text;

        /** @param text The text of the message, can contain {@literal {}} placeholders. */
        public Message(@Nonnull final Level level, @Nonnull final String text) {
            this(level, null, null, text);
        }

        /**
         * @param context In validation messages: context of the validation, for instance the dialog tab.
         * @param label   In validation messages: label of the field which the message is about.
         * @param text    The text of the message, can contain {@literal {}} placeholders.
         */
        public Message(@Nonnull final Level level,
                       @Nullable final String context, @Nullable final String label,
                       @Nonnull final String text) {
            this.level = level;
            this.context = context;
            this.label = label;
            this.text = text;
        }

        /**
         * the 'translate' constructor
         */
        public Message(Map<String, Object> data) {
            Object value;
            Object hint = data.get(HINT);
            level = (value = data.get(LEVEL)) != null ? Level.levelOf(value.toString()) : Level.info;
            context = (value = data.get(CONTEXT)) != null ? prepare(value.toString()) : null;
            label = (value = data.get(LABEL)) != null ? prepare(value.toString()) : null;
            text = (value = data.get(TEXT)) != null
                    ? (hint != null ? prepare(value.toString(), hint) : prepare(value.toString()))
                    : null;
        }

        @SuppressWarnings("Duplicates")
        public void toJson(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name(LEVEL).value(level.name());
            if (StringUtils.isNotBlank(context)) {
                writer.name(CONTEXT).value(context);
            }
            if (StringUtils.isNotBlank(label)) {
                writer.name(LABEL).value(label);
            }
            writer.name(TEXT).value(text);
            writer.endObject();
        }
    }

    protected transient final Gson gson;
    protected transient final SlingHttpServletRequest request;
    protected transient final SlingHttpServletResponse response;

    protected int status = SC_OK;
    protected boolean success = true;
    protected boolean warning = false;

    @Nullable
    protected String title;
    @Nullable
    protected List<Message> messages;
    @Nullable
    protected Map<String, Map<String, Object>> data;
    @Nullable
    protected Map<String, List<Map<String, Object>>> list;

    public Status(@Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response) {
        this(new GsonBuilder().create(), request, response);
    }

    public Status(@Nonnull final Gson gson,
                  @Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response) {
        this.gson = gson;
        this.request = request;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        if (status < 200 || status >= 300) {
            success = false;
        }
    }

    /**
     * Retrieves and validates a required parameter from the request, adding the errorMessage if that fails.
     *
     * @return the validated parameter value, or null if that fails
     */
    @Nullable
    public String getRequiredParameter(@Nonnull final String paramName,
                                       @Nullable final Pattern pattern, @Nonnull final String errorMessage) {
        final RequestParameter requestParameter = request.getRequestParameter(paramName);
        String value = null;
        if (requestParameter != null) {
            value = requestParameter.getString();
            if (pattern == null || pattern.matcher(value).matches()) {
                return value;
            }
        }
        shortMessage(Level.error, errorMessage, value);
        return null;
    }

    public List<String> getRequiredParameters(@Nonnull final String paramName,
                                              @Nullable final Pattern pattern, @Nonnull final String errorMessage) {
        final RequestParameter[] requestParameters = request.getRequestParameters(paramName);
        if (requestParameters == null || requestParameters.length < 1) {
            shortMessage(Level.error, errorMessage);
            return null;
        }
        List<String> values = new ArrayList<>();
        for (RequestParameter parameter : requestParameters) {
            String value = parameter.getString();
            if (pattern != null && !pattern.matcher(value).matches()) {
                shortMessage(Level.error, errorMessage, value);
            }
            values.add(value);
        }
        return values;
    }

    public String getTitle() {
        return hasTitle() ? title : prepare("Result");
    }

    public boolean hasTitle() {
        return StringUtils.isNotBlank(title);
    }

    public void setTitle(String title) {
        this.title = title != null ? prepare(title) : null;
    }

    public boolean isValid() {
        return isSuccess();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isWarning() {
        return warning;
    }

    public boolean isError() {
        return !isSuccess();
    }

    /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
    public void info(@Nonnull final String text, Object... args) {
        shortMessage(Level.info, text, args);
    }

    /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
    public void validationInfo(@Nonnull final String context, @Nonnull final String label,
                               @Nonnull final String text, Object... args) {
        addValidationMessage(Level.info, context, label, text, args);
    }

    /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
    public void warn(@Nonnull final String text, Object... args) {
        shortMessage(Level.warn, text, args);
    }

    /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
    public void validationWarn(@Nonnull final String context, @Nonnull final String label,
                               @Nonnull final String text, Object... args) {
        addValidationMessage(Level.warn, context, label, text, args);
    }

    /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
    public void error(@Nonnull final String text, Object... args) {
        shortMessage(Level.error, text, args);
    }

    /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
    public void validationError(@Nonnull final String context, @Nonnull final String label,
                                @Nonnull final String text, Object... args) {
        addValidationMessage(Level.error, context, label, text, args);
    }

    /**
     * Returns a map that is saved with the given name in the data section of the Status, creating it if necessary.
     *
     * @param name the key of the data element to insert in the status response
     * @return a Map for the data values of the added status object, created if neccesary.
     */
    @Nonnull
    public Map<String, Object> data(@Nonnull final String name) {
        if (data == null) { data = new LinkedHashMap<>();}
        Map<String, Object> object = data.get(name);
        if (object == null) {
            object = new LinkedHashMap<>();
            data.put(name, object);
        }
        return object;
    }

    /**
     * Adds information about resource as {@link #data(String)} object.
     *
     * @param name the key of the data element to insert in the status response
     * @see #reference(Map, Resource)
     */
    public void reference(@Nonnull final String name, Resource resource) {
        Map<String, Object> object = data(name);
        reference(object, resource);
    }

    /**
     * Adds general information about the resource into the object: name, path, type (Sling resourcetype),
     * prim (primary type), synthetic (whether it is a synthetic resource).
     */
    public void reference(Map<String, Object> object, Resource resource) {
        object.put("name", resource.getName());
        object.put("path", resource.getPath());
        object.put("type", resource.getResourceType());
        object.put("prim", resource.adaptTo(ValueMap.class).get(JcrConstants.JCR_PRIMARYTYPE, ""));
        object.put("synthetic", isSyntheticResource(resource));
    }

    /**
     * Returns the list for the given name, creating it if neccesary.
     *
     * @param name the key of the data element to insert in the status response
     * @return a new list of Map items for the data values of the added status object
     */
    @Nonnull
    public List<Map<String, Object>> list(@Nonnull final String name) {
        if (list == null) { list = new LinkedHashMap<>(); }
        List<Map<String, Object>> object = list.get(name);
        if (object == null) {
            object = new ArrayList<>();
            list.put(name, object);
        }
        return object;
    }

    /**
     * Adds a list of resources in a request with common information. If there was already a list with that name,
     * the items are added to it.
     *
     * @param name the key of the data element to insert in the status response
     */
    public void list(@Nonnull final String name, Collection<Resource> items) {
        List<Map<String, Object>> object = list(name);
        for (Resource item : items) {
            Map<String, Object> ref = new HashMap<>();
            reference(ref, item);
            object.add(ref);
        }
    }

    /**
     * @param level the message level
     * @param text  non prepared text message, may contain {@literal {}} placeholders
     * @param args  argument objects for the log like message preparation of text
     */
    public void shortMessage(@Nonnull final Level level, @Nonnull final String text, Object... args) {
        addValidationMessage(level, null, null, prepare(text, args));
    }

    /**
     * For use with validation messages: add context and label, too.
     *
     * @param level   the message level
     * @param context non prepared context key
     * @param label   non prepared aspect label (validation label)
     * @param text    non prepared text message, can contain placeholders {@literal {}}
     * @see MessageFormat
     */
    public void addValidationMessage(@Nonnull final Level level, @Nullable final String context, @Nullable final String label,
                                     @Nonnull final String text, Object... args) {
        addMessage(new Message(level, prepare(context), prepare(label), prepare(text, args)));
    }

    public void addMessage(@Nonnull final Message message) {
        if (messages == null) { messages = new ArrayList<>(); }
        if (message.level == Level.error) {
            status = SC_BAD_REQUEST;
            success = false;
            if (!hasTitle()) {
                setTitle("Error");
            }
        } else if (message.level == Level.warn && status == SC_OK) {
            status = SC_ACCEPTED; // 202 - accepted but there is a warning
            warning = true;
            if (!hasTitle()) {
                setTitle("Warning");
            }
        }
        messages.add(message);
    }

    /**
     * @param text a text for an internationalized message
     * @param args the arguments for the text placeholders
     * @return the internationalized text
     * @see MessageFormat
     */
    public String prepare(@Nullable String text, Object... args) {
        if (text != null) {
            text = I18N.get(request, text);
            if (args != null && args.length > 0) {
                text = LoggerFormat.format(text, args);
            }
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    public void translate(@Nonnull final Reader reader) {
        Map<String, Object> data = gson.fromJson(reader, Map.class);
        Object value;
        if ((value = data.get(TITLE)) != null) {
            setTitle(prepare(value.toString()));
        }
        if ((value = data.get(SUCCESS)) instanceof Boolean) {
            success = (Boolean) value;
            if (success) {
                status = SC_OK;
            } else {
                status = SC_BAD_REQUEST;
                if (!hasTitle()) {
                    setTitle("Error");
                }
            }
        }
        if ((value = data.get(WARNING)) instanceof Boolean) {
            warning = (Boolean) value;
            if (success) {
                status = SC_ACCEPTED;
            }
            if (!hasTitle()) {
                setTitle("Warning");
            }
        }
        if ((value = data.get(STATUS)) != null) {
            setStatus(value instanceof Integer ? (Integer) value : Integer.parseInt(value.toString()));
        }
        if ((value = data.get(MESSAGES)) instanceof Collection) {
            for (Object val : ((Collection<?>) value)) {
                if (val instanceof Map) {
                    //noinspection rawtypes
                    addMessage(new Message((Map) val));
                }
            }
        }
    }

    /** Writes this object as JSON using {@link Gson}. */
    public void toJson(@Nonnull final JsonWriter writer) throws IOException {
        gson.toJson(this, getClass(), writer);
    }

    /** Serializes the status message and writes it in the response, using {@link #getStatus()} as HTTP status. */
    public void sendJson() throws IOException {
        sendJson(getStatus());
    }

    /** Serializes the status message and writes it in the response, using the given HTTP status. */
    public void sendJson(int status) throws IOException {
        JsonWriter writer = ResponseUtil.getJsonWriter(response);
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        toJson(writer);
    }

    /**
     * Shortcut: if you want to log the same message to a logger as you want to present to the user - call the methods here.
     * E.g. <code>status.withLogging(LOG).error("Some error happened in {}", path);</code>.
     */
    @Nonnull
    public StatusWithLogging withLogging(@Nonnull Logger logger) {
        return new StatusWithLogging(logger);
    }

    /** Wrapper that allows adding messages *and* logging them at the same time. Temporary object only - see {@link #withLogging(Logger)}. */
    public class StatusWithLogging {

        @Nonnull
        private final Logger logger;

        public StatusWithLogging(@Nonnull Logger logger) {
            this.logger = logger;
        }

        /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
        public void info(@Nonnull String text, Object... args) {
            Status.this.info(text, args);
            logger.info(text, args);
        }

        /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
        public void validationInfo(@Nonnull String context, @Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.validationInfo(context, label, text, args);
            logger.info(text, args);
        }

        /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
        public void warn(@Nonnull String text, Object... args) {
            Status.this.warn(text, args);
            logger.warn(text, args);
        }

        /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
        public void validationWarn(@Nonnull String context, @Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.validationWarn(context, label, text, args);
            logger.warn(text, args);
        }

        /** For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} . */
        public void error(@Nonnull String text, Object... args) {
            Status.this.error(text, args);
            logger.error(text, args);
        }

        public void error(@Nonnull String text, @Nonnull Throwable ex) {
            Status.this.error(text, ex.getLocalizedMessage());
            logger.error(prepare(text, ex.getLocalizedMessage()), ex);
        }

        /** For the meaning of arguments - compare {@link #addValidationMessage(Level, String, String, String, Object...)} . */
        public void validationError(@Nonnull String context, @Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.validationError(context, label, text, args);
            logger.error(text, args);
        }
    }
}
