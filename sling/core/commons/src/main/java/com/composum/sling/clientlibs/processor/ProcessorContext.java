package com.composum.sling.clientlibs.processor;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rw on 05.02.16.
 */
public class ProcessorContext {

    protected final ResourceResolver resolver;
    protected final ExecutorService executorService;
    protected final Map<String, Object> hints;

    public ProcessorContext(final ResourceResolver resolver,
                            final Map<String, Object> hints,
                            int maxPipelineLength) {
        this.resolver = resolver;
        this.hints = hints;
        executorService = Executors.newFixedThreadPool(maxPipelineLength);
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void hint(String key, Object value) {
        hints.put(key, value);
    }
}
