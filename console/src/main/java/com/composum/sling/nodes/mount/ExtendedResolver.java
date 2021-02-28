package com.composum.sling.nodes.mount;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public interface ExtendedResolver extends ResourceResolver {

    /**
     * move with optional rename and ordering
     *
     * @param srcAbsPath        the absolute path of th resource to move
     * @param destParentAbsPath the absolute path of the designated parent resource
     * @param destChildName     the designated name of the new resource
     * @param order             an ordering rule as described for the SlingPostServlet
     */
    Resource move(@Nonnull String srcAbsPath, @Nonnull String destParentAbsPath,
                  @Nullable String destChildName, @Nullable String order)
            throws PersistenceException;

    /**
     * uploads the content of a file to update or create a file resource
     *
     * @param absPath  the absolute path oof the file resource to update
     * @param content  the new file content
     * @param filename the name of the uploaded file
     * @param mimeType the mime type of the content if known
     * @param charset  the charset of the content
     */
    Resource upload(@Nonnull final String absPath, @Nonnull final InputStream content,
                    @Nullable final String filename, @Nullable final String mimeType,
                    @Nullable final String charset)
            throws PersistenceException;
}
