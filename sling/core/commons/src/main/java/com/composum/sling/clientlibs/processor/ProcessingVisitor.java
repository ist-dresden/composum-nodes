package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibFile;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.handle.ClientlibResourceFolder;
import com.composum.sling.clientlibs.handle.ClientlibVisitor;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;

import static com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode.EMBEDDED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Appends all embedded files to an input stream.
 */
public class ProcessingVisitor extends AbstractClientlibVisitor {

    private static final Logger LOG = getLogger(ProcessingVisitor.class);

    protected final OutputStream output;
    protected final ClientlibProcessor processor;
    protected final ProcessorContext context;

    /**
     * Instantiates a new Processing visitor.
     *
     * @param service   the service
     * @param output    the output stream to write to. Is not closed in this class - remember to close it outside.
     * @param processor optional processor we pipe our output through.
     * @param context   the context where we keep some data
     */
    public ProcessingVisitor(ClientlibElement owner, ClientlibService service, OutputStream output,
                             ClientlibProcessor processor, ProcessorContext context) {
        super(owner, service, context.getResolver(), null);
        this.output = output;
        this.processor = processor;
        this.context = context;
    }

    @Override
    protected ClientlibVisitor createVisitorFor(ClientlibElement element) {
        return new ExcludeDependenciesVisitor(element, service, resolver, processedElements);
    }

    @Override
    public void action(ClientlibFile clientlibFile, VisitorMode mode, ClientlibResourceFolder parent)
            throws IOException {
        if (EMBEDDED != mode) return;
        LOG.trace("Processing {} with {}", clientlibFile, processor);
        long begin = System.currentTimeMillis();
        Resource resource = clientlibFile.handle.getResource();
        if (context.useMinifiedFiles()) {
            resource = service.getMinifiedSibling(resource);
        }
        FileHandle file = new FileHandle(resource);
        InputStream content = file.getStream();
        if (content != null) {
            try {
                if (processor != null) {
                    content = processor.processContent(content, context);
                }
                IOUtils.copy(content, output);
                output.write('\n');
                output.write('\n');
                output.flush();
            } finally {
                content.close();
            }
        } else {
            logNotAvailable(resource, "[content]", parent.getOptional());
        }
        float time = 0.001f * (System.currentTimeMillis() - begin);
        LOG.trace("Processed {} in {} s", clientlibFile, time);
        if (time > 0.1) { LOG.info("Large processing time: {} in {} s", clientlibFile, time); }
    }

    protected void logNotAvailable(Resource resource, String reference, boolean optional) {
        if (optional)
            LOG.debug("Clientlib entry ''{}'' of ''{}'' not available but optional.", reference, resource.getPath());
        else LOG.warn("Clientlib entry ''{}'' of ''{}'' not available.", reference, resource.getPath());
    }

    /** Warns about everything that should be embedded, but is already processed, and not in this */
    @Override
    protected void alreadyProcessed(ClientlibRef ref, VisitorMode mode, ClientlibResourceFolder folder) {
        if (mode == EMBEDDED) {
            LOG.warn("Trying to embed already embedded / dependency {} again at {} from {}", new Object[]{ref, folder, owner});
        }
    }

    /**
     * If some files are included in / requested by dependencies of the rendered client library, these must not be
     * included into the cached file, since these would be loaded twice by the page. Thus, all dependencies are
     * processed by this visitor. We need a separate visitor as to note embedded stuff in client libraries that are
     * dependencies of the processed library has to be noted as well, and this cannot easily be distinguished by the
     * visit methods arguments.
     * <p>
     * This visitor is necessary since the dependencies have to be processed as well since they might override
     * file versions or exclude stuff from embedding.
     */
    protected static class ExcludeDependenciesVisitor extends AbstractClientlibVisitor {

        protected ExcludeDependenciesVisitor(ClientlibElement owner, ClientlibService service, ResourceResolver resolver, LinkedHashSet<ClientlibLink> processedElements) {
            super(owner, service, resolver, processedElements);
        }

        @Override
        protected ClientlibVisitor createVisitorFor(ClientlibElement element) {
            return new ExcludeDependenciesVisitor(element, service, resolver, processedElements);
        }

    }
}
