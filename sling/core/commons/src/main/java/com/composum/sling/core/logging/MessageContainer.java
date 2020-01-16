package com.composum.sling.core.logging;

import com.google.gson.annotations.JsonAdapter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/** A collection of {@link Message}s for humans - also meant for transmitting them via JSON. */
@ThreadSafe
@JsonAdapter(MessageTypeAdapterFactory.class)
public class MessageContainer implements Iterable<Message> {

    private static final Logger LOG = LoggerFactory.getLogger(MessageContainer.class);

    /** A logger where {@link #add(Message)} automatically logs to. */
    @Nullable
    protected transient final Logger log;

    /** Synchronizing on this when accessing messages. */
    protected transient final Object lockObject = new Object();

    /** @see #getMessages() */
    @Nullable
    protected List<Message> messages;

    /**
     * Default constructor which means we do not log added messages - usually it's better to use
     * {@link #MessageContainer(Logger)} and log things immediately.
     */
    public MessageContainer() {
        log = null;
    }

    /**
     * Constructor that allows to set a logger to which {@link #add(Message)} automatically writes the messages.
     *
     * @param log an optional log to which added messages are logged.
     */
    public MessageContainer(@Nullable Logger log) {
        this.log = log;
    }

    /** A (unmodifiable) snapshot of the list of messages. */
    @Nonnull
    public List<Message> getMessages() {
        synchronized (lockObject) {
            return messages != null ? Collections.unmodifiableList(new ArrayList<>(messages)) : Collections.emptyList();
        }
    }

    /**
     * Adds a message to the container, and logs it into the logger if one was specified for this container.
     * The intended usecase is with a logger, so we log a warning if it's called with a throwable but
     * we have no logger, so that Stacktraces don't disappear accidentially.
     *
     * @return this MessageContainer, for builder-style operation chaining.
     */
    @Nonnull
    public MessageContainer add(@Nullable Message message, @Nullable Throwable throwable) {
        if (message != null) {
            synchronized (lockObject) {
                if (messages == null) { messages = new ArrayList<>(); }
                messages.add(message);
            }
            if (log != null) {
                message.logInto(log, throwable);
            } else if (throwable != null) { // very likely a misuse
                LOG.warn("Received throwable but have no logger: {}", message.toFormattedMessage(), throwable);
            }

        }
        return this;
    }

    /**
     * Adds a message to the container, and logs it into the logger if one was specified for this container.
     *
     * @return this MessageContainer, for builder-style operation chaining.
     */
    @Nonnull
    public MessageContainer add(@Nullable Message message) {
        return add(message, null);
    }

    /**
     * Removes all messages.
     *
     * @return this MessageContainer, for builder-style operation chaining.
     */
    @Nonnull
    public MessageContainer clear() {
        synchronized (lockObject) {
            messages = null;
        }
        return this;
    }

    /** Whether there are any messages. */
    public boolean isEmpty() {
        synchronized (lockObject) {
            return messages == null || messages.isEmpty();
        }
    }

    /** Iterates over a readonly view. */
    @Override
    public Iterator<Message> iterator() {
        return getMessages().iterator();
    }

    /** Iterates over a readonly view. */
    @Override
    public void forEach(Consumer<? super Message> action) {
        getMessages().forEach(action);
    }

    /** Iterates over a readonly view. */
    @Override
    public Spliterator<Message> spliterator() {
        return getMessages().spliterator();
    }

}
