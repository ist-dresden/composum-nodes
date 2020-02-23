package com.composum.sling.core.servlet;

import com.composum.sling.core.logging.Message;
import com.composum.sling.core.logging.Message.Level;
import com.composum.sling.core.logging.MessageContainer;
import com.composum.sling.core.logging.MessageTypeAdapterFactory;
import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
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

    private static final Logger LOG = LoggerFactory.getLogger(Status.class);

    /** Constant for often used argument for {@link #data(String)}. */
    public static final String DATA = "data";

    protected transient final Gson gson;
    protected transient final SlingHttpServletRequest request;
    protected transient final SlingHttpServletResponse response;

    protected int status = SC_OK;
    protected boolean success = true;
    protected boolean warning = false;

    /** If set, added messages will be logged here. */
    @Nullable
    protected transient Logger messageLogger;
    @Nullable
    protected String title;
    @Nullable
    protected MessageContainer messages;
    @Nullable
    protected Map<String, Map<String, Object>> data;
    @Nullable
    protected Map<String, List<Map<String, Object>>> list;

    public Status(@Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response,
                  @Nullable Logger messageLogger) {
        this(new GsonBuilder(), request, response, messageLogger);
    }

    /**
     * Constructor without logging messages - consider {@link #Status(SlingHttpServletRequest, SlingHttpServletResponse, Logger)}
     * to automatically log added messages.
     */
    public Status(@Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response) {
        this(new GsonBuilder(), request, response, null);
    }

    /** Construction */
    public Status(@Nonnull final GsonBuilder gsonBuilder,
                  @Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response,
                  @Nullable Logger messageLogger) {
        this.gson = initGson(Objects.requireNonNull(gsonBuilder), () -> request).create();
        this.request = request;
        this.response = response;
        this.messageLogger = messageLogger;
    }

    /**
     * Construct with a specific Gson instance. CAUTION: you need to have called
     * {@link #initGson(GsonBuilder, SlingHttpServletRequest)} for proper message translation.
     * Perhaps rather use {@link #Status(GsonBuilder, SlingHttpServletRequest, SlingHttpServletResponse)}.
     *
     * @deprecated since it's dangerous to forget {@link #initGson(GsonBuilder, SlingHttpServletRequest)}.
     */
    @Deprecated
    public Status(@Nonnull final Gson gson,
                  @Nullable final SlingHttpServletRequest request, @Nullable final SlingHttpServletResponse response) {
        this.gson = Objects.requireNonNull(gson);
        this.request = request;
        this.response = response;
    }

    /** @deprecated Constructor for deserialization with gson only */
    @Deprecated
    public Status() {
        this(new GsonBuilder().create(), null, null);
    }

    /**
     * Registers a type adapter for the JSON serialization of {@link Message} and {@link MessageContainer} that
     * performs i18n according to the given request. If there is no request, we can skip this step as these classes are
     * annotated with {@link com.google.gson.annotations.JsonAdapter}.
     */
    @Nonnull
    public static GsonBuilder initGson(@Nonnull GsonBuilder gsonBuilder,
                                       @Nonnull Supplier<SlingHttpServletRequest> requestProvider) {
        if (requestProvider != null) {
            return gsonBuilder.registerTypeAdapterFactory(new MessageTypeAdapterFactory(requestProvider));
        }
        return gsonBuilder;
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
        validationError(null, null, errorMessage, value);
        return null;
    }

    public List<String> getRequiredParameters(@Nonnull final String paramName,
                                              @Nullable final Pattern pattern, @Nonnull final String errorMessage) {
        final RequestParameter[] requestParameters = request.getRequestParameters(paramName);
        if (requestParameters == null || requestParameters.length < 1) {
            validationError(null, null, errorMessage);
            return null;
        }
        List<String> values = new ArrayList<>();
        for (RequestParameter parameter : requestParameters) {
            String value = parameter.getString();
            if (pattern != null && !pattern.matcher(value).matches()) {
                validationError(null, null, errorMessage, value);
            }
            values.add(value);
        }
        return values;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasTitle() {
        return StringUtils.isNotBlank(title);
    }

    public void setTitle(String rawTitle) {
        if (StringUtils.isNotBlank(rawTitle)) {
            this.title = request != null ? I18N.get(request, rawTitle) : rawTitle;
            if (StringUtils.isBlank(this.title)) {
                this.title = rawTitle;
            }
        } else {
            this.title = null;
        }
    }

    @Nonnull
    public MessageContainer getMessages() {
        if (messages == null) { messages = new MessageContainer(messageLogger); }
        return messages;
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

    public void setWarning(boolean value) {
        this.warning = value;
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

    /**
     * For the meaning of arguments - compare {@link #shortMessage(Level, String, Object...)} .
     * This adds the {@link Exception#getLocalizedMessage()} as message argument and logs the exception if
     * a message logger is set.
     */
    public void error(@Nonnull String text, @Nonnull Throwable exception) {
        getMessages().add(Message.error(text, exception.getLocalizedMessage()), exception);
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
        Map<String, Object> object = data.computeIfAbsent(name, k -> new LinkedHashMap<>());
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
        List<Map<String, Object>> object = list.computeIfAbsent(name, k -> new ArrayList<>());
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
     * Adds a message with minimum information.
     *
     * @param level the message level
     * @param text  non prepared text message, may contain {@literal {}} placeholders
     * @param args  argument objects for the log like message preparation of text
     * @return the created and added message, if you need to add more attributes
     */
    public Message shortMessage(@Nonnull final Level level, @Nonnull final String text, Object... args) {
        return addMessage(new Message(level, text, args));
    }

    /**
     * For use with validation messages: add context and label, too. Validation messages are logged at level debug.
     *
     * @param level   the message level
     * @param context non prepared context key
     * @param label   non prepared aspect label (validation label)
     * @param text    non prepared text message, can contain placeholders {@literal {}}
     * @return the created and added message, if you need to add more attributes
     * @see MessageFormat
     */
    @Nonnull
    public Message addValidationMessage(@Nonnull final Level level, @Nullable final String context,
                                        @Nullable final String label,
                                        @Nonnull final String text, Object... args) {
        return addMessage(new Message(level, text, args).setContext(context).setLabel(label).setLogLevel(Level.debug));
    }

    /**
     * Adds the message and returns it.
     *
     * @return the created and added message, if you need to add more attributes
     */
    @Nonnull
    public Message addMessage(@Nonnull final Message message) {
        if (messages == null) { messages = new MessageContainer(messageLogger); }
        if (message.getLevel() == Level.error) {
            status = SC_BAD_REQUEST;
            success = false;
            if (!hasTitle()) {
                setTitle("Error");
            }
        } else if (message.getLevel() == Level.warn && status == SC_OK) {
            status = SC_ACCEPTED; // 202 - accepted but there is a warning
            warning = true;
            if (!hasTitle()) {
                setTitle("Warning");
            }
        }
        messages.add(message);
        return message;
    }

    /** Writes this object as JSON using {@link Gson}. */
    public void toJson(@Nonnull final JsonWriter writer) throws IOException {
        try {
            gson.toJson(this, getClass(), writer);
        } catch (JsonIOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the JSON representation of this status as String, primarily for logging.
     * Usable for logging: never throws up even if JSON generation fails, though it's
     * obviously lacking data then.
     */
    @Nonnull
    public String getJsonString() {
        try (StringWriter statusString = new StringWriter()) {
            toJson(new JsonWriter(statusString));
            return statusString.toString();
        } catch (IOException | RuntimeException e) {
            LOG.error("Could not create JSON", e);
            try { // since this is primarily for logging, we try this as last resort.
                return ReflectionToStringBuilder.reflectionToString(this);
            } catch (RuntimeException e1) {
                LOG.error("Could not even create some representation via reflection. Giving up.", e1);
                return super.toString();
            }
        }
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending status {} {} {} : {}",
                    new Object[]{status,
                            success ? "success" : "failed",
                            warning ? " with warning" : "",
                            StringUtils.abbreviate(title, 256)});
        }
        toJson(writer);
    }

    @Nonnull
    public Gson getGson() {
        return gson;
    }

    /**
     * Sets the logger of the {@link #getMessages()} message container to logger. Thus, new added messages will also
     * be logged with this logger.
     * Please consider using the constructor
     * {@link #Status(SlingHttpServletRequest, SlingHttpServletResponse, Logger)} or
     * {@link #Status(GsonBuilder, SlingHttpServletRequest, SlingHttpServletResponse, Logger)}.
     *
     * @return this in builder-style
     */
    @Nonnull
    public Status setMessageLogger(@Nullable Logger messageLogger) {
        this.messageLogger = messageLogger;
        return this;
    }

    /**
     * Sets the logger of the {@link #getMessages()} message container to logger. Thus, new added messages will also
     * be logged with this logger.
     *
     * @return this in builder-style
     * @deprecated rather use the constructor
     * {@link #Status(SlingHttpServletRequest, SlingHttpServletResponse, Logger)},
     * {@link #Status(GsonBuilder, SlingHttpServletRequest, SlingHttpServletResponse, Logger)}
     * or, if need be, {@link #setMessageLogger(Logger)}.
     * This is kept for backwards compatibility for a while.
     */
    @Nonnull
    @Deprecated
    public Status withLogging(@Nonnull Logger logger) {
        return setMessageLogger(logger);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Status{");
        sb.append("status=").append(status);
        if (success) { sb.append(", success=").append(success); }
        if (warning) { sb.append(", warning=").append(warning); }
        if (StringUtils.isNotBlank(title)) { sb.append(", title='").append(title).append('\''); }
        sb.append('}');
        return sb.toString();
    }
}
