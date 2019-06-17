package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibCategory;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.processor.RendererContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Calendar;

/**
 * Various internal functionality about client libraries.
 */
public interface ClientlibService {

    String ENCODING_GZIP = "gzip";

    /**
     * Resolves the element corresponding to the ref.
     *
     * @return the corresponding element or null if we can't find it.
     */
    ClientlibElement resolve(ClientlibRef ref, ResourceResolver resolver);

    /* Returns the minified sibling (e.g. ending .min.js if resource ends with .js) if there is one, otherwise
    resource itself is returned. */
    @Nonnull
    Resource getMinifiedSibling(@Nonnull Resource resource);

    ClientlibConfiguration getClientlibConfig();

    /**
     * Renders the references to the ressources of the clientlibs into the page.
     *
     * @param clientlib a {@link Clientlib} or {@link ClientlibCategory}
     */
    void renderClientlibLinks(ClientlibElement clientlib, Writer writer, SlingHttpServletRequest request,
                              RendererContext context) throws IOException, RepositoryException;

    /**
     * Writes the content to a cache if it wasn't there, and returns a collection of information to be put into the
     * response headers.
     *
     * @param clientlibRef      reference to the clientlib / category to render
     * @param minified          when true, the minified version
     * @param encoding          the needed encoding, if applicable
     * @param forceRefreshCache if true, the cache will be refreshed even if it is up to date
     * @param requestedHash     the hash value the client requested
     * @param ifModifiedSince   the value of the If-Modified-Since header, if present
     * @return information about the file
     */
    ClientlibInfo prepareContent(SlingHttpServletRequest request, ClientlibRef clientlibRef, boolean minified, String
            encoding, boolean forceRefreshCache, String requestedHash, long ifModifiedSince) throws IOException,
            RepositoryException;

    /**
     * Writes the cached content to outputStream.
     *
     * @param clientlibRef reference to the clientlib / category to render
     * @param minified     when true, the minified version
     * @param outputStream the stream to write to
     * @param encoding     the needed encoding, if applicable
     */
    void deliverContent(ResourceResolver resolver, ClientlibRef clientlibRef, boolean minified, OutputStream
            outputStream, String encoding) throws IOException, RepositoryException;

    /**
     * Generates a human readable descriptions of inconsistencies of the client libraries wrt. the given resolver
     * (or an anonymous resolver, of none is given).
     * Checks for each client library readable for the given resolver, that all elements of these client libraries
     * and the files referenced from them are readable, too. Also we check that for all categories either all
     * client libraries with this reference are readable, or none of them.
     * All these are errors, since if that's not the case, this can cause a permanent recalculation of the clientlibs content,
     * or, even worse, break the rendering.
     * If {onlyErrors} is false, we also include informational messages about all unreadable libraries.
     *
     *
     * @param type     if not null, only clientlibs / files of that type are checked
     * @param resolver if not null, we use this resolver instead of an anonymous resolver to check readability
     * @param onlyErrors if true, we skip informational messages
     * @return null if everything is OK, otherwise a description of the problems
     */
    @Nullable
    String verifyClientlibPermissions(@Nullable Clientlib.Type type, @Nullable ResourceResolver resolver, boolean onlyErrors);

    /**
     * Clears the whole cache for all clientlibs. Obviously something to used sparingly.
     *
     * @param resolver the resolver to use.
     */
    void clearCache(ResourceResolver resolver) throws PersistenceException;

    class ClientlibInfo {
        public Long size;
        public Calendar lastModified;
        public String mimeType;
        public String encoding;
        public String hash;
        public ClientlibLink link;

        @Override
        public String toString() {
            return "ClientlibInfo{" + "size=" + size + ", lastModified=" + lastModified + ", mimeType='" + mimeType +
                    '\'' + ", encoding='" + encoding + '\'' + ", hash='" + hash + '\'' + ", link=" + link + '}';
        }
    }

}
