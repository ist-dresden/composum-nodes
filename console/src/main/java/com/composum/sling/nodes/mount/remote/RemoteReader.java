package com.composum.sling.nodes.mount.remote;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.jackrabbit.webdav.DavServletResponse.SC_MULTI_STATUS;

/**
 * reads the resource data using default Sling GET servlet JSON requests
 */
public class RemoteReader {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteReader.class);

    public static final String NS_PREFIX_JCR = "jcr";

    public static final Pattern DAV_HREF = Pattern.compile(
            "^((https?:)?//[^/]*)?(?<path>(?<parent>(/.*)?)?/(?<name>[^/]+))(?<folder>/)?$");

    protected final RemoteProvider provider;

    public static final String DAV_NS = "dav:";
    public static final String DAV_TYPE_FOLDER = DAV_NS + "folder";
    public static final String DAV_TYPE_UNKNOWN = DAV_NS + "unknown";

    public RemoteReader(@Nonnull final RemoteProvider provider) {
        this.provider = provider;
    }

    /**
     * @return the resoures repository path on the remote system
     */
    @Nonnull
    public String remotePath(@Nonnull final RemoteResource resource) {
        return provider.remotePath(resource.getPath());
    }

    /**
     * Loads the properties and children into the given resource
     *
     * @param resource     the resource to load / update
     * @param isKnownChild 'true' if the resource is cached already (as a child of another resource) and cannot be ignored
     * @return the loaded resource; 'null' if the resource couldn't be loaded
     */
    @Nullable
    public RemoteResource loadResource(@Nonnull final RemoteResource resource,
                                       boolean isKnownChild) {
        RemoteResource result = resource;
        resource.children = null;
        resource.values = new ValueMapDecorator(new TreeMap<>());
        String path = resource.getPath();
        String logHint = null;
        HttpClient httpClient = provider.remoteClient.buildClient();
        if (!provider.ignoreIt(path)) {
            int statusCode = loadJsonResource(resource, httpClient);
            if (statusCode == SC_OK) {
                logHint = ".JSON";
            } else {
                statusCode = loadDavResource(resource, httpClient);
                if (statusCode == SC_OK || statusCode == SC_MULTI_STATUS) {
                    logHint = "--DAV";
                } else if (statusCode == SC_NOT_FOUND && !isKnownChild) {
                    result = null;
                } else {
                    resource.children = new LinkedHashMap<>();
                    resource.values.put(JcrConstants.JCR_PRIMARYTYPE, "not:accessible");
                    logHint = "---??";
                }
            }
        } else {
            result = null;
        }
        if (result != null) {
            result.values = new ValueMapDecorator(Collections.unmodifiableMap(result.values));
        }
        if (LOG.isDebugEnabled() && logHint != null) {
            LOG.debug("load{} ({}): {}", logHint, resource.getPath(), result.children.size());
        }
        return result;
    }

    //
    // WebDAV based fallback...
    //

    @Nonnull
    public String getDavUrl(@Nonnull final RemoteResource resource) {
        return getDavUrl(resource.getPath());
    }

    @Nonnull
    public String getDavUrl(@Nonnull final String resourcePath) {
        return provider.remoteClient.getHttpUrl(resourcePath);
    }

    /**
     * In the case that a servlet at the remote system blocks the JSON access a WebDAV request is used
     * as fallback to retrieve the resources properties and children.
     *
     * @param resource   the resource to load / update
     * @param httpClient the client instance to execute the request
     * @return the status code of the request response
     */
    protected int loadDavResource(@Nonnull final RemoteResource resource,
                                  @Nonnull final HttpClient httpClient) {
        int statusCode = SC_NO_CONTENT;
        String url = getDavUrl(resource);
        LOG.debug("DAV.load({}) - '{}'", resource.getPath(), url);
        try {
            HttpPropfind davGet = provider.remoteClient.buildPropfind(url);
            try {
                HttpResponse response = provider.remoteClient.execute(httpClient, davGet);
                statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == SC_MULTI_STATUS) {
                    String path = resource.getPath();
                    resource.children = new LinkedHashMap<>();
                    resource.values.clear();
                    boolean resourceIsFolder = false;
                    MultiStatus multiStatus = davGet.getResponseBodyAsMultiStatus(response);
                    MultiStatusResponse[] responses = multiStatus.getResponses();
                    for (MultiStatusResponse item : responses) {
                        Matcher itemHref = DAV_HREF.matcher(item.getHref());
                        if (itemHref.matches()) {
                            String itemPath = provider.localPath(itemHref.group("path"));
                            boolean isFolder = "/".equals(itemHref.group("folder"));
                            if (itemPath.equals(path)) {
                                loadDavResource(resource, item);
                                resourceIsFolder = isFolder;
                            } else {
                                String name = itemHref.group("name");
                                RemoteResource child = new RemoteResource(resource.resolver, path + "/" + name);
                                loadDavResource(child, item);
                                if (!provider.ignoreIt(path + "/" + name)) {
                                    adjustDavType(child, isFolder);
                                    resource.children.put(name, child);
                                }
                            }
                        }
                    }
                    adjustDavType(resource, resourceIsFolder);
                }
            } catch (DavException ex) {
                LOG.error("DAV exception loading '{}': {}", url, ex.toString());
                statusCode = SC_NOT_ACCEPTABLE;
            }
        } catch (IOException ex) {
            LOG.error("IO exception loading '{}': {}", url, ex.toString());
        }
        return statusCode;
    }

    protected void adjustDavType(@Nonnull final RemoteResource resource, boolean isFolder) {
        String primaryType = resource.values.get(JcrConstants.JCR_PRIMARYTYPE, String.class);
        if (StringUtils.isBlank(primaryType) || primaryType.startsWith(DAV_NS)) {
            if (isFolder) {
                primaryType = DAV_TYPE_FOLDER;
            } else if (resource.children != null && resource.children.size() > 0) {
                Resource content = resource.getChild(JcrConstants.JCR_CONTENT);
                String contentType;
                if (content instanceof RemoteResource && JcrConstants.NT_RESOURCE.equals(
                        ((RemoteResource) content).values.get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
                    primaryType = JcrConstants.NT_FILE;
                } else {
                    primaryType = DAV_TYPE_UNKNOWN;
                }
            } else {
                primaryType = DAV_TYPE_UNKNOWN;
            }
            LOG.debug("DAV.adjust({}) - '{}'", resource.getPath(), primaryType);
            resource.values.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
        }
    }

    protected void loadDavResource(@Nonnull final RemoteResource resource,
                                   @Nonnull final MultiStatusResponse response) {
        DavPropertySet properties = response.getProperties(SC_OK);
        for (DavProperty<?> prop : properties) {
            addDavProperty(resource, prop);
        }
    }

    protected void addDavProperty(@Nonnull final RemoteResource resource, @Nonnull final DavProperty<?> property) {
        DavPropertyName propName = property.getName();
        String davNs = propName.getNamespace().getPrefix();
        if (NS_PREFIX_JCR.equals(davNs)) {
            addDavProperty(resource, NS_PREFIX_JCR, property);
        }
    }

    protected void addDavProperty(@Nonnull final RemoteResource resource,
                                  @Nullable final String ns, @Nonnull final DavProperty<?> property) {
        DavPropertyName propName = property.getName();
        String key = ns != null ? ns + ":" + propName.getName() : propName.getName();
        Object value = property.getValue();
        if (value instanceof String) {
            resource.values.put(key, transform((String) value));
        } else if (value != null) {
            resource.values.put(key, value);
        }
    }

    //
    // JSON based loading...
    //

    @Nonnull
    public String getJsonUrl(@Nonnull final RemoteResource resource) {
        return getJsonUrl(resource.getPath());
    }

    @Nonnull
    public String getJsonUrl(@Nonnull final String path) {
        String httpUrl = provider.remoteClient.getHttpUrl(path);
        return httpUrl.replaceAll("\\.", "%2E") + (path.endsWith("/") ? "" : "/") + ".1.json";
    }

    /**
     * The preferred resource loading using the default Sling GET servlet to read
     * the properties and children of the resource to load.
     *
     * @param resource   the resource to load / update
     * @param httpClient the client instance to execute the request
     * @return the status code of the request response
     */
    protected int loadJsonResource(@Nonnull final RemoteResource resource,
                                   @Nonnull final HttpClient httpClient) {
        int statusCode;
        String url = getJsonUrl(resource);
        LOG.debug("JSON.load({}) - '{}'", resource.getPath(), url);
        HttpGet httpGet = provider.remoteClient.buildHttpGet(url);
        try {
            HttpResponse response = provider.remoteClient.execute(httpClient, httpGet);
            statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == SC_OK) {
                try (InputStream stream = response.getEntity().getContent();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                     JsonReader jsonReader = new JsonReader(reader)) {
                    loadJsonResource(resource, jsonReader);
                    if (resource.children == null) { // no children found but searched for - store empty set
                        resource.children = new LinkedHashMap<>();
                    }
                }
            }
        } catch (MalformedJsonException mfex) {
            statusCode = SC_NOT_ACCEPTABLE;
        } catch (IOException ex) {
            LOG.error("exception loading '{}': {}", url, ex.toString());
            statusCode = SC_NOT_ACCEPTABLE;
        }
        return statusCode;
    }

    protected void loadJsonResource(@Nonnull final RemoteResource resource,
                                    @Nonnull final JsonReader jsonReader)
            throws IOException {
        resource.children = null; // children == null -> not loaded completely
        resource.values.clear();
        List<Object> array = null;
        String name = null;
        int skip = 0;
        boolean more = true;
        while (more) {
            switch (jsonReader.peek()) {
                case NAME:
                    if (skip > 0) {
                        String skipped = jsonReader.nextName();
                        LOG.trace("json.[name]({})", skipped);
                    } else {
                        name = jsonReader.nextName();
                        LOG.trace("json.name({})", name);
                    }
                    break;
                case STRING:
                    String string = jsonReader.nextString();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.string({}{}:{})", name, array != null ? "[]" : "", string);
                    }
                    if (skip == 0) {
                        if (array != null) {
                            array.add(string);
                        } else {
                            if (name == null) {
                                throw new IOException("invaid JSON - string without name");
                            }
                            resource.values.put(name, transform(string));
                            name = null;
                        }
                    }
                    break;
                case NUMBER:
                    Object number;
                    try {
                        number = jsonReader.nextInt();
                    } catch (Exception ignore) {
                        try {
                            number = jsonReader.nextLong();
                        } catch (Exception ignore_) {
                            number = jsonReader.nextDouble();
                        }
                    }
                    if (number instanceof Integer) {
                        number = Long.valueOf((Integer) number);
                    }
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.number({}{}:{})", name, array != null ? "[]" : "", number);
                    }
                    if (skip == 0) {
                        if (array != null) {
                            array.add(number);
                        } else {
                            if (name == null) {
                                throw new IOException("invaid JSON - number without name");
                            }
                            if (name.startsWith(":jcr:")) {
                                String binaryName = name.substring(1);
                                resource.values.put(binaryName, new RemoteBinary(
                                        resource.getPath() + "/" + binaryName));
                            }
                            resource.values.put(name, number);
                            name = null;
                        }
                    }
                    break;
                case BOOLEAN:
                    Boolean boolVal = jsonReader.nextBoolean();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.number({}{}:{})", name, array != null ? "[]" : "", boolVal);
                    }
                    if (skip == 0) {
                        if (array != null) {
                            array.add(boolVal);
                        } else {
                            if (name == null) {
                                throw new IOException("invaid JSON - boolean without name");
                            }
                            resource.values.put(name, boolVal);
                            name = null;
                        }
                    }
                    break;
                case NULL:
                    jsonReader.nextNull();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.NULL({}{})", name, array != null ? "[]" : "");
                    }
                    if (skip == 0) {
                        if (array != null) {
                            array.add(null);
                        } else {
                            if (name != null) {
                                resource.values.put(name, null);
                                name = null;
                            }
                        }
                    }
                    break;
                case BEGIN_ARRAY:
                    jsonReader.beginArray();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.array({}[])...", name);
                    }
                    if (skip == 0) {
                        if (name == null) {
                            throw new IOException("invaid JSON - array without name");
                        }
                        array = new ArrayList<>();
                    }
                    break;
                case END_ARRAY:
                    jsonReader.endArray();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.array({}[]:{})<", name, array != null ? array.size() : "null");
                    }
                    if (skip == 0) {
                        if (array == null) {
                            throw new IOException("invaid JSON - end of array without begin");
                        }
                        resource.values.put(name, array.toArray());
                        array = null;
                        name = null;
                    }
                    break;
                case BEGIN_OBJECT:
                    jsonReader.beginObject();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.object({}{})...", name, array != null ? "[]" : "");
                    }
                    if (name != null) {
                        if (skip > 0 || array != null) {
                            skip++;
                        } else {
                            String path = resource.getPath() + "/" + name;
                            // load child and store it... (the children af a child should not be loaded)
                            RemoteResource child = new RemoteResource(resource.resolver, path);
                            LOG.debug("JSON.load.child({})...", path);
                            loadJsonResource(child, jsonReader);
                            if (resource.children == null) {
                                resource.children = new LinkedHashMap<>();
                            }
                            if (!provider.ignoreIt(child.getPath())) {
                                resource.children.put(name, child);
                            }
                            name = null;
                        }
                    }
                    break;
                case END_OBJECT:
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("json.object({}{})<", name, array != null ? "[]" : "");
                    }
                    if (skip > 0) {
                        skip--;
                    }
                    jsonReader.endObject();
                case END_DOCUMENT:
                    more = false;
                    break;
            }
        }
    }

    public static final Pattern STRING_TYPE_PREFIX = Pattern.compile("^\\{([^}]+)}(.*)$");
    public static final Map<String, Boolean> BOOLEAN_VALUES = new HashMap<String, Boolean>() {{
        put("true", Boolean.TRUE);
        put("false", Boolean.FALSE);
        put("on", Boolean.TRUE);
        put("off", Boolean.FALSE);
    }};

    public Object transform(String string) {
        if (StringUtils.isNotBlank(string)) {
            Matcher typePrefix = STRING_TYPE_PREFIX.matcher(string);
            if (typePrefix.matches()) {
                try {
                    int propertyType = PropertyType.valueFromName(typePrefix.group(1));
                    string = typePrefix.group(2);
                    switch (propertyType) {
                        case PropertyType.BOOLEAN:
                            Boolean bool = toBoolean(string);
                            if (bool != null) {
                                return bool;
                            }
                            return bool;
                        case PropertyType.DATE:
                            Calendar calendar = toDate(string);
                            if (calendar != null) {
                                return calendar;
                            }
                            break;
                    }
                } catch (IllegalArgumentException ignore) {
                }
            } else {
                Object object;
                if ((object = toDate(string)) != null) {
                    return object;
                }
                if ((object = toBoolean(string)) != null) {
                    return object;
                }
            }
        }
        return string;
    }

    @Nullable
    public static Boolean toBoolean(@Nonnull final String string) {
        return BOOLEAN_VALUES.get(string.toLowerCase());
    }

    public static final String[] DATE_FORMATS = new String[]{
            "EEE MMM dd yyyy HH:mm:ss 'GMT'z", // "Fri Nov 20 2020 10:43:27 GMT+0100", the default
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSz",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss",
            "dd.MM.yyyy HH:mm:ss"
    };

    @Nullable
    public static Calendar toDate(@Nonnull final String string) {
        for (String format : DATE_FORMATS) {
            try {
                Date date = new SimpleDateFormat(format, Locale.ENGLISH).parse(string);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                return calendar;
            } catch (ParseException ignore) {
            }
        }
        return null;
    }

    public class RemoteBinary extends InputStream {

        protected final String propertyPath;

        private transient HttpGet httpGet;
        private transient InputStream content;

        public RemoteBinary(String propertyPath) {
            this.propertyPath = propertyPath;
        }

        protected void connect() {
            try {
                close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
            String url = provider.remoteClient.getHttpUrl(propertyPath);
            HttpGet httpGet = provider.remoteClient.buildHttpGet(url);
            try {
                HttpResponse response = provider.remoteClient.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                switch (statusLine.getStatusCode()) {
                    case SC_OK:
                        content = response.getEntity().getContent();
                        break;
                    default:
                        content = new ByteArrayInputStream(new byte[0]);
                        release();
                        break;
                }
            } catch (IOException ex) {
                LOG.error("exception loading '{}': {}", url, ex.toString());
                release();
            }
        }

        protected void release() {
            if (httpGet != null) {
                httpGet.releaseConnection();
                httpGet = null;
            }
        }

        protected InputStream content() {
            if (content == null) {
                connect();
            }
            return content;
        }

        @Override
        public int read() throws IOException {
            return content().read();
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
            release();
        }
    }
}
