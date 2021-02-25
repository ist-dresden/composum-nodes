package com.composum.sling.nodes.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.mount.ExtendedResolver;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.tika.mime.MimeType;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * The service servlet handling one single JCR property for one resource.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Property Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + PropertyServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                "sling.auth.requirements=" + PropertyServlet.SERVLET_PATH
        },
        immediate = true
)
public class PropertyServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/nodes/property";
    public static final StringFilter DEFAULT_PROPS_FILTER = new StringFilter.BlackList();
    public static final StringFilter BINARY_PROPS_FILTER = new StringFilter.BlackList();

    @Reference
    private NodesConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {json, bin}

    public enum Operation {get, put, update, map, copy, remove, xss}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    protected ServletOperationSet<Extension, Operation> getOperations() {
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
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.xss, new CheckXssOperation());
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
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.bin,
                Operation.update, new PutUpdateOperation());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.remove, new RemoveOperation());
    }

    protected class CheckXssOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            Status status = new Status(request, response);
            String[] value = request.getParameterValues(PARAM_VALUE);
            boolean result = true;
            if (value != null && value.length > 0) {
                for (int i = 0; result && i < value.length; i++) {
                    result = PropertyUtil.xssCheck(value[i]);
                }
            }
            if (!result) {
                status.warn("XSS check failed");
            }
            status.sendJson();
        }
    }

    protected class MapGetOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!(resource = AbstractServiceServlet.tryToUseRawSuffix(request, resource)).isValid()) {
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

                response.setStatus(SC_OK);

                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    JsonUtil.writeJsonProperties(resource, jsonWriter, filter, node, mapping);
                } else {
                    ValueMap values = ResourceUtil.getValueMap(resource);
                    JsonUtil.writeJsonValueMap(resource, jsonWriter, filter, values, mapping);
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    protected class GetOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                Node node = resource.adaptTo(Node.class);
                String name = XSS.filter(request.getParameter(PARAM_NAME));

                if (node != null && StringUtils.isNotBlank(name)) {

                    response.setStatus(SC_OK);
                    ResponseUtil.writeJsonProperty(resource, response, node, name);

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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                // parse property from JSON into a POJO of type JsonProperty
                JsonUtil.JsonProperty property = getJsonObject(request, JsonUtil.JsonProperty.class);

                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    Session session = node.getSession();

                    // update the property
                    boolean available = JsonUtil.setJsonProperty(node, property,
                            ResponseUtil.getDefaultJsonMapping());
                    session.save();

                    // answer 'OK' (200)
                    response.setStatus(SC_OK);

                    if (available) {
                        // answer with property reloaded and transformed to JSON
                        response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                        ResponseUtil.writeJsonProperty(resource, response, node, property.name);
                    } else {
                        // empty answer for a successful request (possible a deletion)
                        response.setContentLength(0);
                    }

                } else {

                    ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
                    if (values != null) {

                        int type = StringUtils.isNotBlank(property.type)
                                ? PropertyType.valueFromName(property.type) : PropertyType.STRING;
                        Object value = JsonUtil.makeValueObject(type, property.value);

                        if (property.oldname != null && !property.oldname.equals(property.name)) {
                            values.remove(property.oldname);
                        }
                        values.put(property.name, value);
                        resource.getResourceResolver().commit();

                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "can't modify resource (" + resource.getPath() + ")");
                    }
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // parse parameters from JSON into a POJO of type BulkParameters
            BulkParameters parameters = getJsonObject(request, BulkParameters.class);
            if (parameters != null) {
                try {
                    response.setStatus(SC_OK);
                    JsonWriter writer = ResponseUtil.getJsonWriter(response);

                    Node node = resource.adaptTo(Node.class);
                    doIt(request, response, resource, node, parameters, writer);

                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                }
            }
        }

        protected abstract void doIt(@Nonnull final SlingHttpServletRequest request,
                                     @Nonnull final SlingHttpServletResponse response,
                                     @Nonnull final ResourceHandle resource, @Nullable final Node node,
                                     @Nonnull final BulkParameters parameters, @Nonnull final JsonWriter writer)
                throws RepositoryException, ServletException, IOException;

        protected void clearProperty(@Nonnull final Node node, @Nonnull final String name)
                throws RepositoryException {
            try {
                Property property = node.getProperty(name);
                if (property != null) {
                    if (property.isMultiple()) {
                        node.setProperty(name, (Value[]) null);
                    } else {
                        node.setProperty(name, (Value) null);
                    }
                }
            } catch (PathNotFoundException ignore) {
            }
        }
    }

    protected class RemoveOperation extends BulkOperation {

        protected class Result {
            public List<String> names;
        }

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nonnull final ResourceHandle resource, @Nullable final Node node,
                         @Nonnull final BulkParameters parameters, @Nonnull final JsonWriter writer)
                throws RepositoryException, ServletException, IOException {

            Session session = node != null ? node.getSession() : null;
            ModifiableValueMap values = node == null ? resource.adaptTo(ModifiableValueMap.class) : null;

            writer.beginObject();
            if (parameters.names != null) {
                writer.name("removed").beginArray();

                for (String name : parameters.names) {
                    if (node != null) {
                        clearProperty(node, name);
                    } else if (values != null) {
                        values.remove(name);
                    }
                    writer.value(name);
                }

                writer.endArray();
            }
            writer.endObject();

            if (session != null) {
                session.save();
            } else if (values != null) {
                resource.getResourceResolver().commit();
            }
        }
    }

    protected class CopyOperation extends BulkOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         @Nonnull final ResourceHandle resource, @Nullable final Node node,
                         @Nonnull final BulkParameters parameters, @Nonnull final JsonWriter writer)
                throws RepositoryException, ServletException, IOException {

            if (parameters.path != null && parameters.names != null) {
                writer.beginObject();
                writer.name("copied").beginArray();

                if (node != null) {
                    Session session = node.getSession();
                    Node template = session.getNode(parameters.path);
                    if (template != null) {
                        for (String name : parameters.names) {
                            try {
                                Property property = template.getProperty(name);
                                if (property != null) {
                                    clearProperty(node, name);
                                    if (property.isMultiple()) {
                                        node.setProperty(name, property.getValues());
                                    } else {
                                        node.setProperty(name, property.getValue());
                                    }
                                    writer.value(name);
                                }
                            } catch (PathNotFoundException ignore) {
                            }
                        }
                    }
                    session.save();

                } else {

                    ResourceResolver resolver = resource.getResourceResolver();
                    ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
                    Resource template = resolver.getResource(parameters.path);
                    if (values != null && template != null) {
                        ValueMap templateValues = template.getValueMap();
                        for (String name : parameters.names) {
                            Object value = templateValues.get(name);
                            if (value != null) {
                                values.put(name, value);
                                writer.value(name);
                            }
                        }
                    }
                    resolver.commit();
                }

                writer.endArray();
                writer.endObject();
            }
        }
    }

    //
    // Binary properties
    //

    protected class GetBinaryOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                String name = RequestUtil.getParameter(request, PARAM_NAME, ResourceUtil.PROP_DATA);

                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    javax.jcr.Property property = node.getProperty(name);
                    Binary binary = property != null ? property.getBinary() : null;
                    if (binary != null) {

                        try {
                            prepareResponse(response, resource, binary.getSize());
                            sendContent(response, binary.getStream());
                        } finally {
                            binary.dispose();
                        }

                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "no binary '" + name + "' property found");
                    }

                } else {

                    ValueMap values = resource.getValueMap();
                    InputStream content = values.get(name, InputStream.class);

                    if (content != null) {

                        prepareResponse(response, resource, null);
                        sendContent(response, content);

                    } else {
                        LOG.error(resource.getPath() + ": invalid binary GET - resource has no content node");
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "can't determine file node '" + resource.getPath() + "'");
                    }
                }

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

        protected void prepareResponse(@Nonnull final SlingHttpServletResponse response,
                                       ResourceHandle resource, @Nullable final Long size) {
            MimeType mimeType = MimeTypeUtil.getMimeType(resource);
            if (mimeType != null) {
                response.setContentType(mimeType.toString());
            }
            String filename = MimeTypeUtil.getFilename(resource, null);
            if (StringUtils.isNotBlank(filename)) {
                response.setHeader("Content-Disposition", "inline; filename=" + filename);
            }

            Calendar lastModified = resource.getProperty(ResourceUtil.PROP_LAST_MODIFIED, Calendar.class);
            if (lastModified != null) {
                response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified.getTimeInMillis());
            }

            if (size != null) {
                response.setContentLength(size.intValue());
            }
            response.setStatus(SC_OK);
        }

        protected void sendContent(@Nonnull final SlingHttpServletResponse response, @Nullable final InputStream input)
                throws IOException {
            if (input != null) {
                BufferedInputStream buffered = new BufferedInputStream(input);
                try {
                    IOUtils.copy(buffered, response.getOutputStream());
                } finally {
                    buffered.close();
                    input.close();
                }
            } else {
                throw new IOException("no content found");
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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
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
                        String name = nameParam != null ? nameParam.getString() : ResourceUtil.PROP_DATA;

                        LOG.info(resource.getPath() + ": update POST for binary property '" + name + "'");

                        Session session = node.getSession();
                        InputStream input = file.getInputStream();
                        PropertyUtil.setProperty(node, name, input);
                        session.save();

                        response.setContentLength(0);
                        response.setStatus(SC_OK);

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
        public void doIt(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (resource == null || !resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                InputStream input = request.getInputStream();

                Node node = resource.adaptTo(Node.class);
                if (node != null) {

                    Session session = node.getSession();
                    PropertyUtil.setProperty(node, ResourceUtil.PROP_DATA, input);
                    postChange(node);
                    session.save();

                } else {
                    ResourceResolver resolver = resource.getResourceResolver();

                    Resource parent = null;
                    boolean isFileUpdate = resource.getName().equals(JcrConstants.JCR_CONTENT)
                            && resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals(JcrConstants.NT_RESOURCE)
                            && (parent = resource.getParent()) != null
                            && parent.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals(JcrConstants.NT_FILE);

                    if (isFileUpdate && resolver instanceof ExtendedResolver) {
                        ValueMap values = resource.getValueMap();

                        ((ExtendedResolver) resolver).upload(parent.getPath(), input, null,
                                values.get(JcrConstants.JCR_MIMETYPE, String.class), StandardCharsets.UTF_8.name());
                        resolver.commit();

                    } else {

                        ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
                        if (values != null) {

                            values.put(ResourceUtil.PROP_DATA, input);
                            postChange(resource);
                            resolver.commit();

                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                    "can't modify '" + resource.getPath() + "'");
                            return;
                        }
                    }
                }

                response.setContentLength(0);
                response.setStatus(SC_OK);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

        protected void postChange(Node node) throws RepositoryException {
        }

        protected void postChange(Resource resource) {
        }
    }

    protected class PutUpdateOperation extends PutBinaryOperation {

        @Override
        protected void postChange(Node node) throws RepositoryException {
            Calendar lastModified = PropertyUtil.getProperty(node, JcrConstants.JCR_LASTMODIFIED, (Calendar) null);
            if (lastModified != null) {
                Session session = node.getSession();
                String userId = session.getUserID();
                PropertyUtil.setProperty(node, JcrConstants.JCR_LASTMODIFIED, GregorianCalendar.getInstance(), PropertyType.DATE);
                if (StringUtils.isNotBlank(userId)) {
                    PropertyUtil.setProperty(node, JcrConstants.JCR_LASTMODIFIED + "By", userId, PropertyType.STRING);
                }
            }
        }

        @Override
        protected void postChange(Resource resource) {
            ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
            if (values != null) {
                Calendar lastModified = values.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
                if (lastModified != null) {
                    values.put(JcrConstants.JCR_LASTMODIFIED, GregorianCalendar.getInstance());
                }
            }
        }
    }
}
