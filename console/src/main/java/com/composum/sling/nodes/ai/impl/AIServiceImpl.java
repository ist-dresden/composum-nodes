package com.composum.sling.nodes.ai.impl;

import static com.composum.sling.nodes.ai.impl.AIServiceImpl.SERVICE_NAME;
import static java.util.Arrays.asList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.nodes.ai.AIService;
import com.google.gson.Gson;

@Component(
        name = SERVICE_NAME,
        property = {
                Constants.SERVICE_DESCRIPTION + "=AI services based on ChatGPT"
        },
        configurationPolicy = ConfigurationPolicy.OPTIONAL)
@Designate(ocd = AIServiceImpl.Configuration.class)
public class AIServiceImpl implements AIService {

    public static final String SERVICE_NAME = "Composum Nodes AI Service";

    protected static final Logger LOG = LoggerFactory.getLogger(AIServiceImpl.class);

    /**
     * The default model - probably a GPT-4 is needed for complicated stuff like JCR queries.
     */
    public static final String DEFAULT_MODEL = "gpt-4-turbo-preview";
    protected static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";

    protected Configuration config;
    protected String apiKey;
    protected RateLimiter rateLimiter;
    protected Gson gson = new Gson();
    protected CloseableHttpClient httpClient;
    protected String chatURL;
    protected String apiKeyHeader;
    protected String additionalHeader;
    protected String additionalHeaderValue;

    @Override
    public boolean isAvailable() {
        return config != null && !config.disabled() && StringUtils.isNotBlank(apiKey);
    }

    protected boolean isAnthropicClaude() {
        return chatURL != null && chatURL.contains("api.anthropic.com");
    }

    @Nonnull
    @Override
    public String prompt(@Nullable String systemmsg, @Nonnull String usermsg, @Nullable ResponseFormat responseFormat) throws AIServiceException {
        if (!isAvailable()) {
            throw new AIServiceNotAvailableException();
        }
        Map<String, String> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", usermsg);

        Map<String, Object> request = new HashMap<>();
        request.put("model", StringUtils.defaultIfBlank(config.defaultModel(), DEFAULT_MODEL));
        if (systemmsg != null && isAnthropicClaude()) {
            request.put("system", systemmsg);
            request.put("messages", asList(userMessage));
        } else if (systemmsg != null) {
            Map<String, Object> systemMessage = new LinkedHashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemmsg);
            request.put("messages", asList(systemMessage, userMessage));
        } else {
            request.put("messages", asList(userMessage));
        }
        request.put("temperature", 0);
        if (config.maxTokens() > 0) {
            request.put("max_tokens", config.maxTokens());
        }
        if (responseFormat == ResponseFormat.JSON && !isAnthropicClaude()) {
            Map<String, String> responseFormatMap = new LinkedHashMap<>();
            responseFormatMap.put("type", "json_object");
            request.put("response_format", responseFormatMap);
        }

        String requestJson = gson.toJson(request);

        rateLimiter.waitForLimit();
        // retrieve response from OpenAI using httpClient 4
        HttpPost postRequest = new HttpPost(this.chatURL);
        EntityBuilder entityBuilder = EntityBuilder.create();
        entityBuilder.setContentType(ContentType.APPLICATION_JSON);
        entityBuilder.setContentEncoding("UTF-8");
        entityBuilder.setText(requestJson);
        postRequest.setEntity(entityBuilder.build());
        if (this.apiKeyHeader != null) {
            String[] headerSplitted = this.apiKeyHeader.split("\\s");
            String headername = headerSplitted[0];
            String prefix = headerSplitted.length > 1 ? headerSplitted[1].trim() + " " : "";
            postRequest.setHeader(headername, prefix + apiKey);
        }
        if (additionalHeader != null && additionalHeaderValue != null) {
            postRequest.setHeader(additionalHeader, additionalHeaderValue);
        }
        String id = "#" + System.nanoTime();
        LOG.debug("Request {} to OpenAI: {}", id, requestJson);
        try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
            return retrieveMessage(id, response);
        } catch (IOException e) {
            LOG.error("" + e, e);
            throw new AIServiceException("Exception accessing the AI: " + e, e);
        }
    }

    @Nonnull
    protected String retrieveMessage(String id, CloseableHttpResponse response) throws AIServiceException, IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity responseEntity = response.getEntity();
        if (statusCode != 200) {
            String errorbody = responseEntity != null ? responseEntity.toString() : "";
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            responseEntity.writeTo(bytes);
            if (bytes.size() > 0) {
                errorbody = errorbody + "\n" + new String(bytes.toByteArray(), Charsets.UTF_8);
            }
            throw new AIServiceException("Error from AI backend: " + response.getStatusLine() + " " + errorbody);
        }
        String responseJson = EntityUtils.toString(responseEntity);
        LOG.debug("Response {} from OpenAI: {}", id, responseJson);
        return extractText(responseJson);
    }

    @NotNull
    protected String extractText(String responseJson) throws AIServiceException {
        Map<String, Object> responseMap = gson.fromJson(responseJson, Map.class);
        String text;
        if (responseMap.containsKey("choices")) { // OpenAI format
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            String finish_reason = (String) choice.get("finish_reason");
            if (!"stop".equals(finish_reason)) {
                throw new AIServiceException("Finish reason is not stop: " + finish_reason + " in response: " + responseJson);
            }
            Map<String, Object> message = MapUtils.emptyIfNull((Map<String, Object>) choice.get("message"));
            text = (String) message.get("content");
        } else if (responseMap.containsKey("content")) { // Anthropic Claude format
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            Map<String, Object> message = content.get(0);
            text = (String) message.get("text");
        } else {
            LOG.error("Response format not recognized: {}", responseJson);
            throw new AIServiceException("Response format not recognized: " + responseJson);
        }
        if (text == null) {
            LOG.error("No message in response: {}", responseJson);
            throw new AIServiceException("No message in response: " + responseJson);
        }
        return text;
    }

    @Deactivate
    protected void deactivate() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.error("Could not close httpClient", e);
            }
        }
        this.config = null;
    }

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        this.config = configuration;
        chatURL = StringUtils.defaultIfBlank(config.chatCompletionUrl(), CHAT_COMPLETION_URL).trim();
        String envVariable = isAnthropicClaude() ? "ANTHROPIC_API_KEY" : "OPENAI_API_KEY";
        this.apiKey = StringUtils.defaultIfBlank(config.openAiApiKey(), System.getenv(envVariable));
        this.apiKey = StringUtils.defaultIfBlank(this.apiKey, System.getProperty("openai.api.key"));
        if (config.openAiApiKeyFile() != null && !config.openAiApiKeyFile().isEmpty()) {
            try {
                this.apiKey = StringUtils.defaultIfBlank(this.apiKey,
                        FileUtils.readFileToString(new File(config.openAiApiKeyFile()), Charsets.UTF_8));
            } catch (IOException e) {
                LOG.error("Could not read OpenAI key from {}", config.openAiApiKeyFile(), e);
            }
        }
        apiKey = StringUtils.trimToNull(apiKey);
        additionalHeader = StringUtils.trimToNull(config.additionalHeader());
        additionalHeaderValue = StringUtils.trimToNull(config.additionalHeaderValue());
        apiKeyHeader = StringUtils.defaultIfBlank(config.apiKeyHeader(), "Authorization Bearer");
        rateLimiter = null;
        if (isAvailable()) {
            int perMinuteLimit = config.requestsPerMinuteLimit() > 0 ? config.requestsPerMinuteLimit() : 20;
            rateLimiter = new RateLimiter(null, perMinuteLimit, 1, java.util.concurrent.TimeUnit.MINUTES);
            int requestsPerHourLimit = config.requestsPerHourLimit() > 0 ? config.requestsPerHourLimit() : 60;
            rateLimiter = new RateLimiter(rateLimiter, requestsPerHourLimit, 1, java.util.concurrent.TimeUnit.HOURS);
            int requestsPerDayLimit = config.requestsPerDayLimit() > 0 ? config.requestsPerDayLimit() : 120;
            rateLimiter = new RateLimiter(rateLimiter, requestsPerDayLimit, 1, java.util.concurrent.TimeUnit.DAYS);
            httpClient = HttpClientBuilder.create().build();
        }
    }

    @ObjectClassDefinition(name = "Composum Nodes AI Service Configuration", description = "AI services based on ChatGPT")
    public @interface Configuration {

        @AttributeDefinition(name = "Disable the GPT Chat Completion Service", description = "Disable the GPT Chat Completion Service", defaultValue = "false")
        boolean disabled() default false; // we want it to work by just deploying it. Admittedly this is a bit doubtful.

        @AttributeDefinition(name = "Chat Completion URL", description = "The URL for chat completions. If not given, the default for OpenAI is used: " + CHAT_COMPLETION_URL +
                " . In the case of Anthropic Claude it is https://api.anthropic.com/v1/messages.")
        String chatCompletionUrl();

        @AttributeDefinition(name = "API key",
                description = "Key for requests to AI backend, in the case of OpenAI from https://platform.openai.com/api-keys. If not given, we check the key file, the environment Variable OPENAI_API_KEY (or in the case of Anthropic Claude ANTHROPIC_API_KEY), and the system property openai.api.key .")
        String openAiApiKey();

        // alternatively, a key file
        @AttributeDefinition(name = "API key file",
                description = "File containing the API key, as an alternative to API key configuration and the variants described there.")
        String openAiApiKeyFile();

        @AttributeDefinition(name = "Header for API key", description = "The header in the request that is used for sending the API key. If it's two words like 'Authorization Bearer' then that second word is used as prefix for the value. Default: 'Authorization Bearer', as used by OpenAI.")
        String apiKeyHeader();

        @AttributeDefinition(name = "Additional Header", description = "Optionally, an additional header. In the cause of OpenAI that could be 'OpenAI-Organization'. In the case of Anthropic Claude it could be 'anthropic-version'.")
        String additionalHeader();

        @AttributeDefinition(name = "Additional Header Value", description = "The value for the additional header. In the case of OpenAI that could be the organization id. In the case of Anthropic Claude it could be the version of the API, e.g. '2023-06-01'.")
        String additionalHeaderValue();

        @AttributeDefinition(name = "Default model",
                description = "Default model to use for the chat completion. If not configured we take a default, in this version " + DEFAULT_MODEL + ". " +
                        "Please consider the varying prices https://openai.com/pricing or whatever service you use. For programming related questions a GPT-4 / Claude Opus seems necessary, though. Do not configure if not necessary, to follow further changes.")
        String defaultModel();

        @AttributeDefinition(name = "Requests per minute", description = "The number of requests per minute - after half of that we do slow down. >0, the default is 100.")
        int requestsPerMinuteLimit() default 20;

        @AttributeDefinition(name = "Requests per hour", description = "The number of requests per hour - after half of that we do slow down. >0, the default is 1000.")
        int requestsPerHourLimit() default 60;

        @AttributeDefinition(name = "Requests per day", description = "The number of requests per day - after half of that we do slow down. >0, the default is 12000.")
        int requestsPerDayLimit() default 120;

        @AttributeDefinition(name = "Maximum number of tokens", description = "Maximum number of tokens in the response. >0, the default is 2048.")
        int maxTokens() default 2048;
    }
}
