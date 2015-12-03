package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.tika.mime.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

/**
 * The service servlet handling one single JCR property for one resource.
 */
@SlingServlet(
        paths = "/bin/core/property",
        methods = {"GET", "POST", "PUT"}
)
public class PropertyServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyServlet.class);

    public static final StringFilter DEFAULT_PROPS_FILTER = new StringFilter.BlackList(new String[0]);
    public static final StringFilter BINARY_PROPS_FILTER = new StringFilter.BlackList(new String[0]);

    @Reference
    private CoreConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {json, bin}

    public enum Operation {get, put, map, copy, remove}

    protected ServletOperationSet operations = new ServletOperationSet(Extension.json);

    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.get, new GetOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.map, new MapGetOperation());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.bin,
                Operation.get, new GetBinaryOperation());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.bin,
                Operation.put, new PostBinaryOperation());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.put, new PutOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.copy, new CopyOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.bin,
                Operation.put, new PutBinaryOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.remove, new RemoveOperation());
    }

    protected class MapGetOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            StringFilter filter = resource.isFile() ? BINARY_PROPS_FILTER : DEFAULT_PROPS_FILTER;

            try {
                MappingRules mapping = new MappingRules(MappingRules.getDefaultMappingRules(),
                        null, null, null, new MappingRules.PropertyFormat(
                        RequestUtil.getParameter(request, "format",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Scope.definition)),
                        RequestUtil.getParameter(request, "binary",
                                RequestUtil.getSelector(request, MappingRules.PropertyFormat.Binary.link))),
                        null, null);

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);

                response.setStatus(HttpServletResponse.SC_OK);

                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    JsonUtil.writeJsonProperties(jsonWriter, filter, node, mapping);
                } else {
                    ValueMap values = ResourceUtil.getValueMap(resource);
                    JsonUtil.writeJsonValueMap(jsonWriter, filter, values, mapping);
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected class GetOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);

                String name = request.getParameter(PARAM_NAME);
                if (StringUtils.isNotBlank(name)) {

                    response.setStatus(HttpServletResponse.SC_OK);

                    ResponseUtil.writeJsonProperty(response, node, name);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no property name parameter found");
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected class PutOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);

                if (node != null) {
                    Session session = node.getSession();
                    ValueFactory valueFactory = session.getValueFactory();

                    // parse property from JSON into a POJO of type JsonProperty
                    JsonUtil.JsonProperty property = getJsonObject(request, JsonUtil.JsonProperty.class);

                    // update the property
                    boolean available = JsonUtil.setJsonProperty(valueFactory, node, property,
                            ResponseUtil.getDefaultJsonMapping());
                    session.save();

                    // answer 'OK' (200)
                    response.setStatus(HttpServletResponse.SC_OK);

                    if (available) {
                        // answer with property reloaded and transformed to JSON
                        response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                        ResponseUtil.writeJsonProperty(response, node, property.name);
                    } else {
                        // empty answer for a successful request (possible a deletion)
                        response.setContentLength(0);
                    }

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "can't determine node '" + resource.getPath() + "'");
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected static class BulkParameters {

        public String path;
        public List<String> names;
    }

    protected abstract class BulkOperation implements ServletOperation {

        protected class Result {
            public Object result;
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Node node = resource.adaptTo(Node.class);

            if (node != null) {

                try {
                    // parse parameters from JSON into a POJO of type BulkParameters
                    BulkParameters parameters = getJsonObject(request, BulkParameters.class);

                    response.setStatus(HttpServletResponse.SC_OK);
                    JsonWriter writer = ResponseUtil.getJsonWriter(response);
                    if (parameters != null) {
                        doIt(request, response, resource, node, parameters, writer);
                    }

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                }

            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "can't determine node '" + resource.getPath() + "'");
            }
        }

        protected abstract void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                     ResourceHandle resource, Node node, BulkParameters parameters,
                                     JsonWriter writer)
                throws RepositoryException, ServletException, IOException;
    }

    protected class RemoveOperation extends BulkOperation {

        protected class Result {
            public List<String> names;
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource, Node node, BulkParameters parameters,
                         JsonWriter writer)
                throws RepositoryException, ServletException, IOException {

            Session session = node.getSession();
            writer.beginObject();

            if (parameters.names != null) {
                writer.name("removed").beginArray();
                for (String name : parameters.names) {
                    try {
                        Property property = node.getProperty(name);
                        if (property != null) {
                            if (property.isMultiple()) {
                                node.setProperty(name, (Value[]) null);
                            } else {
                                node.setProperty(name, (Value) null);
                            }
                        }
                    } catch (PathNotFoundException pnfex) {
                    }
                    writer.value(name);
                }
                writer.endArray();
            }

            writer.endObject();
            session.save();
        }
    }

    protected class CopyOperation extends BulkOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource, Node node, BulkParameters parameters,
                         JsonWriter writer)
                throws RepositoryException, ServletException, IOException {

            if (parameters.path != null) {
                Session session = node.getSession();
                writer.beginObject();

                Node template = session.getNode(parameters.path);

                if (template != null && parameters.names != null) {
                    writer.name("copied").beginArray();

                    for (String name : parameters.names) {
                        try {
                            Property property = template.getProperty(name);
                            if (property != null) {
                                try {
                                    Property oldProperty = node.getProperty(name);
                                    if (oldProperty != null) {
                                        if (oldProperty.isMultiple()) {
                                            node.setProperty(name, (Value[]) null);
                                        } else {
                                            node.setProperty(name, (Value) null);
                                        }
                                    }
                                } catch (PathNotFoundException pnfex) {
                                }
                                if (property.isMultiple()) {
                                    node.setProperty(name, property.getValues());
                                } else {
                                    node.setProperty(name, property.getValue());
                                }
                                writer.value(name);
                            }
                        } catch (PathNotFoundException pnfex) {
                        }
                    }
                    writer.endArray();
                }

                writer.endObject();
                session.save();
            }
        }
    }

    //
    // Binary properties
    //

    protected class GetBinaryOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    String name = RequestUtil.getParameter(request, PARAM_NAME, com.composum.sling.core.util.ResourceUtil.PROP_DATA);

                    javax.jcr.Property property = node.getProperty(name);
                    Binary binary = property != null ? property.getBinary() : null;
                    if (binary != null) {

                        try {
                            MimeType mimeType = MimeTypeUtil.getMimeType(resource);
                            if (mimeType != null) {
                                response.setContentType(mimeType.toString());
                            }
                            String filename = MimeTypeUtil.getFilename(resource, null);
                            if (StringUtils.isNotBlank(filename)) {
                                response.setHeader("Content-Disposition", "inline; filename=" + filename);
                            }

                            Calendar lastModified = resource.getProperty(com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED, Calendar.class);
                            if (lastModified != null) {
                                response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
                            }

                            response.setContentLength((int) binary.getSize());
                            response.setStatus(HttpServletResponse.SC_OK);

                            InputStream input = binary.getStream();
                            BufferedInputStream buffered = new BufferedInputStream(input);
                            try {
                                IOUtils.copy(buffered, response.getOutputStream());
                            } finally {
                                buffered.close();
                                input.close();
                            }
                        } finally {
                            binary.dispose();
                        }

                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "no binary '" + name + "' property found");
                    }
                } else {
                    LOG.error(resource.getPath() + ": invalid binary GET - resource has no content node");
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "can't determine file node '" + resource.getPath() + "'");
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    // receiving Binary update ...

    /**
     * The POST (multipart form) implementation expects:
     * <ul>
     * <li>the 'file' part (form element / parameter) with the binary content</li>
     * <li>an optional 'name' parameter for the property name (default 'jcr:data')</li>
     * </ul>
     */
    protected class PostBinaryOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    RequestParameterMap parameters = request.getRequestParameterMap();

                    RequestParameter file = parameters.getValue(PARAM_VALUE);
                    if (file != null) {

                        RequestParameter nameParam = parameters.getValue(PARAM_NAME);
                        String name = nameParam != null ? nameParam.getString() : com.composum.sling.core.util.ResourceUtil.PROP_DATA;

                        LOG.info(resource.getPath() + ": update POST for binary property '" + name + "'");

                        Session session = node.getSession();
                        InputStream input = file.getInputStream();
                        PropertyUtil.setProperty(node, name, input);
                        session.save();

                        response.setContentLength(0);
                        response.setStatus(HttpServletResponse.SC_OK);

                    } else {
                        LOG.error(resource.getPath() + ": invalid file update POST - no file/binary content");
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no binary/file content");
                    }
                } else {
                    LOG.error(resource.getPath() + ": invalid file update POST - resource has no content node");
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "can't determine file node '" + resource.getPath() + "'");
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected class PutBinaryOperation implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    Session session = node.getSession();
                    InputStream input = request.getInputStream();
                    PropertyUtil.setProperty(node, com.composum.sling.core.util.ResourceUtil.PROP_DATA, input);
                    session.save();

                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_OK);

                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "can't determine file node '" + resource.getPath() + "'");
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }
}
