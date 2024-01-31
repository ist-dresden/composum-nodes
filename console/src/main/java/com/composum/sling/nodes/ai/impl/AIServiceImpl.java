package com.composum.sling.nodes.ai.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.nodes.ai.AIService;

@Component(
        name = "Composum Nodes AI Service",
        property = {
                Constants.SERVICE_DESCRIPTION + "=AI services based on ChatGPT"
        },
        configurationPolicy = ConfigurationPolicy.OPTIONAL)
@Designate(ocd = AIServiceImpl.Configuration.class)
public class AIServiceImpl implements AIService {

    private static final Logger LOG = LoggerFactory.getLogger(AIServiceImpl.class);

    public static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    private Configuration config;
    private String openAiApiKey;
    private RateLimiter rateLimiter;

    protected void activate(Configuration configuration) {
        this.config = configuration;
        this.openAiApiKey = StringUtils.defaultIfBlank(config.openAiApiKey(), System.getenv("OPENAI_API_KEY"));
        this.openAiApiKey = StringUtils.defaultIfBlank(this.openAiApiKey, System.getProperty("openai.api.key"));
        if (!config.openAiApiKeyFile().isEmpty()) {
            try {
                this.openAiApiKey = StringUtils.defaultIfBlank(this.openAiApiKey,
                        FileUtils.readFileToString(new File(config.openAiApiKeyFile()), Charsets.UTF_8));
            } catch (IOException e) {
                LOG.error("Could not read OpenAI key from {}", config.openAiApiKeyFile(), e);
            }
        }
        openAiApiKey = StringUtils.trimToNull(openAiApiKey);
        rateLimiter = null;
        if (isAvailable()) {
            rateLimiter = new RateLimiter(null, config.requestsPerMinuteLimit(), 1, java.util.concurrent.TimeUnit.MINUTES);
            rateLimiter = new RateLimiter(rateLimiter, config.requestsPerHourLimit(), 1, java.util.concurrent.TimeUnit.HOURS);
            rateLimiter = new RateLimiter(rateLimiter, config.requestsPerDayLimit(), 1, java.util.concurrent.TimeUnit.DAYS);
        }
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @Override
    public boolean isAvailable() {
        return config != null && !config.disabled() && StringUtils.isNoneBlank(config.defaultModel(), openAiApiKey);
    }

    @NotNull
    @Override
    public String prompt(@Nullable String systemprompt, @NotNull String prompt) {
        throw new UnsupportedOperationException("Not implemented yet."); // FIXME hps 31.01.24 not implemented yet
    }

    @ObjectClassDefinition(name = "Composum Nodes AI Service Configuration", description = "AI services based on ChatGPT")
    public @interface Configuration {

        @AttributeDefinition(name = "Disable the GPT Chat Completion Service", description = "Disable the GPT Chat Completion Service", defaultValue = "false")
        boolean disabled() default false; // we want it to work by just deploying it. Admittedly this is a bit doubtful.

        @AttributeDefinition(name = "OpenAI API Key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .")
        String openAiApiKey();

        // alternatively, a key file
        @AttributeDefinition(name = "OpenAI API Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there.")
        String openAiApiKeyFile();

        @AttributeDefinition(name = "Default model to use for the chat completion. The default is " + DEFAULT_MODEL + ". Please consider the varying prices https://openai.com/pricing .", defaultValue = DEFAULT_MODEL)
        String defaultModel() default DEFAULT_MODEL;

        @AttributeDefinition(name = "Requests per minute", description = "The number of requests per minute - after half of that we do slow down. The default is 100.", defaultValue = "20")
        int requestsPerMinuteLimit() default 20;

        @AttributeDefinition(name = "Requests per hour", description = "The number of requests per hour - after half of that we do slow down. The default is 1000.", defaultValue = "60")
        int requestsPerHourLimit() default 60;

        @AttributeDefinition(name = "Requests per day", description = "The number of requests per day - after half of that we do slow down. The default is 12000.", defaultValue = "120")
        int requestsPerDayLimit() default 120;

    }
}
