package com.composum.sling.core.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Deactivate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

/**
 * A basic class for all '/bin/{service}/path/to/resource' servlets.
 */
public abstract class AbstractServiceServlet extends SlingAllMethodsServlet implements RestrictedService {

    public static final String PARAM_FILE = "file";
    public static final String PARAM_CMD = "cmd";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_ID = "id";
    public static final String PARAM_INDEX = "index";
    public static final String PARAM_JCR_CONTENT = "jcrContent";
    public static final String PARAM_LABEL = "label";
    public static final String PARAM_MIME_TYPE = "mimeType";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_PATH = "path";
    public static final String PARAM_BEFORE = "before";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_RESOURCE_TYPE = "resourceType";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_URL = "url";
    public static final String PARAM_VALUE = "value";
    public static final String PARAM_VERSION = "version";

    public static final String DATE_FORMAT = "yyyy-MM-DD HH:mm:ss";

    private ServiceRestrictions restrictionsService;

    @Deactivate
    protected void deactivate() {
        restrictionsService = null;
    }

    protected ServiceRestrictions getRestrictions() {
        if (restrictionsService == null) {
            final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            final ServiceReference<?> reference =
                    bundleContext.getServiceReference(ServiceRestrictions.class.getName());
            restrictionsService = (ServiceRestrictions) bundleContext.getService(reference);
        }
        return restrictionsService;
    }

    @Override
    public ServiceRestrictions.Key getServiceKey() {
        final Restricted restricted = getClass().getAnnotation(Restricted.class);
        return restricted != null ? new ServiceRestrictions.Key(restricted.key()) : null;
    }

    protected boolean isEnabled(@NotNull final SlingHttpServletRequest request,
                                @NotNull final SlingHttpServletResponse response,
                                @NotNull final ServiceRestrictions.Permission needed) {
        return getRestrictions().isPermissible(request, getServiceKey(), needed);
    }

    @NotNull
    protected ServiceRestrictions.Permission methodGetPermission(@NotNull final SlingHttpServletRequest request) {
        return ServiceRestrictions.Permission.read;
    }

    @NotNull
    protected ServiceRestrictions.Permission methodPostPermission(@NotNull final SlingHttpServletRequest request) {
        return ServiceRestrictions.Permission.write;
    }

    @NotNull
    protected ServiceRestrictions.Permission methodPutPermission(@NotNull final SlingHttpServletRequest request) {
        return ServiceRestrictions.Permission.write;
    }

    @NotNull
    protected ServiceRestrictions.Permission methodDeletePermission(@NotNull final SlingHttpServletRequest request) {
        return ServiceRestrictions.Permission.write;
    }

    //
    // Servlet method implementation using ServletOperationSet
    //

    /**
     * Each servlet implementation must provide access to its operation set for request delegation.
     */
    @NotNull
    protected abstract ServletOperationSet<?, ?> getOperations();

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (isEnabled(request, response, methodGetPermission(request))) {
            setNoCacheHeaders(response);
            getOperations().doGet(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (isEnabled(request, response, methodPostPermission(request))) {
            setNoCacheHeaders(response);
            getOperations().doPost(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doPut(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (isEnabled(request, response, methodPutPermission(request))) {
            setNoCacheHeaders(response);
            getOperations().doPut(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doDelete(@NotNull final SlingHttpServletRequest request,
                            @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (isEnabled(request, response, methodDeletePermission(request))) {
            setNoCacheHeaders(response);
            getOperations().doDelete(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    //
    // HTTP control methods
    //

    public static void setNoCacheHeaders(@NotNull final SlingHttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    //
    // service resource retrieval
    //

    /**
     * Retrieves the resource using the suffix from the request.
     *
     * @param request the sling request with the resource path in the suffix
     * @return the resource (NOT <code>null</code>; returns a handle with an invalid resource if not resolvable)
     */
    @NotNull
    public static ResourceHandle getResource(SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        String path = getPath(request);
        Resource resource = resolver.resolve(path);
        if (ResourceUtil.isNonExistingResource(resource)) {
            String decoded = LinkUtil.decodePath(path);
            resource = resolver.resolve(decoded);
        }
        return ResourceHandle.use(resource);
    }

    public static String getPath(SlingHttpServletRequest request) {
        RequestPathInfo reqPathInfo = request.getRequestPathInfo();
        String path = reqPathInfo.getSuffix();
        if (StringUtils.isBlank(path)) {
            path = request.getParameter(PARAM_PATH);
        }
        path = XSS.filter(path);
        path = path.replaceAll("&amp;", "&"); // rollback encoding of '&' done by the filter()
        return path;
    }

    /**
     * @return the given resource if valid, otherwise the resource referenced by the raw suffix (no XSS filter)
     * if such a resource is available - to support select and rename of nodes with invalid names (node repair)
     */
    @NotNull
    public static ResourceHandle tryToUseRawSuffix(@NotNull final SlingHttpServletRequest request,
                                                   @NotNull ResourceHandle resource) {
        if (!resource.isValid()) {
            // try to use the resource path as requested (without XSS filter) - in console context only!
            String resourcePath = request.getRequestPathInfo().getSuffix();
            Resource requested = null;
            if (StringUtils.isNotBlank(resourcePath)) {
                requested = request.getResourceResolver().getResource(resourcePath);
            }
            if (requested != null) {
                resource = ResourceHandle.use(requested);
            }
        }
        return resource;
    }

    //
    // default responses...
    //

    protected void jsonAnswerItemExists(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException { // TODO use Status. Unfortunately somewhat risky.
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
        jsonWriter.beginObject();
        jsonWriter.name("success").value(false);
        jsonWriter.name("messages").beginArray();
        jsonWriter.beginObject();
        jsonWriter.name("level").value("warn");
        jsonWriter.name("text").value(I18N.get(request,
                "An element with the same name exists already - use a different name!"));
        jsonWriter.endObject();
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    //
    // JSON answer helpers
    //

    public static void jsonValue(JsonWriter writer, Object value) throws IOException {
        if (value instanceof String) {
            writer.value((String) value);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            writer.beginObject();
            for (Object key : map.keySet()) {
                writer.name(key.toString());
                jsonValue(writer, map.get(key));
            }
            writer.endObject();
        } else if (value instanceof Object[]) {
            writer.beginArray();
            for (Object v : (Object[]) value) {
                jsonValue(writer, v);
            }
            writer.endArray();
        } else if (value instanceof Iterable) {
            writer.beginArray();
            for (Object v : (Iterable<?>) value) {
                jsonValue(writer, v);
            }
            writer.endArray();
        } else if (value instanceof Iterator) {
            Iterator<?> iterator = (Iterator<?>) value;
            writer.beginArray();
            while (iterator.hasNext()) {
                jsonValue(writer, iterator.next());
            }
            writer.endArray();
        } else if (value instanceof Calendar) {
            writer.value(new SimpleDateFormat(DATE_FORMAT).format(((Calendar) value).getTime()));
        } else {
            writer.value(value != null ? value.toString() : null);
        }
    }

    //
    // JSON parameters parsing
    //

    public static <T> T getJsonObject(SlingHttpServletRequest request, Class<T> type)
            throws IOException {
        InputStream inputStream = request.getInputStream();
        Reader inputReader = new InputStreamReader(inputStream, MappingRules.CHARSET.name());
        // parse JSON input into a POJO of the requested type
        Gson gson = JsonUtil.GSON_BUILDER.create();
        return gson.fromJson(inputReader, type);
    }

    public static <T> T getJsonObject(SlingHttpServletRequest request, Class<T> type,
                                      InstanceCreator<T> instanceCreator)
            throws IOException {
        InputStream inputStream = request.getInputStream();
        Reader inputReader = new InputStreamReader(inputStream, MappingRules.CHARSET.name());
        // parse JSON input into a POJO of the requested type
        Gson gson = new GsonBuilder().registerTypeAdapter(type, instanceCreator).create();
        return gson.fromJson(inputReader, type);
    }

    public static <T> T getJsonObject(String input, Class<T> type) {
        Reader inputReader = new StringReader(input);
        // parse JSON input into a POJO of the requested type
        Gson gson = JsonUtil.GSON_BUILDER.create();
        return gson.fromJson(inputReader, type);
    }
}
