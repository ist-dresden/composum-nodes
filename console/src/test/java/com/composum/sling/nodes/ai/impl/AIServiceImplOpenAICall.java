package com.composum.sling.nodes.ai.impl;

import static org.mockito.Mockito.when;

import java.io.IOException;

import org.mockito.Mockito;

/**
 * Calls OpenAI chat completion service to verify the implementation works.
 * Not a test since that needs network and OpenAI key.
 */
public class AIServiceImplOpenAICall {

    private static AIServiceImpl aiService = new AIServiceImpl();

    public static void main(String[] args) throws Exception {
        setUp();
        testPrompt();
    }

    public static void setUp() throws IOException {
        AIServiceImpl.Configuration config = Mockito.mock(AIServiceImpl.Configuration.class);
        when(config.disabled()).thenReturn(false);
        when(config.defaultModel()).thenReturn(AIServiceImpl.DEFAULT_MODEL);
        when(config.openAiApiKey()).thenReturn(null); // rely on env var OPENAI_API_KEY
        when(config.requestsPerMinuteLimit()).thenReturn(20);
        when(config.requestsPerHourLimit()).thenReturn(60);
        when(config.requestsPerDayLimit()).thenReturn(120);
        aiService.activate(config);
        if (!aiService.isAvailable()) {
            throw new IllegalStateException("AI service not available, probably because of missing OPENAI_API_KEY");
        }
    }

    public static void testPrompt() throws Exception {
        System.out.println(aiService.prompt(null, "Hi!", null));
        System.out.println(aiService.prompt("Reverse the users message.", "Hi!", null));
        System.out.println(aiService.prompt(null, "Make a haiku in JSON format about Composum.", AIServiceImpl.ResponseFormat.JSON));
        System.out.println(aiService.prompt("Always respond in JSON", "Make a haiku about Composum.", AIServiceImpl.ResponseFormat.JSON));
    }
}
