package com.composum.sling.nodes.servlet;

import com.composum.sling.core.Restricted;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.XSS;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.composum.sling.nodes.servlet.ExportQueryJson.SERVICE_KEY;
import static com.composum.sling.nodes.servlet.NodeServlet.getJsonSelectorIndent;
import static com.composum.sling.nodes.servlet.NodeServlet.getJsonSelectorRules;

/**
 * a servlet to export the results of a query execution as a JSON object with emebedded object for each
 * found resource - the resource type of this servlet is used in the 'export set' configuration; see:
 * /libs/composum/nodes/browser/query/export/json
 */
@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Export Query Json Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=composum/nodes/browser/query/export/json/objects",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
@Restricted(key = SERVICE_KEY)
public class ExportQueryJson extends SlingAllMethodsServlet implements RestrictedService {

    private static final Logger LOG = LoggerFactory.getLogger(ExportQueryJson.class);

    public static final String SERVICE_KEY = "nodes/query/export";

    @Reference
    private ServiceRestrictions restrictions;

    @Override
    @NotNull
    public ServiceRestrictions.Key getServiceKey() {
        return new ServiceRestrictions.Key(SERVICE_KEY);
    }

    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request,
                          @NotNull SlingHttpServletResponse response)
            throws IOException {

        if (restrictions.isPermissible(request, getServiceKey(), ServiceRestrictions.Permission.read)) {

            Resource resource = request.getResource();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json; charset=UTF-8");

            String filename = XSS.filter(request.getRequestPathInfo().getSuffix());
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
                ValueMap values = resource.getValueMap();
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

        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }
}
