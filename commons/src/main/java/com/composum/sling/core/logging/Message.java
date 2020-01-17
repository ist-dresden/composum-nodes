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
     * The level at which this message is logged, if different from level. This might be transient, but we make it
     * non-transient because messages might be transmitted to a remote system and logged there again.
     *
     * @see #getLogLevel()
     */
    @Nullable
    protected Level logLevel;

    /** @see #getContext() */
    @Nullable
    protected String context;

    /** @see #getLabel() */
    @Nullable
    protected String label;

    /**
     * If set, an i18n-ed version of {@link #rawText} with all placeholders replaced. This is modified during
     * JSON-serialization.
     */
    protected String text;

    /**
     * The raw version of the text which can contain {@literal {}}-placeholders and is used as
     * i18n-key.
     */
    protected String rawText;

    /** @see #getArguments() */
    @Nullable
    protected Object[] arguments;

    /** @see #getCategory() */
    @Nullable
    protected String category;

    /** @see #getDetails() */
    @Nullable
    protected List<Message> details;

    /** @see #getTimestamp() */
    protected Long timestamp;

    /** @deprecated only for JSON deserialization. */
    @Deprecated
    public Message() {
        // empty
    }

    /**
     * Creates a message.
     *
     * @param level     the level of the message, default {@link Level#info}
     * @param rawText   the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    public Message(@Nullable Level level, @Nonnull String rawText, Object... arguments) {
        this.rawText = rawText;
        this.level = level;
        this.category = category;
        this.arguments = arguments != null && arguments.length > 0 ? arguments : null;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Convenience-method - constructs with {@link Level#error}.
     *
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message error(@Nonnull String text, Object... arguments) {
        return new Message(Level.error, text, arguments);
    }

    /**
     * Convenience-method - constructs a validation message (which is logged only at level debug) with
     * {@link Level#error}.
     *
     * @param context   an optional context of the message, such as the dialog tab in a validation message.
     * @param label     an optional: label of the field which the message is about, primarily in validation messages.
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    public static Message validationError(@Nullable final String context, @Nullable final String label,
                                          @Nonnull final String text, Object... args) {
        return error(text, args).setContext(context).setLabel(label).setLogLevel(Level.debug);
    }

    /**
     * Convenience-method - constructs with {@link Level#warn}.
     *
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message warn(@Nonnull String text, Object... arguments) {
        return new Message(Level.warn, text, arguments);
    }

    /**
     * Convenience-method - constructs a validation message (which is logged only at level debug) with
     * {@link Level#warn}.
     *
     * @param context   an optional context of the message, such as the dialog tab in a validation message.
     * @param label     an optional: label of the field which the message is about, primarily in validation messages.
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    public static Message validationWarn(@Nullable final String context, @Nullable final String label,
                                         @Nonnull final String text, Object... args) {
        return warn(text, args).setContext(context).setLabel(label).setLogLevel(Level.debug);
    }

    /**
     * Convenience-method - constructs with {@link Level#info}.
     *
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message info(@Nonnull String text, Object... arguments) {
        return new Message(Level.info, text, arguments);
    }

    /**
     * Convenience-method - constructs a validation message (which is logged only at level debug) with
     * {@link Level#info}.
     *
     * @param context   an optional context of the message, such as the dialog tab in a validation message.
     * @param label     an optional: label of the field which the message is about, primarily in validation messages.
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    public static Message validationInfo(@Nullable final String context, @Nullable final String label,
                                         @Nonnull final String text, Object... args) {
        return info(text, args).setContext(context).setLabel(label).setLogLevel(Level.debug);
    }

    /**
     * Convenience-method - constructs with {@link Level#debug}.
     *
     * @param text      the untranslated text of message, possibly with placeholders {@literal {}} for arguments
     * @param arguments optional arguments placed in placeholders. Caution: must be primitive types if this is to be
     *                  transmitted with JSON!
     */
    @Nonnull
    public static Message debug(@Nonnull String text, Object... arguments) {
        return new Message(Level.debug, text, arguments);
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
     * The level with which we will log the message if {@link MessageContainer} has a logger. Transient attribute -
     * will not be transmitted in JSON. This can be different from the {@link #getLevel()}, e.g. in the case of
     * validation messages which should only be logged as debug. {@link Level#none} means it will not be logged; null
     * means that {@link #getLevel()} is used.
     */
    @Nullable
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the level with which we will log the message if {@link MessageContainer} has a logger. Transient attribute -
     * will not be transmitted in JSON. This can be different from the {@link #getLevel()}, e.g. in the case of
     * validation messages which should only be logged as debug. {@link Level#none} means it will not be logged; null
     * means that {@link #getLevel()} is used.
     *
     * @return {this} for chaining calls in builder-style
     */
    @Nonnull
    public Message setLogLevel(@Nullable Level logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * The raw, un-i18n-ed, human readable message text, possibly with argument placeholders {@literal {}}.
     * If i18n is wanted, this is the key for the i18n - all variable parts should be put into the arguments. Mandatory part of a message.
     */
    @Nonnull
    public String getRawText() {
        return rawText;
    }

    /**
     * A human readable i18n-ed message text composed from {@link #getRawText()}, all argument placeholders
     * {@literal {}} replaced by the corresponding {@link #getArguments()}. This is lazily created from {@link #getRawText()}
     * and {@link #getArguments()}. In JSON-serializations, the {@link #getRawText()} is i18n-ed to the request when
     * {@link MessageTypeAdapterFactory} is correctly used.
     */
    @Nonnull
    public String getText() {
        if (text != null) { return text; }
        if (rawText == null) { return ""; }
        text = rawText;
        if (arguments != null && arguments.length > 0) {
            text = LoggerFormat.format(text, arguments);
        }
        return text;
    }

    /** Like {@link #getText()}, but returns the text i18n-ed for the request and parameters replaced. */
    public String getMessage(@Nullable SlingHttpServletRequest request) {
        if (request == null) {
            return getText();
        }
        String i18nMessage = rawText;
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
     * Optional arguments used in placeholders of the {@link #getText()}. If transmission over JSON is needed,
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

    @Override
    public Message clone() throws CloneNotSupportedException {
        return (Message) super.clone();
    }

    /**
     * Clones the {@link Message} and sets its {@link Message#text} to a properly i18n-ed version,
     * and also makes Strings out of non-String and non-Numeric arguments to avoid problems with not JSON
     * serializable arguments.
     * <p>
     * Tradeoff: keeping numbers is more efficient but GSON turns numbers into doubles on deserialization
     * which sometimes creates differences when formatting is repeated on a serialized and deserialized
     * {@link Message}.
     */
    protected Message prepareForJsonSerialization(SlingHttpServletRequest request) throws CloneNotSupportedException {
        Message i18nMessage = clone();
        i18nMessage.text = getMessage(request);
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
     * Logs the message into the specified logger, including the {cause}'s stacktrace.
     * Can be done automatically by a {@link MessageContainer} if {@link MessageContainer#MessageContainer(Logger)}
     * is used.
     *
     * @param log   the log to write the message into
     * @param cause optionally, an exception that is logged as a cause of the message
     * @return this message for builder-style operation-chaining.
     */
    @Nonnull
    public Message logInto(@Nullable Logger log, @Nullable Throwable cause) {
        Level thelevel = getLogLevel();
        thelevel = thelevel != null ? thelevel : getLevel();
        if (log != null) {
            switch (thelevel) {
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
                case none:
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
     * logging / debugging purposes. Not i18n-ed.
     */
    public String toFormattedMessage() {
        StringBuilder buf = new StringBuilder();
        appendFormattedTo(buf, "", level);
        return buf.toString();
    }

    protected void appendFormattedTo(StringBuilder buf, String indent, Level baseLevel) {
        buf.append(indent);
        boolean differingLoglevel = logLevel != null && logLevel != level;
        if (level != null && baseLevel != null && level != baseLevel || differingLoglevel) {
            if (level != null) { buf.append(level.name()); }
            if (differingLoglevel) { buf.append("(").append(logLevel.name()).append(")"); }
            buf.append(": ");
        }
        if (arguments != null) {
            FormattingTuple formatted = MessageFormatter.arrayFormat(rawText, arguments);
            buf.append(formatted.getMessage());
        } else {
            buf.append(rawText);
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

    /** Kind of message, also used as loglevel when this is logged. */
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
        debug,

        /** Special category mainly useful as {@link #getLogLevel()} which means the message is not being logged. */
        none;

    }

}
