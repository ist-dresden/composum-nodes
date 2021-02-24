package com.composum.sling.nodes.mount.remote;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Component(
        name = "com.composum.sling.nodes.remote.resolver.RemoteProvider",
        service = ResourceProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Remote Resource Provider"
        }
)
@Designate(ocd = RemoteProvider.Config.class, factory = true)
public class RemoteProvider extends ResourceProvider<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteProvider.class);

    @ObjectClassDefinition(name = "Composum Nodes Remote Resource Provider")
    public @interface Config {

        @AttributeDefinition(name = "Provider Root")
        String provider_root();

        @AttributeDefinition(name = "Resolver Search Path")
        String[] resolver_search_path() default {
                "${root}/apps/",
                "${root}/libs/"
        };

        @AttributeDefinition(name = "Ignored Path Patterns")
        String[] ignored_patterns() default {
                "^/mnt/.*$"
        };

        @AttributeDefinition(name = "Remote HTTP URL")
        String remote_url_http();

        @AttributeDefinition(name = "Username")
        String login_username();

        @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD)
        String login_password();

        @AttributeDefinition()
        String webconsole_configurationFactory_nameHint()
                default "local: {provider.root}, remote: {remote.url.http}";
    }

    protected BundleContext bundleContext;
    protected Config config;

    protected String localRoot;
    protected String[] searchPath;
    protected List<Pattern> ignoredPathPatterns;

    protected RemoteReader reader;
    protected RemoteWriter writer;

    @Activate
    protected void activate(final BundleContext bundleContext, final Config config) {
        this.bundleContext = bundleContext;
        this.config = config;
        localRoot = config.provider_root();
        searchPath = config.resolver_search_path().clone();
        for (int i = 0; i < searchPath.length; i++) {
            searchPath[i] = searchPath[i].replaceAll("\\$\\{root}", localRoot);
        }
        ignoredPathPatterns = new ArrayList<>();
        for (String rule : config.ignored_patterns()) {
            ignoredPathPatterns.add(Pattern.compile(rule.replaceAll("\\$\\{root}", localRoot)));
        }
        reader = new RemoteReader(this, config.remote_url_http(),
                config.login_username(), config.login_password());
        writer = new RemoteWriter(this, config.remote_url_http(),
                config.login_username(), config.login_password());
    }

    @Deactivate
    protected void deactivate() {
        writer = null;
        reader = null;
        ignoredPathPatterns = null;
        searchPath = null;
        localRoot = null;
        config = null;
        bundleContext = null;
    }

    public boolean isLocal(@Nonnull final String path) {
        return path.equals(localRoot) || path.startsWith(localRoot + "/");
    }

    /**
     * @return the repository path of the local resource
     */
    protected String localPath(@Nonnull String path) {
        String localRoot = getProviderRoot();
        if (!isLocal(path)) {
            path = localRoot + (path.startsWith("/") ? "" : "/") + path;
        }
        return path;
    }

    /**
     * @return the given (local) path transformed to the remote system
     */
    protected String remotePath(@Nonnull String path) {
        String localRoot = getProviderRoot();
        if (isLocal(path)) {
            path = path.substring(localRoot.length());
        }
        return path;
    }

    protected boolean ignoreIt(@Nonnull final String path) {
        for (Pattern pattern : ignoredPathPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public String getProviderRoot() {
        return localRoot;
    }

    @Nullable
    @Override
    public Resource getResource(@Nonnull final ResolveContext<Object> ctx, @Nonnull final String path,
                                @Nonnull final ResourceContext resourceContext, @Nullable final Resource parent) {
        if (ignoreIt(path)) {
            return null;
        }
        RemoteResolver remoteResolver = new RemoteResolver(this, ctx.getResourceResolver());
        return remoteResolver.getResource(path);
    }

    @Nullable
    @Override
    public Iterator<Resource> listChildren(@Nonnull final ResolveContext<Object> ctx, @Nonnull final Resource parent) {
        return parent.listChildren();
    }
}
