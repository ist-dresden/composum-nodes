package com.composum.sling.nodes.mount.remote;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RemoteClient {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteClient.class);

    public static final Pattern REMOTE_URL_PATTERN = Pattern.compile(
            "^(?<url>(?<scheme>https?)://(?<host>[^:/]+)(:(?<port>\\d+))?(?<context>/.+)?)/?$");

    @NotNull
    protected final RemoteProvider provider;
    @NotNull
    protected final Collection<String> builderKeys;
    private transient Collection<RemoteClientBuilder> builders;

    protected final HttpHost remoteHost;
    protected final String remoteUrl;

    private transient HttpClientContext clientContext;

    @NotNull
    private final List<Header> defaultHeaders;

    protected RemoteClient(@NotNull final RemoteProvider provider, @NotNull final RemoteProvider.Config config,
                           @NotNull final Collection<String> builderKeys) {
        this.provider = provider;
        this.builderKeys = builderKeys;

        Matcher matcher = REMOTE_URL_PATTERN.matcher(config.remote_url());
        if (matcher.matches()) {
            String scheme = matcher.group("scheme");
            String host = matcher.group("host");
            String portStr = matcher.group("port");
            int port = -1;
            if (StringUtils.isNotBlank(portStr)) {
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException nfex) {
                    LOG.error("invalid port: '{}'", portStr);
                }
            }
            remoteHost = new HttpHost(host, port, scheme);
            String url = matcher.group("url");
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            remoteUrl = url;
            LOG.info("remote: '{}', host: '{}' ({}://{}:{})", remoteUrl, remoteHost, scheme, host, port);
        } else {
            remoteHost = null;
            remoteUrl = "";
            LOG.error("invalid remote URL '{}'", config.remote_url());
        }

        defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader(HttpHeaders.AUTHORIZATION,
                getAuthHeader(config.login_username(), config.login_password())));

        for (String header : config.request_headers()) {
            if (StringUtils.isNotBlank(header)) {
                String[] parts = StringUtils.split(header, "=", 2);
                defaultHeaders.add(new BasicHeader(parts[0], parts.length > 1 ? parts[1] : ""));
            }
        }
    }

    /**
     * @return 'true' if the remote URL has been accepted by this remote client
     */
    public boolean isValid() {
        return remoteHost != null;
    }

    /**
     * @return the URL to send a POST request to change the resource at the given path
     */
    @NotNull
    public String getHttpUrl(@NotNull final String resourcePath) {
        String path = provider.remotePath(resourcePath);
        return remoteUrl + path;
    }

    /**
     * @return the URL to access the remote resource via HTTP
     */
    @NotNull
    public String getHttpUrl(@NotNull final RemoteResource resource) {
        return getHttpUrl(resource.getPath());
    }

    //
    // general method object builder methods...
    //

    protected HttpHead buildHttpHead(@NotNull final String url) {
        return new HttpHead(url);
    }

    protected HttpGet buildHttpGet(@NotNull final String url) {
        return new HttpGet(url);
    }

    protected HttpPropfind buildPropfind(@NotNull final String url)
            throws IOException {
        return new HttpPropfind(url, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
    }

    protected HttpPost buildHttpPost(@NotNull final String url) {
        return new HttpPost(url);
    }

    //
    // request execution
    //

    /**
     * request execution in the remote clients HTTP context
     */
    public HttpResponse execute(@NotNull final HttpUriRequest request) throws IOException {
        return execute(buildClient(), request);
    }

    /**
     * request execution in the remote clients HTTP context
     */
    public HttpResponse execute(@NotNull final HttpClient client, @NotNull final HttpUriRequest request)
            throws IOException {
        return client.execute(request, getClientContext());
    }

    /**
     * @return the context for the request execution
     */
    protected HttpClientContext getClientContext() {
        if (clientContext == null) {
            clientContext = new HttpClientContext();
            for (RemoteClientBuilder clientBuilder : getBuilders()) {
                clientBuilder.configure(clientContext);
            }
        }
        return clientContext;
    }

    /**
     * @return the client to load remote resources
     */
    @NotNull
    protected HttpClient buildClient() {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setDefaultHeaders(defaultHeaders);
        for (RemoteClientBuilder clientBuilder : getBuilders()) {
            clientBuilder.configure(builder);
        }
        return builder.build();
    }

    /**
     * @return the client builder set (lazy loaded to ensure that they are registered already)
     */
    protected Collection<RemoteClientBuilder> getBuilders() {
        if (builders == null) {
            builders = provider.clientSetup.getBuilders(builderKeys);
        }
        return builders;
    }

    /**
     * @return the explicit header value for preemptive authentication
     */
    protected String getAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }
}
