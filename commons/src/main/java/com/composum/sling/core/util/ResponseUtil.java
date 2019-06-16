package com.composum.sling.core.util;

import com.composum.sling.core.mapping.MappingRules;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A basic class for all '/bin/{service}/path/to/resource' servlets.
 */
public class ResponseUtil {

    public static final String JSON_CONTENT_TYPE = "application/json;charset=" + MappingRules.CHARSET;

    public static String getMessage(Throwable ex) {
        StringBuilder builder = new StringBuilder();
        String msg;
        if (StringUtils.isNotBlank(msg = ex.getMessage())) {
            builder.append(msg);
        } else if (ex instanceof AccessDeniedException ||
                ex instanceof IllegalAccessException ||
                ex instanceof LoginException) {
            builder.append("access denied");
        } else {
            Throwable cause = ex.getCause();
            if (cause != null) {
                builder.append(getMessage(cause));
            } else {
                builder.append("server error");
            }
        }
        return builder.toString();
    }

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

    public static void writeEmptyObject(SlingHttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
            jsonWriter.beginObject().endObject();
        }
    }

    public static void writeEmptyArray(SlingHttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
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
