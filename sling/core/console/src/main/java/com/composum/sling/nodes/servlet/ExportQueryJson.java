package com.composum.sling.nodes.servlet;

import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.composum.sling.nodes.servlet.NodeServlet.getJsonSelectorIndent;
import static com.composum.sling.nodes.servlet.NodeServlet.getJsonSelectorRules;

/**
 * a servlet to export the results of a query execution as a JSON object with emebedded object for each
 * found resource - the resource type of this servlet is used in the 'export set' configuration; see:
 * /libs/composum/nodes/browser/query/export/json
 */
@SlingServlet(
        resourceTypes = "composum/nodes/browser/query/export/json/objects",
        methods = {"POST"}
)
public class ExportQueryJson extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ExportQueryJson.class);

    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request,
                          @Nonnull SlingHttpServletResponse response)
            throws IOException {

        Resource resource = request.getResource();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=UTF-8");

        String filename = request.getRequestPathInfo().getSuffix();
        if (filename != null) {
            while (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
        }
        if (StringUtils.isBlank(filename)) {
            filename = "query-export.json";
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // reuse the 'selector parameters' of the JSON view; see 'selectors' in the export configuration
        int indent = getJsonSelectorIndent(request);
        JsonWriter writer = new JsonWriter(response.getWriter());
        if (indent > 0) {
            writer.setIndent(StringUtils.repeat(' ', indent));
        }

        try {
            ValueMap values = resource.adaptTo(ValueMap.class);
            // reuse the 'selector parameters' of the JSON view; see 'selectors' in the export configuration
            MappingRules rules = getJsonSelectorRules(request);
            writer.beginObject();
            writer.name("query").value(values.get("query", ""));
            for (Resource item : resource.getChildren()) {
                ValueMap valueMap = item.adaptTo(ValueMap.class);
                writer.name(item.getPath());
                JsonUtil.exportJson(writer, item, rules);
            }
            writer.endObject();

        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }
}
