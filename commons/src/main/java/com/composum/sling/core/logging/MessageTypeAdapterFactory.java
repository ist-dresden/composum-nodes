package com.composum.sling.core.logging;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Creates a {@link TypeAdapter} for the JSON (de-)serialization of {@link MessageContainer}s and {@link Message}s.
 * In this implementation it only handles exactly {@link MessageContainer} and {@link Message}, no derived classes.
 * <p>
 * The {@link TypeAdapter} serializes the {@link MessageContainer} to a JSON array of GSON serialized messages
 * and takes care of i18n.
 * <p>
 * Considerations for i18n: If a {@link Message} is delivered in a response, it should normally contain the i18n-ed message
 * with all placeholders replaced by the arguments as attribute "message". However, we also want to transfer the raw
 * data (un-i18n-ed message and the arguments) to be able to generate several i18ns (e.g. when logging audit-data in
 * a business-language). So we also transmit the raw data: "rawText" and "arguments" (as {@link Object#toString()}).
 */
public class MessageTypeAdapterFactory implements TypeAdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MessageTypeAdapterFactory.class);

    @NotNull
    protected final Supplier<SlingHttpServletRequest> requestProvider;

    /**
     * Default constructor, e.g. if used with {@link JsonAdapter} - does not provide i18n for the messages.
     *
     * @deprecated since using explicitly makes little sense.
     */
    @Deprecated
    public MessageTypeAdapterFactory() {
        this(() -> null);
    }

    /** If you want i18n. For use with {@link com.google.gson.GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)}. */
    public MessageTypeAdapterFactory(@Nullable SlingHttpServletRequest request) {
        this.requestProvider = () -> request;
    }

    /** If you want i18n. For use with {@link com.google.gson.GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)}. */
    public MessageTypeAdapterFactory(@NotNull Supplier<SlingHttpServletRequest> requestProvider) {
        this.requestProvider = Objects.requireNonNull(requestProvider);
    }

    @SuppressWarnings({"unchecked", "NullabilityAnnotations", "ReturnOfNull"})
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (MessageContainer.class.equals(type.getRawType())) {
            return (TypeAdapter<T>) new MessageContainerTypeAdapter(gson);
        }
        if (Message.class.isAssignableFrom(type.getRawType())) {
            TypeAdapter<Message> defaultGsonAdapter = (TypeAdapter<Message>) gson.getDelegateAdapter(this, type);
            return (TypeAdapter<T>) new MessageTypeAdapter(gson, Objects.requireNonNull(defaultGsonAdapter));
        }
        return null;
    }

    protected static class MessageContainerTypeAdapter extends TypeAdapter<MessageContainer> {
        @NotNull
        protected final Gson gson;

        public MessageContainerTypeAdapter(@NotNull Gson gson) {
            this.gson = gson;
        }

        @SuppressWarnings("resource")
        @Override
        public void write(JsonWriter out, MessageContainer container) throws IOException {
            if (container != null) {
                out.beginArray();
                for (Message msg : container.getMessages()) {
                    // use getMessages() to avoid concurrency problems - returns a snapshot
                    gson.toJson(msg, msg.getClass(), out);
                }
                out.endArray();
            } else {
                out.nullValue();
            }
        }

        @Override
        public MessageContainer read(JsonReader in) throws IOException {
            MessageContainer container = new MessageContainer();
            in.beginArray();
            while (in.hasNext()) { container.add(gson.fromJson(in, Message.class));}
            in.endArray();
            return container;
        }
    }

    protected class MessageTypeAdapter extends TypeAdapter<Message> {
        @NotNull
        protected final Gson gson;

        @NotNull
        protected final TypeAdapter<Message> defaultGsonAdapter;

        public MessageTypeAdapter(@NotNull Gson gson, @NotNull TypeAdapter<Message> defaultGsonAdapter) {
            this.gson = gson;
            this.defaultGsonAdapter = defaultGsonAdapter;
        }

        @SuppressWarnings("resource")
        @Override
        public void write(JsonWriter out, Message value) throws IOException {
            if (value != null) {
                // to avoid touching the original message to set the message attribute, we clone it first.
                Message i18nMessage = prepareI18nMessage(value);
                defaultGsonAdapter.write(out, i18nMessage);
            } else {
                out.nullValue();
            }
        }

        /** @see Message#prepareForJsonSerialization(SlingHttpServletRequest) */
        @NotNull
        protected Message prepareI18nMessage(@NotNull Message value) {
            Message i18nMessage = value;
            try {
                i18nMessage = value.prepareForJsonSerialization(requestProvider.get());
            } catch (CloneNotSupportedException impossible) {
                LOG.error("Could not clone " + value.getClass(), impossible);
                i18nMessage.getText(); // make sure that i18nmessage.message at least contains something.
            }
            return i18nMessage;
        }

        @Override
        public Message read(JsonReader in) throws IOException {
            return defaultGsonAdapter.read(in);
        }
    }
}
