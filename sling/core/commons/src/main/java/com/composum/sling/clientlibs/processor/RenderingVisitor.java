package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.*;
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
 * Visitor that realizes the rendering process for a client library.
 * The visit functions return true if the processed element embedded some files.
 */
public class RenderingVisitor extends AbstractClientlibVisitor {

    private static final Logger LOG = getLogger(RenderingVisitor.class);

    protected final RendererContext context;
    protected final List<ClientlibLink> linksToRender;

    public RenderingVisitor(ClientlibElement owner, RendererContext context) {
        this(owner, context, null, null);
    }

    protected RenderingVisitor(ClientlibElement owner, RendererContext context, List<ClientlibLink> linksToRender,
                               LinkedHashSet<ClientlibLink> processedElements) {
        super(owner, context.getClientlibService(), context.getResolver(), processedElements);
        this.context = context;
        this.linksToRender = linksToRender != null ? linksToRender : new ArrayList<ClientlibLink>();
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
                       ClientlibResourceFolder parent) throws IOException, RepositoryException {
        if (hasEmbeddedFiles && !context.getConfiguration().getDebug()) render(mode, clientlibCategory);
        else context.registerClientlibLink(clientlibCategory.makeLink());
    }

    @Override
    public void action(Clientlib clientlib, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws
            IOException, RepositoryException {
        if (hasEmbeddedFiles && !context.getConfiguration().getDebug()) render(mode, clientlib);
        else context.registerClientlibLink(clientlib.makeLink());
    }

    @Override
    public void action(ClientlibFile file, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) {
        render(mode, file);
    }

    protected void render(VisitorMode mode, ClientlibElement element) {
        ClientlibLink link = element.makeLink();
        if (owner == element) link = link.withHash(getHash());
        if (context.isClientlibRendered(element.getRef())) {
            if (EMBEDDED == mode) {
                LOG.error("Already rendered / embedded file is also embedded in clientlib {} and thus included twice:" +
                                " {}", owner, link);
            }
        } else {
            if (DEPENDS == mode || context.getConfiguration().getDebug())
                linksToRender.add(link);
            context.registerClientlibLink(link);
        }
    }

    @Override
    public void action(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
        LOG.trace(">>> {} {}", mode, externalUri);
        if (!context.isClientlibRendered(externalUri.getRef())) {
            ClientlibLink link = externalUri.makeLink();
            linksToRender.add(link); // external references can't be embedded
            context.registerClientlibLink(link);
        }
        LOG.trace("<<< {} {}", mode, externalUri);
    }

    public List<ClientlibLink> getLinksToRender() {
        return linksToRender;
    }
}
