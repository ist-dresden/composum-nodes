package com.composum.sling.core.service;

import com.composum.sling.core.ResourceHandle;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionManager;

/**
 * Hook for performing operations before a version is checked in by the Composum console
 * {@link com.composum.sling.nodes.servlet.VersionServlet},
 * e.g. saving some metadata on the checked in resource. (This also called during the checkpoint operation).
 * All OSGI-services implementing this will be called on checkin.
 *
 * @see com.composum.sling.nodes.servlet.VersionServlet
 */
public interface VersionCheckinPreprocessor {


    /**
     * This is called before the resource is checked in.
     *
     * @param request        the request on which this is checked in
     * @param session        the current users session
     * @param versionManager the version manager
     * @param resource       the resource that should be checked in . Is null if the requested resource isn't present.
     */
    void beforeCheckin(@Nonnull SlingHttpServletRequest request, @Nonnull JackrabbitSession session, VersionManager versionManager, @Nullable ResourceHandle resource) throws RepositoryException, PersistenceException;

    /**
     * This is called after the resource is checked in. Possibly it's already checked out again (if {@link VersionManager#checkpoint(String)} was used.)
     *
     * @param request        the request on which this is checked in
     * @param session        the current users session
     * @param versionManager the version manager
     * @param resource       the resource that should be checked in . Is null if the requested resource isn't present.
     */
    void afterCheckin(SlingHttpServletRequest request, JackrabbitSession session, VersionManager versionManager, ResourceHandle resource) throws RepositoryException, PersistenceException;

}
