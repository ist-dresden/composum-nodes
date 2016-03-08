package com.composum.sling.clientlibs.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import java.util.HashSet;

/**
 * The context implementation for the clientlib ink rendering.
 * This context provides a registry to avoid clientlib duplicates.
 */
public class RendererContext {

    private static final Logger LOG = LoggerFactory.getLogger(RendererContext.class);

    public static final String CONTEXT_KEY = RendererContext.class.getName() + ".instance";

    public static RendererContext instance(ServletRequest request) {
        RendererContext attribute = (RendererContext) request.getAttribute(CONTEXT_KEY);
        if (attribute == null) {
            attribute = new RendererContext();
            request.setAttribute(CONTEXT_KEY, attribute);
        }
        return attribute;
    }

    protected final HashSet<String> alreadyRendered;

    protected RendererContext() {
        this.alreadyRendered = new HashSet<>();
    }

    public boolean tryAndRegister(String key) {
        if (!alreadyRendered.contains(key)) {
            LOG.debug("registered: " + key);
            alreadyRendered.add(key);
            return true;
        }
        LOG.debug("rejected: " + key);
        return false;
    }
}
