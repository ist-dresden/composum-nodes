package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.BeanContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * The context implementation for the clientlib ink rendering.
 * This context provides a registry to avoid clientlib duplicates.
 */
public class RendererContext {

    private static final Logger LOG = LoggerFactory.getLogger(RendererContext.class);

    public static final String CONTEXT_KEY = RendererContext.class.getName() + ".instance";

    public static RendererContext instance(BeanContext context, SlingHttpServletRequest request) {
        RendererContext attribute = (RendererContext) request.getAttribute(CONTEXT_KEY);
        if (attribute == null) {
            attribute = new RendererContext(context, request);
            request.setAttribute(CONTEXT_KEY, attribute);
        }
        return attribute;
    }

    public final BeanContext context;
    public final SlingHttpServletRequest request;

    protected final HashSet<String> alreadyRendered;

    private transient SlingScriptHelper scriptHelper;
    private transient ClientlibService clientlibService;

    protected RendererContext(BeanContext context, SlingHttpServletRequest request) {
        this.context = context;
        this.request = request;
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

    public boolean mapClientlibURLs() {
        return getClientlibService().mapClientlibURLs();
    }

    public ClientlibService getClientlibService() {
        if (clientlibService == null) {
            clientlibService = getService(ClientlibService.class);
        }
        return clientlibService;
    }

    public SlingScriptHelper getScriptHelper() {
        if (scriptHelper == null) {
            scriptHelper = context.getAttribute("sling", SlingScriptHelper.class);
        }
        return scriptHelper;
    }

    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        return getScriptHelper().getService(type);
    }
}
