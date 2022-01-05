package com.composum.sling.core.service;

import com.google.gson.JsonElement;
import org.apache.sling.api.SlingHttpServletRequest;

import org.jetbrains.annotations.NotNull;
import java.io.Reader;
import java.io.Writer;

/**
 * Translates all strings in a JSON.
 */
public interface TranslationService {

    /**
     *
     */
    @NotNull
    JsonElement translate(@NotNull SlingHttpServletRequest request, @NotNull JsonElement element);

    /**
     *
     */
    @NotNull
    JsonElement translate(@NotNull SlingHttpServletRequest request, @NotNull Reader reader);

    /**
     *
     */
    void translate(@NotNull SlingHttpServletRequest request,
                   @NotNull Reader reader, @NotNull Writer writer);
}
