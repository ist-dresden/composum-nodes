package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.core.util.LinkUtil.adjustMappedUrl;

/**
 * a Sling URL parser / builder class
 */
public class SlingUrl {

    public static final LinkCodec CODEC = new LinkCodec();

    public static final Pattern URL_PATTERN = Pattern.compile(
            "(([a-zA-Z]+):)?((//([^/:]+)(:([0-9]+))?)?(/([^/.?]*/)*)?([^/.?]*)(\\.[^/?#]*)?(/[^?#]*)?(\\?[^?#]*)?(#.*)?)$"
    );

    public static final Pattern HTTP_SCHEME = Pattern.compile("^https?$", Pattern.CASE_INSENSITIVE);
    public static final Pattern SPECIAL_SCHEME = Pattern.compile("^(mailto|tel)$", Pattern.CASE_INSENSITIVE);

    protected String scheme;
    protected String host;
    protected Integer port;
    protected String contextPath;
    protected String path;
    protected String name;
    protected final List<String> selectors = new ArrayList<>();
    protected String extension;
    protected String suffix;
    protected final LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();
    protected String fragment;

    private transient String url;
    protected boolean external;
    protected boolean special;  // true, if phone or mail or ... link

    protected String resourcePath;
    protected Resource resource;

    protected final SlingHttpServletRequest request;
    protected final LinkMapper linkMapper;

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource) {
        this(request, resource, null, null);
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource, @Nullable final String extension) {
        this(request, resource, null, extension,
                null, null, true, getLinkMapper(request, null));
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource,
                    @Nullable final String selectors, @Nullable final String extension) {
        this(request, resource, selectors, extension,
                null, null, true, getLinkMapper(request, null));
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource,
                    @Nullable final String selectors, @Nullable final String extension,
                    @Nullable final String suffix) {
        this(request, resource, selectors, extension, suffix,
                null, true, getLinkMapper(request, null));
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource,
                    @Nullable final String selectors, @Nullable final String extension,
                    @Nullable final String suffix,
                    @Nullable final String parameterString) {
        this(request, resource, selectors, extension, suffix, parameterString,
                true, getLinkMapper(request, null));
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request,
                    @Nonnull final Resource resource,
                    @Nullable final String selectors, @Nullable final String extension,
                    @Nullable final String suffix,
                    @Nullable final String parameterString, boolean decodeParameters,
                    @Nullable LinkMapper linkMapper) {
        this.request = request;
        this.linkMapper = linkMapper;
        this.resource = resource;
        this.resourcePath = resource.getPath();
        this.name = StringUtils.substringAfterLast(resourcePath, "/");
        this.path = resourcePath.substring(0, resourcePath.length() - name.length());
        setSelectors(selectors);
        setExtension(extension);
        setSuffix(suffix);
        setParameters(parameterString, decodeParameters);
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request, @Nonnull final String url) {
        this(request, url, false);
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request, @Nonnull final String url,
                    boolean decode) {
        this(request, url, decode, getLinkMapper(request, null));
    }

    public SlingUrl(@Nonnull final SlingHttpServletRequest request, @Nonnull final String url,
                    boolean decode, @Nullable LinkMapper linkMapper) {
        this.request = request;
        this.linkMapper = linkMapper;
        parseUrl(url, decode);
    }

    @Nonnull
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public boolean isExternal() {
        return external;
    }

    public SlingUrl external(boolean value) {
        return setExternal(value);
    }

    public SlingUrl setExternal(boolean value) {
        external = value;
        return this;
    }

    public boolean isSpecial() {
        return special;
    }

    public SlingUrl special(boolean value) {
        return setSpecial(value);
    }

    public SlingUrl setSpecial(boolean value) {
        external = value;
        return this;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    @Nullable
    public Resource getResource() {
        return resource;
    }

    public String getContextPath() {
        return StringUtils.isNotBlank(contextPath) ? contextPath : request.getContextPath();
    }

    public String getPath() {
        return path + name;
    }

    @Nonnull
    List<String> getSelectors() {
        return selectors;
    }

    public SlingUrl selector(String... value) {
        return addSelector(value);
    }

    public SlingUrl addSelector(String... value) {
        clearUrl();
        if (value != null) {
            selectors.addAll(Arrays.asList(value));
        }
        return this;
    }

    public SlingUrl setSelector(String... value) {
        clearSelectors();
        addSelector(value);
        return this;
    }

    public SlingUrl selectors(@Nullable final String selectors) {
        return setSelectors(selectors);
    }

    public SlingUrl setSelectors(@Nullable final String selectors) {
        clearSelectors();
        if (selectors != null) {
            for (String sel : StringUtils.split(selectors, '.')) {
                if (StringUtils.isNotBlank(sel)) {
                    this.selectors.add(sel);
                }
            }
        }
        return this;
    }

    public SlingUrl removeSelector(String... value) {
        clearUrl();
        if (value != null) {
            for (String val : value) {
                selectors.remove(val);
            }
        }
        return this;
    }

    public SlingUrl clearSelectors() {
        clearUrl();
        selectors.clear();
        return this;
    }

    @Nullable
    public String getExtension() {
        return extension;
    }

    public SlingUrl extension(@Nullable final String extension) {
        return setExtension(extension);
    }

    public SlingUrl setExtension(@Nullable String extension) {
        clearUrl();
        if (extension != null) {
            int dot = extension.lastIndexOf(".");
            if (dot >= 0) {
                extension = extension.substring(dot + 1);
            }
        }
        this.extension = extension;
        return this;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    public SlingUrl suffix(@Nullable final Resource resource) {
        return setSuffix(resource != null ? resource.getPath() : null);
    }

    public SlingUrl suffix(@Nullable final Resource resource, @Nullable final String extension) {
        return setSuffix(resource != null ? resource.getPath()
                + (StringUtils.isNotBlank(extension) ? ("." + extension) : "") : null);
    }

    public SlingUrl suffix(@Nullable final String suffix) {
        return setSuffix(suffix);
    }

    public SlingUrl setSuffix(@Nullable final String suffix) {
        clearUrl();
        this.suffix = suffix;
        return this;
    }

    @Nonnull
    Map<String, List<String>> getParameters() {
        return parameters;
    }

    public SlingUrl parameter(String name, String... value) {
        return addParameter(name, value);
    }

    public SlingUrl addParameter(String name, String... value) {
        clearUrl();
        List<String> values = parameters.computeIfAbsent(name, k -> new ArrayList<>());
        if (value != null) {
            for (String val : value) {
                if (val != null) {
                    values.add(val);
                }
            }
        }
        return this;
    }

    public SlingUrl addParameters(String parameterString, boolean decode) {
        if (parameterString != null) {
            parseParameters(parameterString, decode);
        }
        return this;
    }

    public SlingUrl setParameter(String name, String... value) {
        removeParameter(name);
        addParameter(name, value);
        return this;
    }

    public SlingUrl parameters(String parameterString) {
        return setParameters(parameterString, true);
    }

    public SlingUrl setParameters(String parameterString, boolean decode) {
        clearParameters();
        addParameters(parameterString, decode);
        return this;
    }

    public SlingUrl removeParameter(String name) {
        clearUrl();
        parameters.remove(name);
        return this;
    }

    public SlingUrl clearParameters() {
        clearUrl();
        parameters.clear();
        return this;
    }

    @Nullable
    public String getFragment() {
        return fragment;
    }

    public SlingUrl fragment(@Nullable final String fragment) {
        return setFragment(fragment);
    }

    public SlingUrl setFragment(@Nullable final String fragment) {
        clearUrl();
        this.fragment = fragment;
        return this;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object other) {
        return toString().equals(other.toString());
    }

    @Override
    public String toString() {
        return getUrl();
    }

    public String getUrl() {
        if (url == null) {
            url = buildUrl();
        }
        return url;
    }

    public void clearUrl() {
        this.url = null;
    }

    public void reset() {
        scheme = null;
        host = null;
        port = null;
        contextPath = null;
        path = null;
        name = null;
        selectors.clear();
        extension = null;
        suffix = null;
        parameters.clear();
        fragment = null;
        clearUrl();
        external = special = false;
        resourcePath = null;
        resource = null;
    }

    protected String buildUrl() {
        StringBuilder prepend = new StringBuilder();
        String uri = special ? name : external ? (path + name) : LinkUtil.encodePath(path + name);
        if (!external && linkMapper != null) {
            uri = linkMapper.mapUri(request, uri);
            uri = adjustMappedUrl(request, uri);
        } else {
            if (StringUtils.isNotBlank(scheme)) {
                prepend.append(scheme).append(':');
            }
            if (StringUtils.isNotBlank(host)) {
                prepend.append("//").append(host);
                if (port != null) {
                    prepend.append(":").append(port);
                }
            }
        }
        StringBuilder builder = new StringBuilder(prepend.toString());
        builder.append(uri);
        for (String value : selectors) {
            builder.append('.').append(external ? value : CODEC.encode(value));
        }
        if (StringUtils.isNotBlank(extension)) {
            builder.append('.').append(extension);
        }
        if (StringUtils.isNotBlank(suffix)) {
            builder.append(external ? suffix : LinkUtil.encodePath(suffix));
        }
        if (!special) {
            if (parameters.size() > 0) {
                int index = 0;
                for (Map.Entry<String, List<String>> param : parameters.entrySet()) {
                    String name = param.getKey();
                    List<String> values = param.getValue();
                    if (values.size() > 0) {
                        for (String val : values) {
                            builder.append(index == 0 ? '?' : '&');
                            builder.append(external ? name : CODEC.encode(name));
                            if (val != null) {
                                builder.append("=").append(external ? val : CODEC.encode(val));
                            }
                            index++;
                        }
                    } else {
                        builder.append(index == 0 ? '?' : '&');
                        builder.append(external ? name : CODEC.encode(name));
                        index++;
                    }
                }
            }
            if (StringUtils.isNotBlank(fragment)) {
                builder.append('#').append(external ? fragment : CODEC.encode(fragment));
            }
        }
        return builder.toString();
    }

    protected void parseUrl(@Nonnull final String url, final boolean decode) throws IllegalArgumentException {
        reset();
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("can't parse URL '" + url + "'");
        }
        String value;
        if (StringUtils.isNotBlank(value = matcher.group(2))) {
            scheme = value;
            external = true;
            special = SPECIAL_SCHEME.matcher(scheme).matches();
        } else {
            external = false;
            special = false;
        }
        if (special) {
            name = matcher.group(3);
        } else {
            if (StringUtils.isNotBlank(value = matcher.group(5))) {
                host = value;
            }
            if (StringUtils.isNotBlank(value = matcher.group(7))) {
                port = Integer.parseInt(value);
            }
            if (StringUtils.isNotBlank(value = matcher.group(8))) {
                path = decode ? CODEC.decode(value) : value;
                String contextPath = request.getContextPath();
                if (StringUtils.isNotBlank(contextPath) && path.startsWith(contextPath + "/")) {
                    this.contextPath = contextPath;
                    path = path.substring(contextPath.length());
                }
            }
            name = StringUtils.isNotBlank(value = matcher.group(10)) ? (decode ? CODEC.decode(value) : value) : "";
            if (StringUtils.isNotBlank(value = matcher.group(11))) {
                String[] selExt = StringUtils.split(value.substring(1), '.');
                for (int i = 0; i < selExt.length - 1; i++) {
                    selectors.add(decode ? CODEC.decode(selExt[i]) : selExt[i]);
                }
                extension = CODEC.decode(decode ? selExt[selExt.length - 1] : selExt[selExt.length - 1]);
            }
            if (StringUtils.isNotBlank(value = matcher.group(12))) {
                suffix = decode ? CODEC.decode(value) : value;
            }
            if (StringUtils.isNotBlank(value = matcher.group(13))) {
                parseParameters(value, decode);
            }
            if (StringUtils.isNotBlank(value = matcher.group(14))) {
                fragment = (decode ? CODEC.decode(value) : value).substring(1);
            }
        }
        if (!external) {
            ResourceResolver resolver = request.getResourceResolver();
            resourcePath = path + name;
            if (StringUtils.isNotBlank(extension)) {
                resourcePath += '.' + extension;
            }
            resource = resolver.getResource(resourcePath);
            if (resource == null) {
                resourcePath = path + name;
                resource = resolver.getResource(resourcePath);
            }
        }
    }

    protected void parseParameters(@Nonnull String parameterString, boolean decode) {
        parameterString = parameterString.trim();
        while (parameterString.startsWith("?")) {
            parameterString = parameterString.substring(1);
        }
        for (String param : StringUtils.split(parameterString, '&')) {
            String[] nameVal = StringUtils.split(param, "=", 2);
            addParameter(decode ? CODEC.decode(nameVal[0]) : nameVal[0],
                    nameVal.length > 1 ? (decode ? CODEC.decode(nameVal[1]) : nameVal[1]) : null);
        }
    }

    protected static LinkMapper getLinkMapper(@Nonnull final SlingHttpServletRequest request,
                                              @Nullable LinkMapper linkMapper) {
        if (linkMapper == null) {
            linkMapper = (LinkMapper) request.getAttribute(LinkMapper.LINK_MAPPER_REQUEST_ATTRIBUTE);
            if (linkMapper == null) {
                linkMapper = LinkMapper.RESOLVER;
            }
        }
        return linkMapper;
    }
}
