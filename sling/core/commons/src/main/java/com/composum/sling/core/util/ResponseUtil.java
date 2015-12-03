package com.composum.sling.core.util;

import com.composum.sling.core.mapping.MappingRules;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingHttpServletResponse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A basic class for all '/bin/{service}/path/to/resource' servlets.
 */
public class ResponseUtil extends org.apache.sling.api.request.RequestUtil {

    public static final String JSON_CONTENT_TYPE = "application/json;charset=" + MappingRules.CHARSET;

    /**
     * the default rule set for general import an export features
     */
    public static MappingRules getDefaultJsonMapping() {
        return new MappingRules(MappingRules.getDefaultMappingRules().MAPPING_NODE_FILTER,
                MappingRules.MAPPING_EXPORT_FILTER, MappingRules.MAPPING_IMPORT_FILTER,
                new MappingRules.PropertyFormat(MappingRules.PropertyFormat.Scope.definition,
                        MappingRules.PropertyFormat.Binary.link),
                0, MappingRules.ChangeRule.update);
    }

    //
    // JSON streaming
    //

    public static JsonWriter getJsonWriter(SlingHttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(MappingRules.CHARSET.name());
        PrintWriter writer = response.getWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(true);
        return jsonWriter;
    }

    public static void writeEmptyArray(SlingHttpServletResponse response) throws IOException {
        try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
            jsonWriter.beginArray().endArray();
        }
    }

    /**
     * Write one JCR property as JSON object back using the writer of the response (used for GET and PUT).
     *
     * @param response the HTTP response with the writer
     * @param node     the JCR node of the referenced resource
     * @param name     the name of the property requested
     * @throws RepositoryException error on accessing JCR
     * @throws IOException         error on write JSON
     */
    public static void writeJsonProperty(SlingHttpServletResponse response, Node node, String name)
            throws RepositoryException, IOException {

        JsonWriter jsonWriter = getJsonWriter(response);

        javax.jcr.Property property = node.getProperty(name);
        if (property != null) {
            JsonUtil.writeJsonProperty(jsonWriter, node, property, getDefaultJsonMapping());
        }
    }
}
