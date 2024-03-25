package com.composum.sling.nodes.ai.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.composum.sling.nodes.ai.AIService;

public class AIServiceImplTest {

    @Test
    public void extractOpenAIResponse() throws AIService.AIServiceException {
        String response = "{\n" +
                "  \"id\": \"chatcmpl-123\",\n" +
                "  \"model\": \"gpt-3.5-turbo-0125\",\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Hello there, how may I assist you today?\"\n" +
                "      },\n" +
                "      \"finish_reason\": \"stop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        String result = new AIServiceImpl().extractText(response);
        assertEquals("Hello there, how may I assist you today?", result);
    }

    @Test
    public void extractClaudeResponse() throws AIService.AIServiceException {
        String response = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"text\": \"Hi! My name is Claude.\",\n" +
                "      \"type\": \"text\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"id\": \"msg_013Zva2CMHLNnXjNJJKqJ2EF\",\n" +
                "  \"model\": \"claude-3-opus-20240229\",\n" +
                "  \"role\": \"assistant\",\n" +
                "  \"stop_reason\": \"end_turn\",\n" +
                "  \"stop_sequence\": null,\n" +
                "  \"type\": \"message\",\n" +
                "  \"usage\": {\n" +
                "    \"input_tokens\": 10,\n" +
                "    \"output_tokens\": 25\n" +
                "  }\n" +
                "}";
        String result = new AIServiceImpl().extractText(response);
        assertEquals("Hi! My name is Claude.", result);
    }

}
