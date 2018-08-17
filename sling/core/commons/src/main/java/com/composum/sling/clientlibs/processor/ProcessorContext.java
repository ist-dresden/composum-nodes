package com.composum.sling.clientlibs.processor;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * The context in which processing of a {@link com.composum.sling.clientlibs.handle.Clientlib} takes place.
 * Provides a registry to avoid clientlib duplicates.
 */
public class ProcessorContext {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorContext.class);

    public static final String CONTEXT_KEY = ProcessorContext.class.getName() + ".instance";

    protected final SlingHttpServletRequest request;
    protected final ResourceResolver resolver;
    protected final ExecutorService executorService;
    protected final Map<String, Object> hints = Collections.synchronizedMap(new HashMap<>());
    protected final boolean mapClientlibURLs;
    protected final boolean useMinifiedFiles;

    public ProcessorContext(final SlingHttpServletRequest request,
                            final ResourceResolver resolver,
                            final ExecutorService executorService,
                            final boolean mapClientlibURLs,
                            final boolean useMinifiedFiles) {
        this.request = request;
        this.resolver = resolver;
        this.executorService = executorService;
        this.mapClientlibURLs = mapClientlibURLs;
        this.useMinifiedFiles = useMinifiedFiles;
        request.setAttribute(CONTEXT_KEY, this);
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    public boolean mapClientlibURLs() {
        return mapClientlibURLs;
    }

    public boolean useMinifiedFiles() {
        return useMinifiedFiles;
    }

    /** Schedules the runnable for execution in the future. */
    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    /** Schedules something for execution in the future. */
    public <T> Future<T> submit(Callable<T> callable) {
        return executorService.submit(callable);
    }

    public void hint(String key, Object value) {
        hints.put(key, value);
    }

    public Map<String, Object> getHints() {
        return hints;
    }

}
