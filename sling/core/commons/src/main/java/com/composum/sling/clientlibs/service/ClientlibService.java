package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.processor.RendererContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

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
    Resource getMinifiedSibling(Resource resource);

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
     * Checks that all clientlib items are anonymously readable, and logs and returns a human readable description
     * of the results. If that's not the case, this can cause a permanent recalculation of the clientlibs content, or, even worse, break the rendering.
     *
     * @param type  if not null, only clientlibs / files of that type are checked
     * @param force if false, the check is done only if the last run was more than an hour ago. If true, it's done in any case.
     * @return null if everything is OK, otherwise a description of the problems
     */
    String verifyClientlibPermissions(Clientlib.Type type, boolean force);

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
