package com.composum.sling.nodes.ai;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * AI related services.
 */
public interface AIService {

    public static enum ResponseFormat {
        TEXT,
        /**
         * Returns only json; important: the prompt must contain a request to return JSON!
         */
        JSON
    }

    /**
     * Whether the service is configured.
     */
    boolean isAvailable();

    /**
     * Simplest AI related service: execute a prompt and return the response of the AI.
     *
     * @param systemprompt   a system prompt to be used in addition to the prompt
     * @param prompt         the prompt to be used
     * @param responseFormat the format of the response, default {@link ResponseFormat#TEXT}.
     * @return the response of the AI
     */
    @Nonnull
    String prompt(@Nullable String systemprompt, @Nonnull String prompt, @Nullable ResponseFormat responseFormat) throws AIServiceException;

    /**
     * Something went wrong.
     */
    public class AIServiceException extends Exception {
        public AIServiceException(String message) {
            super(message);
        }

        public AIServiceException(String message, IOException e) {
            super(message, e);
        }
    }

    public class AIServiceNotAvailableException extends AIServiceException {
        public AIServiceNotAvailableException() {
            super("AI Service not available");
        }
    }

}
