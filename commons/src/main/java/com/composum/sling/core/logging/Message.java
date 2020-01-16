package com.composum.sling.core.logging;

import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.LoggerFormat;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A container for a message, e.g. about internal processes, that can be presented to the user. It could be localized,
 * and there might be details which can be suppressed, depending on the user / user settings or choices.
 * <p>
 * For proper i18n in JSON serialization you need to register
 * {@link MessageTypeAdapterFactory#MessageTypeAdapterFactory(SlingHttpServletRequest)} with
 * {@link com.google.gson.GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)}.
 * The default {@link JsonAdapter} is just a fallback that uses no i18n.
 * CAUTION: careful when extending this class - the {@link MessageTypeAdapterFactory} might not work for derived classes.
 */
@JsonAdapter(MessageTypeAdapterFactory.class)
public class Message implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    /** @see #getLevel() */
    protected Level level;

    /**
     * If set, an i18n-ed version of {@link #rawMessage} with all placeholders replaced. This is modified during
     * JSON-serialization.
     */
    protected String message;

    /**
     * The raw version of the message which can contain {@literal {}}-placeholders and is used as
     * i18n-key.
     */
    protected String rawMessage;

    /** @see #getArguments() */
    protected Object[] arguments;

    /** @see #getCategory() */
    @Nullable
    protected String category;

    /** @see #getContext() */
    @Nullable
    protected String context;

    /** @see #getLabel() */
    @Nullable
    protected String label;

    /** @see #getDetails() */
    @Nullable
    protected List<Message> details;

    /** @see #getTimestamp() */
    protected Long timestamp;

    /** Saves whether the message was already {@link #i18n(SlingHttpServletRequest)}-ized. */
    protected transient boolean i18lized;

    /** @deprecated only for JSON deserialization. */
    @Deprecated
    public Message() {
        // empty
    }

    /**
     * Creates a message.
     *
     * @param level      the level of the message, default {@link Level#info}
     * @param rawMessage the message, possibly with placeholders {@literal {}} for arguments
     * @param arguments  optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                   transmitted with JSON!
     */
    public Message(@Nullable Level level, @Nonnull String rawMessage, Object... arguments) {
        this.rawMessage = rawMessage;
        this.level = level;
        this.category = category;
        this.arguments = arguments != null && arguments.length > 0 ? arguments : null;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Convenience-method - constructs with {@link Level#error}.
     *
     * @param message   the message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message error(@Nonnull String message, Object... arguments) {
        return new Message(Level.error, message, arguments);
    }

    /**
     * Convenience-method - constructs with {@link Level#warn}.
     *
     * @param message   the message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message warn(@Nonnull String message, Object... arguments) {
        return new Message(Level.warn, message, arguments);
    }

    /**
     * Convenience-method - constructs with {@link Level#info}.
     *
     * @param message   the message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message info(@Nonnull String message, Object... arguments) {
        return new Message(Level.info, message, arguments);
    }

    /**
     * Convenience-method - constructs with {@link Level#debug}.
     *
     * @param message   the message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message debug(@Nonnull String message, Object... arguments) {
        return new Message(Level.debug, message, arguments);
    }

    /**
     * Adds a detailmessage.
     *
     * @return this for builder style operation chaining.
     */
    @Nonnull
    public Message addDetail(@Nonnull Message detailMessage) {
        if (details == null) { details = new ArrayList<>(); }
        details.add(detailMessage);
        return this;
    }

    /** Time the message was created, as in {@link System#currentTimeMillis()}. */
    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    /** Time the message was created. */
    @Nullable
    public Date getTimestampAsDate() {
        return timestamp != null ? new Date(timestamp) : null;
    }

    /** The kind of message - informational, warning, error. Default is {@link Level#info}. */
    @Nonnull
    public Level getLevel() {
        return level != null ? level : Level.info;
    }

    /**
     * The raw, un-i18n-ed, human readable message text, possibly with argument placeholders {@literal {}}.
     * If i18n is wanted, this is the key for the i18n - all variable parts should be put into the arguments. Mandatory part of a message.
     */
    @Nonnull
    public String getRawMessage() {
        return rawMessage;
    }

    /**
     * A human readable message text composed from {@link #getRawMessage()}, all argument placeholders {@literal {}}
     * replaced by the corresponding {@link #getArguments()}. This is lazily created from {@link #getRawMessage()}
     * and {@link #getArguments()}. In JSON-serializations, the {@link #getRawMessage()} is i18n-ed to the request when
     * {@link MessageTypeAdapterFactory} is correctly used.
     */
    @Nonnull
    public String getMessage() {
        if (message != null) { return message; }
        if (rawMessage == null) { return ""; }
        message = rawMessage;
        if (arguments != null && arguments.length > 0) {
            message = LoggerFormat.format(message, arguments);
        }
        return message;
    }

    /** Like {@link #getMessage()}, but returns the message localized for the request and parameters replaced. */
    public String getMessage(@Nullable SlingHttpServletRequest request) {
        if (request == null) {
            return getMessage();
        }
        String i18nMessage = rawMessage;
        if (StringUtils.isNotBlank(i18nMessage)) {
            String newMessage = I18N.get(request, i18nMessage);
            if (StringUtils.isNotBlank(newMessage)) {
                i18nMessage = newMessage;
            }
            if (arguments != null && arguments.length > 0) {
                i18nMessage = LoggerFormat.format(i18nMessage, arguments);
            }
        } else {
            i18nMessage = "";
        }
        return i18nMessage;
    }

    /**
     * Optional arguments used in placeholders of the {@link #getMessage()}. If transmission over JSON is needed,
     * these must be serializable with GSON.
     */
    @Nonnull
    public List<Object> getArguments() {
        return arguments != null ? Arrays.asList(arguments) : Collections.emptyList();
    }

    /**
     * Can optionally be used to categorize messages for filtering / sorting. This is not meant to be shown directly
     * to the user.
     */
    @Nullable
    public String getCategory() {
        return category;
    }

    /**
     * Sets the optional category: can optionally be used to categorize messages for filtering / sorting. This is not
     * meant to be shown directly to the user.
     *
     * @return this for builder style operation chaining.
     */
    @Nonnull
    public Message setCategory(@Nullable String category) {
        this.category = category;
        return this;
    }

    /**
     * Optional: label of the field which the message is about, primarily in validation messages. (Not the
     * human-readable but the programmatical id is meant.)
     */
    @Nullable
    public String getLabel() {
        return label;
    }

    /**
     * Sets the optional context: a label of the field which the message is about, primarily in validation messages.
     * (Not the human-readable but the programmatical id is meant.)
     *
     * @return this for builder style operation chaining.
     */
    @Nonnull
    public Message setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    /**
     * Optional: a context of the message, such as the dialog tab in a validation message. (Not the
     * human-readable but the programmatical id is meant.)
     */
    @Nullable
    public String getContext() {
        return context;
    }

    /**
     * Sets a context: a context of the message, such as the dialog tab in a validation message. (Not the
     * human-readable but the programmatical id is meant.)
     *
     * @return this for builder style operation chaining.
     */
    @Nonnull
    public Message setContext(@Nullable String context) {
        this.context = context;
        return this;
    }

    /**
     * Optional unmodifiable collection of detail messages describing the problem further. To add details use {@link #addDetail(Message)}.
     *
     * @see #addDetail(Message)
     */
    @Nonnull
    public List<Message> getDetails() {
        return details != null ? Collections.unmodifiableList(details) : Collections.emptyList();
    }

    /**
     * Internationalizes the message according to the requests locale. This modifies the message: the
     * {@link #getMessage()} is looked up as i18n key, and then the arguments are placed into the placeholders and
     * then cleared. Recommended only after {@link #logInto(Logger)} or {@link #logInto(Logger, Throwable)}.
     *
     * @return this message for builder-style operation-chaining.
     * @deprecated rather register a {@link MessageTypeAdapterFactory#MessageTypeAdapterFactory(SlingHttpServletRequest)}.
     */
    @Nonnull
    @Deprecated
    public Message i18n(SlingHttpServletRequest request) {
        if (!i18lized) {
            message = getMessage(request);
        } else { // already i18lized - misuse
            LOG.warn("Second i18n on same message", new Exception("Stacktrace for second i18n, not thrown"));
        }
        return this;
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
        return (Message) super.clone();
    }

    /**
     * Clones the {@link Message} and sets its {@link Message#message} to a properly i18n-ed version,
     * and also makes Strings out of non-String and non-Numeric arguments to avoid problems with not JSON
     * serializable arguments.
     * <p>
     * Tradeoff: keeping numbers is more efficient but GSON turns numbers into doubles on deserialization
     * which sometimes creates differences when formatting is repeated on a serialized and deserialized
     * {@link Message}.
     */
    protected Message prepareForJsonSerialization(SlingHttpServletRequest request) throws CloneNotSupportedException {
        Message i18nMessage = clone();
        i18nMessage.message = getMessage(request);
        if (arguments != null) {
            i18nMessage.arguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; ++i) {
                Object arg = arguments[i];
                if ((arg instanceof String) || (arg instanceof Number)) {
                    i18nMessage.arguments[i] = arg;
                } else {
                    i18nMessage.arguments[i] = LoggerFormat.format("{}", arg);
                }
            }
        }
        return i18nMessage;
    }

    /**
     * Logs the message into the specified logger. Can be done automatically by a {@link MessageContainer} if
     * {@link MessageContainer#MessageContainer(Logger)} is used.
     *
     * @param log the log to write the message into
     * @return this message for builder-style operation-chaining.
     * @see MessageContainer#MessageContainer(Logger)
     */
    @Nonnull
    public Message logInto(@Nullable Logger log) {
        return logInto(log, null);
    }

    /**
     * Logs the message into the specified logger.
     *
     * @param log   the log to write the message into
     * @param cause optionally, an exception that is logged as a cause of the message
     * @return this message for builder-style operation-chaining.
     */
    @Nonnull
    public Message logInto(@Nonnull Logger log, @Nullable Throwable cause) {
        if (log != null) {
            switch (getLevel()) {
                case error:
                    if (log.isErrorEnabled()) {
                        log.error(toFormattedMessage(), cause);
                    }
                    break;
                case warn:
                    if (log.isWarnEnabled()) {
                        log.warn(toFormattedMessage(), cause);
                    }
                    break;
                case debug:
                    if (log.isDebugEnabled()) {
                        log.debug(toFormattedMessage(), cause);
                    }
                    break;
                case info:
                default:
                    if (log.isInfoEnabled()) {
                        log.info(toFormattedMessage(), cause);
                    }
                    break;
            }
        }
        return this;
    }

    /**
     * Return a full text representation of the message with replaced arguments and appended details. Mainly for
     * logging / debugging purposes.
     */
    public String toFormattedMessage() {
        StringBuilder buf = new StringBuilder();
        appendFormattedTo(buf, "", level);
        return buf.toString();
    }

    protected void appendFormattedTo(StringBuilder buf, String indent, Level baseLevel) {
        buf.append(indent);
        if (level != null && baseLevel != null && level != baseLevel) {
            buf.append(level.name()).append(": ");
        }
        if (arguments != null) {
            FormattingTuple formatted = MessageFormatter.arrayFormat(message, arguments);
            buf.append(formatted.getMessage());
        } else {
            buf.append(message);
        }
        if (details != null) {
            String addIndent = indent + "    ";
            buf.append("\n").append(addIndent).append("Details:");
            for (Message detail : details) {
                buf.append("\n");
                detail.appendFormattedTo(buf, addIndent, baseLevel);
            }
        }
    }

    public enum Level {
        /**
         * Problems that require the users attention. This usually means that an operation was aborted or yielded
         * errorneous results.
         */
        error,
        /**
         * A warning that might or might not indicate that the result of an operation could have had errorneous
         * results.
         */
        warn,
        /** Informational messages for further details. */
        info,
        /**
         * Detailed informations that are not normally shown to users, but could help to investigate problems if
         * required.
         */
        debug
    }

}
