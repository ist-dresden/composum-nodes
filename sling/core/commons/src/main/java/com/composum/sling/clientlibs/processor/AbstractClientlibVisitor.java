package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode.DEPENDS;
import static com.composum.sling.clientlibs.handle.ClientlibVisitor.VisitorMode.EMBEDDED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for visitors to aid processing of all parts of a client library.
 */
public abstract class AbstractClientlibVisitor implements ClientlibVisitor {

    /** Matches the results of {@link #getHash()}. */
    public static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-zA-Z_-]{11}");

    protected static final Logger LOG = getLogger(AbstractClientlibVisitor.class);
    protected final ClientlibService service;
    protected final ResourceResolver resolver;
    protected final LinkedHashSet<ClientlibLink> processedElements;

    /** The clientlib or category which this visitor processes - i.e., which owns embedded stuff. */
    protected final ClientlibElement owner;
    protected long embeddedHash = 0;

    protected boolean hasEmbeddedFiles = false;

    @Override
    public ClientlibElement getOwner() {
        return owner;
    }

    @Override
    public ClientlibVisitor execute() throws IOException, RepositoryException {
        owner.accept(this, DEPENDS, null);
        return this;
    }

    protected AbstractClientlibVisitor(ClientlibElement owner, ClientlibService service, ResourceResolver resolver,
                                       LinkedHashSet<ClientlibLink> processedElements) {
        this.service = service;
        this.resolver = resolver;
        this.owner = owner;
        this.processedElements = null != processedElements ? processedElements : new LinkedHashSet<ClientlibLink>();
    }

    /**
     * Returns the appropriate visitor for the given mode and element: if mode=DEPENDS and element is a clientlib or
     * category.
     */
    protected ClientlibVisitor getVisitorFor(VisitorMode mode, ClientlibElement element) {
        if (EMBEDDED == mode) return this;
        if (element instanceof ClientlibCategory || element instanceof Clientlib) return createVisitorFor(element);
        return this;
    }

    /**
     * Creates a new visitor for the given clientlib or category as owner. That'll usually be of the same type as
     * ourselves. Caution: they have to share the same {@link #processedElements} set!
     */
    protected abstract ClientlibVisitor createVisitorFor(ClientlibElement element);

    @Override
    public void visit(ClientlibCategory category, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent)
            throws IOException, RepositoryException {
        LOG.debug(">>> {} {}", mode, category);
        if (notProcessed(category.getRef())) {
            for (Clientlib clientlib : category.clientlibs)
                clientlib.accept(this, EMBEDDED, null);
            action(category, mode, parent);
            markAsProcessed(category.makeLink());
        }
        LOG.debug("<<< {} => {}", category, hasEmbeddedFiles);
    }

    @Override
    public void visit(Clientlib clientlib, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder parent) throws
            IOException, RepositoryException {
        LOG.debug(">>> {} {}", mode, clientlib);
        if (notProcessed(clientlib.getRef())) {
            updateHash(clientlib.resource.getPath(), clientlib.resource.getLastModified());
            ClientlibResourceFolder folder = clientlib.getResourceFolder();
            if (null != folder) folder.accept(this, mode, null);
            action(clientlib, mode, parent);
            markAsProcessed(clientlib.makeLink());
        }
        LOG.debug("<<< {} => {}", clientlib, hasEmbeddedFiles);
    }

    @Override
    public void visit(ClientlibResourceFolder folder, ClientlibVisitor.VisitorMode mode, ClientlibResourceFolder
            parent) throws IOException, RepositoryException {
        LOG.debug(">>> {} {}", mode, folder);
        updateHash(folder.resource.getPath(), folder.resource.getLastModified());
        for (ClientlibRef dependency : folder.getDependencies())
            resolveAndAccept(dependency, DEPENDS, folder);
        boolean embedding = supportsEmbedding(folder.getType()) && !folder.getExpanded();
        VisitorMode embeddingMode = embedding ? EMBEDDED : DEPENDS;
        for (ClientlibRef embedded : folder.getEmbedded())
            resolveAndAccept(embedded, embeddingMode, folder);
        for (ClientlibElement child : folder.getChildren())
            child.accept(getVisitorFor(mode, child), embeddingMode, folder);
        LOG.debug("<<< {} => {}", folder, hasEmbeddedFiles);
    }

    @Override
    public void visit(ClientlibFile file, VisitorMode mode, ClientlibResourceFolder parent) throws
            RepositoryException, IOException {
        LOG.debug(">>> {} {}", mode, file);
        if (notProcessed(file.getRef())) {
            if (EMBEDDED == mode) {
                updateHash(file.handle.getPath(), file.handle.getLastModified());
                hasEmbeddedFiles = true;
            }
            action(file, mode, parent);
            markAsProcessed(file.makeLink());
        }
        LOG.debug("<<< {} {}", mode, file);
    }

    @Override
    public void visit(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
        LOG.debug(">>> {} {}", mode, externalUri);
        if (notProcessed(externalUri.getRef())) {
            action(externalUri, mode, parent);
            markAsProcessed(externalUri.makeLink());
        }
        // never embedded -> irrelevant for hash.
        LOG.debug("<<< {} {}", mode, externalUri);
    }

    protected void resolveAndAccept(ClientlibRef ref, VisitorMode mode, ClientlibResourceFolder folder) throws
            IOException, RepositoryException {
        if (notProcessed(ref)) {
            ClientlibElement element = service.resolve(ref, resolver);
            if (null != element) element.accept(getVisitorFor(mode, element), mode, folder);
        } else {
            alreadyProcessed(ref, mode, folder);
        }
    }

    /** Hook for additional checks about already processed elements. */
    protected void alreadyProcessed(ClientlibRef ref, VisitorMode mode, ClientlibResourceFolder folder) {
        // empty here.
    }

    protected void updateHash(String path, Calendar updatetime) {
        LOG.debug("Hashing {} with {}", path, null != updatetime ? updatetime.getTime() : null);
        embeddedHash = embeddedHash * 92821 + path.hashCode();
        if (null != updatetime) {
            embeddedHash = embeddedHash * 92821 + updatetime.getTimeInMillis();
        }
    }

    /**
     * Hash consisting of the updatetimes and paths of all embedded files, which should be
     * sufficient to identify any changes as a strong HTTP ETag.
     * That's an URL-safe string with 11 characters containing digits, letters and - or _ .
     */
    public String getHash() {
        long h = embeddedHash;
        byte[] b = new byte[8];
        for (int i = 0; i < 8; ++i) {
            b[i] = (byte) h;
            h = h >> 8;
        }
        return Base64.encodeBase64URLSafeString(b);
    }

    /** Checks whether something matching this reference has already been {@link #markAsProcessed(ClientlibLink)}. */
    protected boolean notProcessed(ClientlibRef ref) {
        return !ref.isSatisfiedby(processedElements);
    }

    protected void markAsProcessed(ClientlibLink link) {
        if (processedElements.contains(link)) {
            LOG.error("Bug: duplicate clientlib link: {}", link);
        } else {
            processedElements.add(link);
            LOG.debug("registered: {}", link);
        }
    }

    public Set<ClientlibLink> getProcessedElements() {
        return processedElements;
    }

    protected boolean supportsEmbedding(Clientlib.Type type) {
        return Clientlib.Type.js == type || Clientlib.Type.css == type;
    }

    /** Optional action to take after visiting the element. Default : empty. */
    protected void action(ClientlibCategory clientlibCategory, VisitorMode mode, ClientlibResourceFolder parent)
            throws IOException, RepositoryException {
    }

    /** Optional action to take after visiting the element. Default : empty. */
    protected void action(Clientlib clientlib, VisitorMode mode, ClientlibResourceFolder parent) throws IOException,
            RepositoryException {
    }

    /** Optional action to take after visiting the element. Default : empty. */
    protected void action(ClientlibResourceFolder folder, VisitorMode mode, ClientlibResourceFolder parent) throws
            IOException, RepositoryException {
    }

    /** Optional action to take after visiting the element. Default : empty. */
    protected void action(ClientlibFile file, VisitorMode mode, ClientlibResourceFolder parent) throws
            RepositoryException, IOException {
    }

    /** Optional action to take after visiting the element. Default : empty. */
    protected void action(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
    }

}
