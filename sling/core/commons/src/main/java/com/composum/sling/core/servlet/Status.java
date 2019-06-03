package com.composum.sling.core.servlet;

import com.composum.sling.core.util.LoggerFormat;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.cpnl.CpnlElFunctions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * the standardised answer object of a servlet request to fill the response output
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

    public enum Level {info, warn, error}

    public class Message {

        public final Level level;
        public final String context;
        public final String label;
        public final String text;
        public final String hint;

        public Message(@Nonnull final Level level, @Nonnull final String text) {
            this(level, null, null, text, null);
        }

        public Message(@Nonnull final Level level, @Nonnull final String text, @Nonnull final String hint) {
            this(level, null, null, text, hint);
        }

        public Message(@Nonnull final Level level,
                       @Nullable final String context, @Nullable final String label,
                       @Nonnull final String text, @Nullable final String hint) {
            this.level = level;
            this.context = context;
            this.label = label;
            this.text = text;
            this.hint = hint;
        }

        /** the 'translate' constructor */
        public Message(Map<String, Object> data) {
            Object value;
            level = (value = data.get(LEVEL)) != null ? Level.valueOf(value.toString()) : Level.info;
            context = (value = data.get(CONTEXT)) != null ? prepare(value.toString()) : null;
            label = (value = data.get(LABEL)) != null ? prepare(value.toString()) : null;
            text = (value = data.get(TEXT)) != null ? prepare(value.toString()) : null;
            hint = (value = data.get(HINT)) != null ? prepare(value.toString()) : null;
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
            if (StringUtils.isNotBlank(hint)) {
                writer.name(HINT).value(hint);
            }
            writer.endObject();
        }
    }

    protected final Gson gson;
    protected final SlingHttpServletRequest request;
    protected final SlingHttpServletResponse response;

    protected int status = SC_OK;
    protected boolean success = true;
    protected boolean warning = false;

    protected String title;
    protected final List<Message> messages;
    protected final Map<String, Map<String, Object>> data;

    public Status(@Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response) {
        this(new GsonBuilder().create(), request, response);
    }

    public Status(@Nonnull final Gson gson,
                  @Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response) {
        this.gson = gson;
        this.request = request;
        this.response = response;
        data = new HashMap<>();
        messages = new ArrayList<>();
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

    public void info(@Nonnull final String text, Object... args) {
        addMessage(Level.info, text, args);
    }

    public void warn(@Nonnull final String text, Object... args) {
        addMessage(Level.warn, text, args);
    }

    public void warn(@Nonnull final String label, @Nonnull final String text, Object... args) {
        addMessage(Level.warn, null, label, prepare(text, args), null);
    }

    public void warn(@Nonnull final String context, @Nonnull final String label,
                     @Nonnull final String text, Object... args) {
        addMessage(Level.warn, context, label, text);
    }

    public void error(@Nonnull final String text, Object... args) {
        addMessage(Level.error, text);
    }

    public void error(@Nonnull final String label, @Nonnull final String text, Object... args) {
        addMessage(Level.error, null, label, prepare(text, args), null);
    }

    public void error(@Nonnull final String context, @Nonnull final String label,
                      @Nonnull final String text, Object... args) {
        addMessage(Level.error, label, context, text);
    }

    /**
     * @param name the key of the data element to insert in the status response
     * @return a new Map for the data values of the added status object
     */
    @Nonnull
    public Map<String, Object> data(@Nonnull final String name) {
        Map<String, Object> object = new LinkedHashMap<>();
        data.put(name, object);
        return object;
    }

    /**
     * @param level the message level
     * @param text  non prepared text message
     * @param args  argument objects for the log like message preparation
     */
    public void addMessage(@Nonnull final Level level, @Nonnull final String text, Object... args) {
        addMessage(level, null, null, prepare(text, args), null);
    }

    /**
     * @param level   the message level
     * @param context non prepared context key
     * @param label   non prepared aspect label (validation label)
     * @param text    pre prepared text message
     * @param hint    pre prepared hint message
     */
    public void addMessage(@Nonnull final Level level, @Nullable final String context, @Nullable final String label,
                           @Nonnull final String text, @Nullable final String hint) {
        addMessage(new Message(level, prepare(context), prepare(label), text, hint));
    }

    public void addMessage(@Nonnull final Message message) {
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
     */
    public String prepare(@Nullable String text, Object... args) {
        if (text != null) {
            text = CpnlElFunctions.i18n(request, text);
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
        if ((value = data.get(MESSAGES)) instanceof Collection) {
            for (Object val : ((Collection<?>) value)) {
                if (val instanceof Map) {
                    addMessage(new Message((Map) val));
                }
            }
        }
    }

    public void toJson(@Nonnull final JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(STATUS).value(getStatus());
        writer.name(SUCCESS).value(isSuccess());
        writer.name(WARNING).value(isWarning());
        writer.name(TITLE).value(getTitle());
        writer.name(MESSAGES).beginArray();
        for (Message message : messages) {
            message.toJson(writer);
        }
        writer.endArray();
        for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
            writer.name(entry.getKey());
            gson.toJson(entry.getValue(), Map.class, writer);
        }
        writer.endObject();
    }

    public void sendJson() throws IOException {
        JsonWriter writer = ResponseUtil.getJsonWriter(response);
        response.setStatus(getStatus());
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

    public class StatusWithLogging {

        @Nonnull
        private final Logger logger;

        public StatusWithLogging(@Nonnull Logger logger) {
            this.logger = logger;
        }

        public void info(@Nonnull String text, Object... args) {
            Status.this.info(text, args);
            logger.info(text, args);
        }

        public void warn(@Nonnull String text, Object... args) {
            Status.this.warn(text, args);
            logger.warn(text, args);
        }

        public void warn(@Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.warn(label, text, args);
            logger.warn(text, args);
        }

        public void warn(@Nonnull String context, @Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.warn(context, label, text, args);
            logger.warn(text, args);
        }

        public void error(@Nonnull String text, Object... args) {
            Status.this.error(text, args);
            logger.error(text, args);
        }

        public void error(@Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.error(label, text, args);
            logger.error(text, args);
        }

        public void error(@Nonnull String context, @Nonnull String label, @Nonnull String text, Object... args) {
            Status.this.error(context, label, text, args);
            logger.error(text, args);
        }
    }
}
