package com.composum.sling.nodes.ai.impl;

import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

/**
 * Calls Anthropic Claude create messsage service to verify the implementation works.
 * Not a test since that needs network and OpenAI key.
 *
 * @see "https://docs.anthropic.com/claude/reference/messages_post"
 */
public class AIServiceImplClaudeCall {

    private static AIServiceImpl aiService = new AIServiceImpl();

    public static void main(String[] args) throws Exception {
        setUp();
        testPrompt();
    }

    public static void setUp() throws IOException {
        AIServiceImpl.Configuration config = Mockito.mock(AIServiceImpl.Configuration.class);
        when(config.disabled()).thenReturn(false);
        when(config.chatCompletionUrl()).thenReturn("https://api.anthropic.com/v1/messages");
        when(config.apiKeyHeader()).thenReturn("x-api-key");
        when(config.defaultModel()).thenReturn("claude-3-opus-20240229");
        when(config.additionalHeader()).thenReturn("anthropic-version");
        String version = StringUtils.defaultIfBlank(System.getenv("ANTHROPIC_API_VERSION"), "2023-06-01");
        when (config.additionalHeaderValue()).thenReturn(version);
        when(config.openAiApiKey()).thenReturn(null); // rely on env var ANTHROPIC_API_KEY
        when(config.requestsPerMinuteLimit()).thenReturn(20);
        when(config.requestsPerHourLimit()).thenReturn(60);
        when(config.requestsPerDayLimit()).thenReturn(120);
        when(config.maxTokens()).thenReturn(2048);
        aiService.activate(config);
        if (!aiService.isAvailable()) {
            throw new IllegalStateException("AI service not available, probably because of missing ANTHROPIC_API_KEY");
        }
    }

    public static void testPrompt() throws Exception {
        System.out.println(aiService.prompt(null, "Hi!", null));
        System.out.println(aiService.prompt("Reverse the users message.", "Hi!", null));
        System.out.println(aiService.prompt(null, "Make a haiku in JSON format about Composum.", AIServiceImpl.ResponseFormat.JSON));
        System.out.println(aiService.prompt("Always respond in JSON", "Make a haiku about Composum.", AIServiceImpl.ResponseFormat.JSON));
    }
}
