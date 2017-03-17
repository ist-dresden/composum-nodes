package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
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

    protected final HashSet<ClientlibLink> renderedClientlibs;

    private transient SlingScriptHelper scriptHelper;
    private transient ClientlibService clientlibService;

    protected RendererContext(BeanContext context, SlingHttpServletRequest request) {
        this.context = context;
        this.request = request;
        this.renderedClientlibs = new HashSet<>();
    }

    public boolean isClientlibRendered(ClientlibRef reference) {
        for (ClientlibLink link : renderedClientlibs) {
            if (reference.use(link)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("rendered: " + reference.keyPath + " - using: " + reference.getUsedAlternative());
                }
                return true;
            }
        }
        return false;
    }

    public void registerClientlibLink(ClientlibLink link) {
        if (true || !renderedClientlibs.contains(link)) {
            renderedClientlibs.add(link);
            if (LOG.isDebugEnabled()) {
                LOG.debug("registered: " + link);
            }
        } else {
            LOG.error("duplicate clientlib link: " + link);
        }
    }

    public boolean mapClientlibURLs() {
        return getClientlibService().mapClientlibURLs();
    }

    public boolean useMinifiedFiles() {
        return getClientlibService().useMinifiedFiles();
    }

    public ClientlibService getClientlibService() {
        if (clientlibService == null) {
            clientlibService = context.getService(ClientlibService.class);
        }
        return clientlibService;
    }
}
