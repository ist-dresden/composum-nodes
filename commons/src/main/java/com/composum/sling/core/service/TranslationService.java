package com.composum.sling.core.service;

import com.google.gson.JsonElement;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.annotation.Nonnull;
import java.io.Reader;
import java.io.Writer;

/**
 * Permission and Member check service
 */
public interface TranslationService {

    /**
     *
     */
    @Nonnull
    JsonElement translate(@Nonnull SlingHttpServletRequest request, @Nonnull JsonElement element);

    /**
     *
     */
    @Nonnull
    JsonElement translate(@Nonnull SlingHttpServletRequest request, @Nonnull Reader reader);

    /**
     *
     */
    void translate(@Nonnull SlingHttpServletRequest request,
                   @Nonnull Reader reader, @Nonnull Writer writer);
}
