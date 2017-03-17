package com.composum.sling.clientlibs.processor;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by rw on 05.02.16.
 */
public class ProcessorContext {

    protected final SlingHttpServletRequest request;
    protected final ResourceResolver resolver;
    protected final ExecutorService executorService;
    protected final Map<String, Object> hints;
    protected final boolean mapClientlibURLs;
    protected final boolean useMinifiedFiles;

    public ProcessorContext(final SlingHttpServletRequest request,
                            final ResourceResolver resolver,
                            final ExecutorService executorService,
                            final Map<String, Object> hints,
                            final boolean mapClientlibURLs,
                            final boolean useMinifiedFiles) {
        this.request = request;
        this.resolver = resolver;
        this.hints = hints;
        this.executorService = executorService;
        this.mapClientlibURLs = mapClientlibURLs;
        this.useMinifiedFiles = useMinifiedFiles;
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

    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    public void hint(String key, Object value) {
        hints.put(key, value);
    }
}
