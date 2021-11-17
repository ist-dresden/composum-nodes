package com.composum.sling.nodes.mount.remote;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.net.ProxySelector;

@Component
@Designate(ocd = RemoteProxyBuilder.Config.class, factory = true)
public class RemoteProxyBuilder implements RemoteClientBuilder {

    @ObjectClassDefinition(name = "Composum Nodes Remote Resource Proxy")
    public @interface Config {

        @AttributeDefinition(
                name = "Aspect Key",
                description = "the key to add (reference) this configuration aspect to a remote provider"
        )
        String aspect_key() default "proxy";

        @AttributeDefinition(
                name = "Service Ranking",
                description = "the ranking of this configuration"
        )
        int service_ranking() default 100;

        @AttributeDefinition(
                name = "Use System Proxy",
                description = "use the predefined proxy configuration of the host system"
        )
        boolean proxy_system_default() default false;

        @AttributeDefinition(
                name = "Proxy Host",
                description = "the hostname (or the ip address) of the proxy server"
        )
        String proxy_host();

        @AttributeDefinition(
                name = "Proxy Port",
                description = "the proxy port of the proxy server"
        )
        int proxy_port() default 3128;

        @AttributeDefinition(
                name = "Proxy Username"
        )
        String proxy_username();

        @AttributeDefinition(
                name = "Proxy Password",
                type = AttributeType.PASSWORD
        )
        String proxy_password();

        @AttributeDefinition()
        String webconsole_configurationFactory_nameHint()
                default "'{aspect.key}' ({service.ranking}) - use system: {proxy.system.default}, proxy: {proxy.host}:{proxy.port}";
    }

    private Config config;

    private HttpHost proxyHost;
    private Credentials proxyCredentials;
    private AuthScheme proxyAuthScheme = new BasicScheme();

    @Override
    public void configure(@NotNull final HttpClientContext context) {
        if (proxyCredentials != null) {
            CredentialsProvider credentialsProvider = context.getCredentialsProvider();
            if (credentialsProvider == null) {
                credentialsProvider = new BasicCredentialsProvider();
                context.setCredentialsProvider(credentialsProvider);
            }
            credentialsProvider.setCredentials(new AuthScope(proxyHost), proxyCredentials);
        }
        if (proxyAuthScheme != null) {
            AuthCache authCache = context.getAuthCache();
            if (authCache == null) {
                authCache = new BasicAuthCache();
                context.setAuthCache(authCache);
            }
            context.getAuthCache().put(proxyHost, proxyAuthScheme);
        }
    }

    @Override
    public void configure(@NotNull final HttpClientBuilder builder) {
        if (config != null) {
            if (config.proxy_system_default()) {
                builder.setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
            } else if (proxyHost != null) {
                builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxyHost));
            }
        }
    }

    @Activate
    @Modified
    protected void activate(final BundleContext bundleContext, final RemoteProxyBuilder.Config config) {
        this.config = config;
        proxyHost = null;
        proxyCredentials = null;
        proxyAuthScheme = null;
        if (!config.proxy_system_default() && StringUtils.isNotBlank(config.proxy_host()) && config.proxy_port() > 0) {
            proxyHost = new HttpHost(config.proxy_host(), config.proxy_port());
            if (StringUtils.isNotBlank(config.proxy_username()) && StringUtils.isNotBlank(config.proxy_password())) {
                proxyCredentials = new UsernamePasswordCredentials(config.proxy_username(), config.proxy_password());
                proxyAuthScheme = new BasicScheme();
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        proxyAuthScheme = null;
        proxyCredentials = null;
        proxyHost = null;
        config = null;
    }
}
