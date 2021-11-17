package com.composum.sling.nodes.mount.remote;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A remote resource provider enables the mounting of a remote Sling system via HTTP based on
 * the JSON data rendered by default Sling GET servlet. The CRUD operations to manipulate the
 * remote resources are using the default Sling POST servlet.
 */
@Component(
        name = "com.composum.sling.nodes.mount.remote.RemoteProvider",
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

        @AttributeDefinition(
                name = "Provider Root",
                description = "the mount point of the remote tree in the local resource hierarchy"
        )
        String provider_root();

        @AttributeDefinition(
                name = "Resolver Search Path",
                description = "the resolver search path used by the remote system mapped to the local tree; use ${root} as placeholder for the provider root path"
        )
        String[] resolver_search_path() default {
                "${root}/apps/",
                "${root}/libs/"
        };

        @AttributeDefinition(
                name = "Ignored Path Patterns",
                description = "the set of path patterns in the local tree which should be ignored by this provider"
        )
        String[] ignored_patterns() default {
                "^/mnt(/.*)?$",
                "^${root}/mnt(/.*)?$",
                "^${root}/(api|bin|index|is|resource-status)$",
                "^${root}(/system/sling)?/login[^/]*$",
                "^${root}(/.*)?/[^/]+\\.servlet$"
        };

        @AttributeDefinition(
                name = "Remote HTTP URL",
                description = "the URL to access the remote system (the HTTP URL of the remote repository root)"
        )
        String remote_url();

        @AttributeDefinition(
                name = "Username"
        )
        String login_username();

        @AttributeDefinition(
                name = "Password",
                type = AttributeType.PASSWORD
        )
        String login_password();

        @AttributeDefinition(
                name = "Client Config",
                description = "client builder service keys to extend the remote client builder"
        )
        String[] client_configuration();

        @AttributeDefinition(
                name = "HTTP Request Headers",
                description = "request headers to use: <name>=<value>"
        )
        String[] request_headers();

        @AttributeDefinition()
        String webconsole_configurationFactory_nameHint()
                default "local: {provider.root}, remote: {remote.url}, extensions: {client.configuration}";
    }

    @Reference
    protected RemoteClientSetup clientSetup;

    protected BundleContext bundleContext;
    private Config config;

    protected String localRoot;
    protected String[] searchPath;
    protected List<Pattern> ignoredPathPatterns;

    protected RemoteClient remoteClient;
    protected RemoteReader remoteReader;
    protected RemoteWriter remoteWriter;

    @Activate
    @Modified
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
        remoteClient = new RemoteClient(this, config, Arrays.asList(config.client_configuration()));
        remoteReader = new RemoteReader(this);
        remoteWriter = new RemoteWriter(this);
    }

    @Deactivate
    protected void deactivate() {
        remoteWriter = null;
        remoteReader = null;
        remoteClient = null;
        ignoredPathPatterns = null;
        searchPath = null;
        localRoot = null;
        config = null;
        bundleContext = null;
    }

    /**
     * @return 'true' if the path is part of the local repository tree (starts with the provider root)
     */
    public boolean isLocal(@NotNull final String path) {
        return path.equals(localRoot) || path.startsWith(localRoot + "/");
    }

    /**
     * @return the repository path of the local resource
     */
    protected String localPath(@NotNull String path) {
        String localRoot = getProviderRoot();
        if (!isLocal(path)) {
            path = localRoot + (path.startsWith("/") ? "" : "/") + path;
        }
        return path;
    }

    /**
     * @return the given (local) path transformed to the remote system
     */
    protected String remotePath(@NotNull String path) {
        String localRoot = getProviderRoot();
        if (isLocal(path)) {
            path = path.substring(localRoot.length());
        }
        return path;
    }

    /**
     * @return 'true' if the given repository path should be ignored in the remote tree
     */
    protected boolean ignoreIt(@NotNull final String path) {
        for (Pattern pattern : ignoredPathPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the mount point of this provider in the repository tree
     */
    @NotNull
    public String getProviderRoot() {
        return localRoot;
    }

    /**
     * @return the remote resource if the path in in the scope of this provider, otherwise 'null'
     */
    @Nullable
    @Override
    public Resource getResource(@NotNull final ResolveContext<Object> ctx, @NotNull final String path,
                                @NotNull final ResourceContext resourceContext, @Nullable final Resource parent) {
        if (ignoreIt(path) || !remoteClient.isValid()) {
            return null;
        }
        RemoteResolver remoteResolver = new RemoteResolver(this, ctx.getResourceResolver());
        return remoteResolver.getResource(path);
    }

    /**
     * @return the children retrieved using the the parent resource itself
     */
    @Nullable
    @Override
    public Iterator<Resource> listChildren(@NotNull final ResolveContext<Object> ctx, @NotNull final Resource parent) {
        return parent.listChildren();
    }
}
