package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode.DEPENDS;
import static com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode.EMBEDDED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Visitor that realizes the rendering process for a client library. The visit functions return true if the processed
 * element embedded some files.
 */
public class RenderingVisitor extends AbstractClientlibVisitor {

    private static final Logger LOG = getLogger(RenderingVisitor.class);

    protected final RendererContext context;
    protected final List<ClientlibLink> linksToRender;
    protected final boolean ownerWasAlreadyRendered;

    public RenderingVisitor(ClientlibElement owner, RendererContext context) {
        this(owner, context, null, null);
    }

    protected RenderingVisitor(ClientlibElement owner, RendererContext context, List<ClientlibLink> linksToRender,
                               LinkedHashSet<ClientlibLink> processedElements) {
        super(owner, context.getClientlibService(), context.getResolver(), processedElements);
        this.context = context;
        this.linksToRender = linksToRender != null ? linksToRender : new ArrayList<>();
        this.ownerWasAlreadyRendered = context.isClientlibRendered(owner.getRef());
    }

    @Override
    public RenderingVisitor execute() throws IOException, RepositoryException {
        super.execute();
        return this;
    }

    @Override
    protected ClientlibVisitor createVisitorFor(ClientlibElement element) {
        return new RenderingVisitor(element, context, linksToRender, processedElements);
    }

    @Override
    public void action(ClientlibCategory clientlibCategory, ClientlibVisitor.VisitorMode mode,
                       ClientlibResourceFolder parent) {
        if (hasEmbeddedFiles && !context.getConfiguration().getDebug()) render(mode, clientlibCategory, parent);
        else context.registerClientlibLink(clientlibCategory.makeLink(), parent);
    }

    @Override
    public void action(Clientlib clientlib, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) {
        if (hasEmbeddedFiles && !context.getConfiguration().getDebug()) render(mode, clientlib, parent);
        else context.registerClientlibLink(clientlib.makeLink(), parent);
    }

    @Override
    public void action(ClientlibFile file, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) {
        render(mode, file, parent);
    }

    protected void render(VisitorMode mode, ClientlibElement element, ClientlibResourceFolder parent) {
        if (ownerWasAlreadyRendered) return;
        ClientlibLink link = element.makeLink();
        if (owner == element) link = link.withHash(getHash());
        if (context.isClientlibRendered(element.getRef())) {
            if (EMBEDDED == mode)
                LOG.error("Already rendered / embedded file is also embedded in clientlib {} and thus included twice:" +
                        " {}", owner, link);
        } else {
            if (DEPENDS == mode || context.getConfiguration().getDebug())
                linksToRender.add(link);
            context.registerClientlibLink(link, parent);
        }
    }

    @Override
    public void action(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
        LOG.trace(">>> {} {}", mode, externalUri);
        if (!context.isClientlibRendered(externalUri.getRef())) {
            ClientlibLink link = externalUri.makeLink();
            linksToRender.add(link); // external references can't be embedded
            context.registerClientlibLink(link, parent);
        }
        LOG.trace("<<< {} {}", mode, externalUri);
    }

    public List<ClientlibLink> getLinksToRender() {
        return linksToRender;
    }

    @Override
    protected void notPresent(ClientlibRef ref, VisitorMode mode, ClientlibResourceFolder parent) {
        if (StringUtils.contains(ref.path, ","))
            LOG.warn("Not present and contains , - should probably be a multi string: " +
                    "{} references {}", parent, ref);
        else if (StringUtils.contains(ref.category, ","))
            LOG.warn("Not present and contains , - should probably be a multi " +
                    "string: " +
                    "{} references {}", parent, ref);
        else if (ref.optional) {
            LOG.debug("Not present: opt. {} referenced from {}", ref, parent);
        } else {
            LOG.warn("Not present: mand. {} referenced from {}", ref, parent);
        }
    }
}
