package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.clientlibs.service.DefaultClientlibService;
import com.composum.sling.core.BeanContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The context implementation for the clientlib link rendering, request scoped.
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

    protected final Set<ClientlibLink> renderedClientlibs;

    protected transient ClientlibService clientlibService;

    protected RendererContext(BeanContext context, SlingHttpServletRequest request) {
        this.context = context;
        this.request = request;
        this.renderedClientlibs = new LinkedHashSet<>();
    }

    /** Checks whether a referenced resource or client library is satisfied by an already rendered resource. */
    public boolean isClientlibRendered(ClientlibRef reference) {
        for (ClientlibLink link : renderedClientlibs) {
            if (reference.isSatisfiedby(link)) {
                LOG.debug("rendered: {} - using: {}", reference, link.path);
                return true;
            }
        }
        return false;
    }

    /** Registers rendered resources / client libraries that have already been rendered for the current request. */
    public void registerClientlibLink(ClientlibLink link) {
        if (renderedClientlibs.contains(link)) {
            LOG.error("Bug: duplicate clientlib link: " + link);
        } else {
            renderedClientlibs.add(link);
            if (LOG.isDebugEnabled()) {
                LOG.debug("registered: " + link);
            }
        }
    }

    public boolean mapClientlibURLs() {
        return getConfiguration().getMapClientlibURLs();
    }

    public boolean useMinifiedFiles() {
        return getConfiguration().getUseMinifiedFiles();
    }

    public ClientlibService getClientlibService() {
        if (clientlibService == null) {
            clientlibService = context.getService(ClientlibService.class);
        }
        return clientlibService;
    }

    public Set<ClientlibLink> getRenderedClientlibs() {
        return renderedClientlibs;
    }

    public ClientlibConfiguration getConfiguration() {
        return getClientlibService().getClientlibConfig();
    }

    public ResourceResolver getResolver() {
        return request.getResourceResolver();
    }

}

