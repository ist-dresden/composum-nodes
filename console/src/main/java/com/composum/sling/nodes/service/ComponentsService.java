package com.composum.sling.nodes.service;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ComponentsService {

    /**
     * Creates or replaces an overlay component for the specified component type.
     *
     * @param resolver     the resolver to use
     * @param templateType the component type as resource type (relative) or as absolute path
     * @return the resource of the created overlay
     * @throws PersistenceException an error has been detected
     */
    @Nullable
    Resource createOverlay(@Nonnull ResourceResolver resolver, @Nonnull String templateType)
            throws PersistenceException;

    /**
     * Deletes an overlay component of the specified component type.
     *
     * @param resolver     the resolver to use
     * @param templateType the component type as resource type (relative) or as absolute path
     * @return 'true' of the deletion was succesful'
     * @throws PersistenceException an error has been detected
     */
    boolean removeOverlay(@Nonnull ResourceResolver resolver, @Nonnull String overlayType)
            throws PersistenceException;
}
