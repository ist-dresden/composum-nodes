package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.service.ClientlibRenderer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Contains the default implementation for link rendering.
 */
public abstract class AbstractClientlibRenderer implements ClientlibRenderer {

    private static final Logger LOG = getLogger(AbstractClientlibRenderer.class);

    /** The link template we use when rendering. */
    protected abstract String getLinkTemplate();

    @Override
    public void renderClientlibLinks(ClientlibElement clientlib,
                                     Writer writer, SlingHttpServletRequest request, RendererContext context)
            throws IOException, RepositoryException {
        renderClientlibLinks(clientlib, writer, request, context, getLinkTemplate());
    }

    protected void renderClientlibLinks(ClientlibElement clientlib,
                                        Writer writer, SlingHttpServletRequest request, RendererContext context,
                                        String template)
            throws IOException, RepositoryException {
        RenderingVisitor visitor = new RenderingVisitor(clientlib, context).execute();
        List<ClientlibLink> links = visitor.getLinksToRender();
        LOG.debug("Links to render: {}", links);
        for (int i = 0; i < links.size(); ) {
            ClientlibLink link = links.get(i);
            String rel = link.properties.get(ClientlibLink.PROP_REL); // unused in some formats.
            String renderedLink = MessageFormat.format(template, link.getUrl(request, context), rel != null ? rel : "");
            LOG.trace("Rendered link {}", renderedLink);
            writer.append(renderedLink);
            if (++i < links.size()) {
                writer.append('\n');
            }
        }
    }

}
