package com.composum.sling.nodes.ai;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * AI related services.
 */
public interface AIService {

    /**
     * Whether the service is configured.
     */
    boolean isAvailable();

    /**
     * Simplest AI related service: execute a prompt and return the response of the AI.
     */
    @Nonnull
    String prompt(@Nullable String systemprompt, @Nonnull String prompt);

    /**
     * Something went wrong.
     */
    public class AIServiceException extends Exception {
        public AIServiceException(String message) {
            super(message);
        }
    }

}
