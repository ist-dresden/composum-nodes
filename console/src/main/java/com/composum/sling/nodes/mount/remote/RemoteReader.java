package com.composum.sling.nodes.mount.remote;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
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
public class RemoteReader extends RemoteClient {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteReader.class);

    public static final String NS_PREFIX_JCR = "jcr";

    public RemoteReader(@Nonnull final RemoteProvider provider, @Nonnull final String httpUrl,
                        @Nonnull final String username, @Nonnull final String password) {
        super(provider, httpUrl, username, password);
    }

    /**
     * @return the resoures repository path on the remote system
     */
    @Nonnull
    public String remotePath(@Nonnull final RemoteResource resource) {
        return provider.remotePath(resource.getPath());
    }

    @Nullable
    public RemoteResource loadResource(@Nonnull final RemoteResolver resolver,
                                       @Nonnull final RemoteResource resource) {
        RemoteResource result = resource;
        resource.children = null;
        resource.values = new ValueMapDecorator(new TreeMap<>());
        String logHint = null;
        HttpClient httpClient = buildClient();
        if (checkRemoteResource(httpClient, resource)) {
            int statusCode = loadJsonResource(resolver, resource, httpClient);
            if (statusCode == SC_OK) {
                logHint = ".JSON";
            } else {
                statusCode = loadDavResource(resolver, resource, httpClient);
                if (statusCode == SC_OK || statusCode == SC_MULTI_STATUS) {
                    logHint = "--DAV";
                } else if (statusCode == SC_NOT_FOUND) {
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

    protected boolean checkRemoteResource(HttpClient httpClient, RemoteResource resource) {
        try {
            HttpHead httpHead = buildHttpHead(getHttpUrl(resource.getPath()));
            return httpClient.execute(httpHead).getStatusLine().getStatusCode() != SC_NOT_FOUND;
        } catch (IOException ignore) {
            return false;
        }
    }

    //
    // WebDAV based fallback...
    //

    @Nullable
    public String getDavUrl(@Nonnull final RemoteResource resource) {
        return getDavUrl(resource.getPath());
    }

    @Nullable
    public String getDavUrl(@Nonnull final String resourcePath) {
        return getHttpUrl(resourcePath);
    }

    protected int loadDavResource(@Nonnull final RemoteResolver resolver,
                                  @Nonnull final RemoteResource resource,
                                  @Nonnull final HttpClient httpClient) {
        int statusCode = SC_NO_CONTENT;
        String url = getDavUrl(resource);
        LOG.debug("loadDAV({}) - '{}'", resource.getPath(), url);
        try {
            HttpPropfind davGet = new HttpPropfind(url, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                HttpResponse response = httpClient.execute(davGet);
                statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == SC_MULTI_STATUS) {
                    String path = resource.getPath();
                    resource.children = new LinkedHashMap<>();
                    resource.values.clear();
                    MultiStatus multiStatus = davGet.getResponseBodyAsMultiStatus(response);
                    MultiStatusResponse[] responses = multiStatus.getResponses();
                    for (MultiStatusResponse item : responses) {
                        String itemHref = item.getHref();
                        if (itemHref.equals(url) || itemHref.equals(url + "/")) {
                            loadDavResource(resource, item);
                        } else {
                            String name = StringUtils.substringAfterLast(itemHref, "/");
                            RemoteResource child = new RemoteResource(resolver, path + "/" + name);
                            loadDavResource(child, item);
                            resource.children.put(name, child);
                        }
                    }
                    adjustDavType(resource);
                }
            } catch (DavException ex) {
                LOG.error("DAV exception loading '{}': {}", url, ex.toString());
                statusCode = SC_NOT_ACCEPTABLE;
            } finally {
                davGet.releaseConnection();
            }
        } catch (IOException ex) {
            LOG.error("IO exception loading '{}': {}", url, ex.toString());
        }
        return statusCode;
    }

    protected void adjustDavType(@Nonnull final RemoteResource resource) {
        String primaryType = resource.values.get(JcrConstants.JCR_PRIMARYTYPE, String.class);
        if (StringUtils.isBlank(primaryType)) {
            resource.values.put(JcrConstants.JCR_PRIMARYTYPE,
                    resource.children != null && resource.children.size() > 0 ? "dav:folder" : "dav:unknown");
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
        String httpUrl = getHttpUrl(path);
        return httpUrl.replaceAll("\\.", "%2E") + (path.endsWith("/") ? "" : "/") + ".1.json";
    }

    protected int loadJsonResource(@Nonnull final RemoteResolver resolver,
                                   @Nonnull final RemoteResource resource,
                                   @Nonnull final HttpClient httpClient) {
        int statusCode;
        String url = getJsonUrl(resource);
        LOG.debug("loadJSON({}) - '{}'", resource.getPath(), url);
        HttpGet httpGet = buildHttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == SC_OK) {
                try (InputStream stream = response.getEntity().getContent();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                     JsonReader jsonReader = new JsonReader(reader)) {
                    loadJsonResource(resolver, resource, jsonReader);
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
        } finally {
            httpGet.releaseConnection();
        }
        return statusCode;
    }

    protected void loadJsonResource(@Nonnull final RemoteResolver resolver,
                                    @Nonnull final RemoteResource resource,
                                    @Nonnull final JsonReader jsonReader)
            throws IOException {
        resource.children = null; // children == null -> not loaded completely
        resource.values.clear();
        List<Object> array = null;
        String name = null;
        boolean more = true;
        while (more) {
            switch (jsonReader.peek()) {
                case NAME:
                    name = jsonReader.nextName();
                    break;
                case STRING:
                    String string = jsonReader.nextString();
                    if (array != null) {
                        array.add(string);
                    } else {
                        if (name == null) {
                            throw new IOException("invaid JSON - string without name");
                        }
                        resource.values.put(name, transform(string));
                        name = null;
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
                    break;
                case BOOLEAN:
                    Boolean boolVal = jsonReader.nextBoolean();
                    if (array != null) {
                        array.add(boolVal);
                    } else {
                        if (name == null) {
                            throw new IOException("invaid JSON - boolean without name");
                        }
                        resource.values.put(name, boolVal);
                        name = null;
                    }
                    break;
                case NULL:
                    jsonReader.nextNull();
                    if (array != null) {
                        array.add(null);
                    } else {
                        if (name != null) {
                            resource.values.put(name, null);
                            name = null;
                        }
                    }
                    break;
                case BEGIN_ARRAY:
                    jsonReader.beginArray();
                    if (name == null) {
                        throw new IOException("invaid JSON - array without name");
                    }
                    array = new ArrayList<>();
                    break;
                case END_ARRAY:
                    jsonReader.endArray();
                    if (array == null) {
                        throw new IOException("invaid JSON - end of array without begin");
                    }
                    resource.values.put(name, array.toArray());
                    array = null;
                    name = null;
                    break;
                case BEGIN_OBJECT:
                    jsonReader.beginObject();
                    if (name != null) {
                        String path = resource.getPath() + "/" + name;
                        // load child and store it... (the children af a child should not be loaded)
                        RemoteResource child = new RemoteResource(resolver, path);
                        loadJsonResource(resolver, child, jsonReader);
                        if (resource.children == null) {
                            resource.children = new LinkedHashMap<>();
                        }
                        resource.children.put(name, child);
                        name = null;
                    }
                    break;
                case END_OBJECT:
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
            "yyyy-MM-dd'T'HH:mm:ss.SSSz",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss"
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
            String url = getHttpUrl(propertyPath);
            HttpClient httpClient = buildClient();
            HttpGet httpGet = buildHttpGet(url);
            try {
                HttpResponse response = httpClient.execute(httpGet);
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
