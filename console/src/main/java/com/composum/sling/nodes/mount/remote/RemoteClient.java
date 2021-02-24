package com.composum.sling.nodes.mount.remote;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public abstract class RemoteClient {

    public static class Parameters extends ArrayList<NameValuePair> {

        public void add(String name, String value) {
            add(new BasicNameValuePair(name, value));
        }
    }

    @Nonnull
    protected final RemoteProvider provider;
    @Nonnull
    protected final String httpUrl;
    @Nonnull
    private final String username;
    @Nonnull
    private final String password;

    protected RemoteClient(@Nonnull final RemoteProvider provider, @Nonnull final String httpUrl,
                           @Nonnull final String username, @Nonnull final String password) {
        this.provider = provider;
        this.httpUrl = httpUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * @return the URL to send a POST request to change the resource at the given path
     */
    @Nonnull
    public String getHttpUrl(@Nonnull final String resourcePath) {
        String path = provider.remotePath(resourcePath);
        return httpUrl + path;
    }

    /**
     * @return the URL to access the remote resource via HTTP
     */
    @Nonnull
    public String getHttpUrl(@Nonnull final RemoteResource resource) {
        return getHttpUrl(resource.getPath());
    }

    /**
     * @return the client to load remote resources
     */
    @Nonnull
    protected HttpClient buildClient() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    protected org.apache.commons.httpclient.HttpClient buildCommonsClient() {
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        httpClient.getState().setCredentials(org.apache.commons.httpclient.auth.AuthScope.ANY,
                new org.apache.commons.httpclient.UsernamePasswordCredentials(username, password));
        return httpClient;
    }

    protected String getAuthHeader() {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    protected void setupMethod(HttpRequestBase request) {
        request.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
        request.addHeader("X-SLING-REMOTE", provider.localRoot);
    }

    protected HttpHead buildHttpHead(@Nonnull final String url) {
        HttpHead method = new HttpHead(url);
        setupMethod(method);
        return method;
    }

    protected HttpGet buildHttpGet(@Nonnull final String url) {
        HttpGet method = new HttpGet(url);
        setupMethod(method);
        return method;
    }

    protected HttpPost buildHttpPost(@Nonnull final String url) {
        HttpPost method = new HttpPost(url);
        setupMethod(method);
        return method;
    }

    protected PostMethod buildPostMethod(@Nonnull final String url) {
        PostMethod postMethod = new PostMethod(url);
        postMethod.addRequestHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
        postMethod.addRequestHeader("X-SLING-REMOTE", provider.localRoot);
        return postMethod;
    }
}
