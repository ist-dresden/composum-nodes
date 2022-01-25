package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.TranslationService;
import com.composum.sling.core.util.I18N;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import org.jetbrains.annotations.NotNull;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Translations Service"
        }
)
public class CoreTranslationService implements TranslationService {

    protected static Gson gson = new GsonBuilder().create();

    @Override
    @NotNull
    public JsonElement translate(@NotNull final SlingHttpServletRequest request, @NotNull final JsonElement element) {
        if (element instanceof JsonObject) {
            JsonObject translated = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
                translated.add(entry.getKey(), translate(request, entry.getValue()));
            }
            return translated;
        } else if (element instanceof JsonArray) {
            JsonArray translated = new JsonArray();
            for (JsonElement entry : ((JsonArray) element)) {
                translated.add(translate(request, entry));
            }
            return translated;
        } else if (element instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) element;
            if (primitive.isString()) {
                primitive = new JsonPrimitive(I18N.get(request, primitive.getAsString()));
            }
            return primitive;
        }
        return element;
    }

    @Override
    @NotNull
    public JsonElement translate(@NotNull final SlingHttpServletRequest request, @NotNull final Reader reader) {
        return translate(request, gson.fromJson(reader, JsonElement.class));
    }

    @Override
    public void translate(@NotNull final SlingHttpServletRequest request,
                          @NotNull final Reader reader, @NotNull final Writer writer) {
        JsonElement element = translate(request, reader);
        gson.toJson(element, writer);
    }
}
