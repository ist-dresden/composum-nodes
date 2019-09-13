package com.composum.sling.clientlibs.service;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

@Component(
        label = "Composum Core Clientlib Configuration",
        description = "Delivers the composed clienlib content bundled and compressed.",
        immediate = true,
        metatype = true
)
@Service
public class ClientlibConfigurationService implements ClientlibConfiguration {

    public static final String TAGDEBUG = "tagdebug";
    @Property(
            name = TAGDEBUG,
            label = "Tag-Debug",
            description = "Inserts HTML comments with the client libraries that have been called up into the page",
            boolValue = false
    )
    protected boolean tagdebug;

    // CSS configuration

    public static final String DEBUG = "debug";
    @Property(
            name = DEBUG,
            label = "Debug",
            description = "let files unchanged and unbundled if set to 'true'",
            boolValue = false
    )
    protected boolean debug;

    public static final String RERENDER_ON_NOCACHE = "rerender.on.nocache";
    @Property(
            name = RERENDER_ON_NOCACHE,
            label = "Rerender On Nocache",
            description = "Renders clientlib again if a no-cache header is received - mainly for debugging purposes. Changes in the JS / CSS files automatically lead to re-rendering the clientlib.",
            boolValue = false
    )
    protected boolean rerenderOnNocache;

    public static final String CSS_MINIMIZE = "css.minimize";
    @Property(
            name = CSS_MINIMIZE,
            label = "CSS - Minimize",
            description = "compress with the YUI compressor (if not 'debug' is set); default: true",
            boolValue = true
    )
    protected boolean cssMinimize;

    public static final String CSS_LINEBREAK = "css.lineBreak";
    @Property(
            name = CSS_LINEBREAK,
            label = "CSS - Line Break",
            description = "length of compressed CSS source lines (if not 'debug' is set); default: 0",
            intValue = 0
    )
    protected int cssLineBreak;

    public static final String CSS_DEFAULT_TEMPLATE = "  <link rel=\"stylesheet\" href=\"{0}\" />";
    public static final String CSS_TEMPLATE = "css.template";
    @Property(
            name = CSS_TEMPLATE,
            label = "CSS - Link Template",
            description = "the HTML template for the CSS link rendering",
            value = CSS_DEFAULT_TEMPLATE
    )
    protected String cssTemplate;

    // JS configuration

    public static final String JS_DEFAULT_TEMPLATE = "  <script type=\"text/javascript\" src=\"{0}\"></script>";
    public static final String JS_TEMPLATE = "javascript.template";
    @Property(
            name = JS_TEMPLATE,
            label = "JS - Template",
            description = "the HTML template for the Javascript tag rendering",
            value = JS_DEFAULT_TEMPLATE
    )
    protected String jsTemplate;

    // Link rendering configuration

    public static final String LINK_DEFAULT_TEMPLATE = "  <link rel=\"{1}\" href=\"{0}\" />";
    public static final String LINK_TEMPLATE = "link.template";
    @Property(
            name = LINK_TEMPLATE,
            label = "Link - Template",
            description = "the HTML template for the general link rendering",
            value = LINK_DEFAULT_TEMPLATE
    )
    protected String linkTemplate;

    // general configuration

    public static final boolean DEFAULT_GZIP_ENABLED = false;
    public static final String GZIP_ENABLED = "gzip.enabled";
    @Property(
            name = GZIP_ENABLED,
            label = "General - GZip enabled",
            description = "if 'true' the content is zippend if possible",
            boolValue = DEFAULT_GZIP_ENABLED
    )
    protected boolean gzipEnabled;

    public static final String DEFAULT_CACHE_ROOT = "/var/composum/clientlibs";
    public static final String CACHE_ROOT = "clientlibs.cache.root";
    @Property(
            name = CACHE_ROOT,
            label = "General - Cache Root",
            description = "the root folder for the Javascript clientlib cache",
            value = DEFAULT_CACHE_ROOT
    )
    protected String cacheRoot;

    public static final boolean DEFAULT_USE_MINIFIED_FILES = true;
    public static final String USE_MINIFIED_FILES = "clientlibs.files.minified";
    @Property(
            name = USE_MINIFIED_FILES,
            label = "General - Use minified CSS/JS",
            description = "if 'on' for all clientlib files which have a '.min' sibling the '.min' files is used; default: 'on'",
            boolValue = DEFAULT_USE_MINIFIED_FILES
    )
    protected boolean useMinifiedFiles;

    public static final boolean DEFAULT_MAP_CLIENTLIB_URLS = true;
    public static final String MAP_CLIENTLIB_URLS = "clientlibs.url.map";
    @Property(
            name = MAP_CLIENTLIB_URLS,
            label = "General - Map Clientlib URLs",
            description = "if 'on' all clientlib URLs are mapped by the Resource Resolver; default: 'on'",
            boolValue = DEFAULT_MAP_CLIENTLIB_URLS
    )
    protected boolean mapClientlibURLs;

    public static final int DEFAULT_THREAD_POOL_MIN = 10;
    public static final String MIN_THREAD_POOL_SIZE = "clientlibs.threadpool.min";
    @Property(
            name = MIN_THREAD_POOL_SIZE,
            label = "General - Threadpool min",
            description = "the minimum size of the thread pool for clientlib processing (must be "
                    + DEFAULT_THREAD_POOL_MIN + " or greater)",
            intValue = DEFAULT_THREAD_POOL_MIN
    )
    protected int threadPoolMin;

    public static final int DEFAULT_THREAD_POOL_MAX = 20;
    public static final String MAX_THREAD_POOL_SIZE = "clientlibs.threadpool.max";
    @Property(
            name = MAX_THREAD_POOL_SIZE,
            label = "General - Threadpool max",
            description = "the size (maximum) of the thread pool for clientlib processing (must be equal or greater than the minimum)",
            intValue = DEFAULT_THREAD_POOL_MAX
    )
    protected int threadPoolMax;

    public static final int DEFAULT_RESOLVER_CACHETIME = 60;
    public static final String RESOLVER_CACHETIME = "clientlibs.resolver.cachetime";
    @Property(
            name = RESOLVER_CACHETIME,
            label = "General - Resolver cachetime",
            description = "the time (in seconds) the clientlib resolver caches the locations of all client libraries for a category. <=0 means no caching.",
            intValue = DEFAULT_RESOLVER_CACHETIME
    )
    protected int resolverCachetime;

    // CSS configuration

    @Override
    public boolean getCssMinimize() {
        return cssMinimize;
    }

    @Override
    public int getCssLineBreak() {
        return cssLineBreak;
    }

    @Override
    public String getCssTemplate() {
        return cssTemplate;
    }

    // JS configuration

    @Override
    public String getJavascriptTemplate() {
        return jsTemplate;
    }

    // Link configuration

    @Override
    public String getLinkTemplate() {
        return linkTemplate;
    }

    // general configuration

    @Override
    public boolean getDebug() {
        return debug;
    }

    @Override
    public boolean getMapClientlibURLs() {
        return mapClientlibURLs;
    }

    @Override
    public boolean getUseMinifiedFiles() {
        return useMinifiedFiles;
    }

    @Override
    public boolean getGzipEnabled() {
        return gzipEnabled;
    }

    @Override
    public String getCacheRoot() {
        return cacheRoot;
    }

    @Override
    public int getThreadPoolMin() {
        return threadPoolMin;
    }

    @Override
    public int getThreadPoolMax() {
        return threadPoolMax;
    }

    /** General - Resolver cachetime : the time (in seconds) the clientlib resolver caches the locations of all client libraries for a category. <=0 means no caching. */
    @Override
    public int getResolverCachetime() {
        return resolverCachetime;
    }

    /** Inserts HTML comments with the client libraries that have been called up into the page */
    @Override
    public boolean getTagDebug() {
        return tagdebug;
    }

    @Override
    public boolean getRerenderOnNocache() {
        return rerenderOnNocache;
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        // CSS configuration
        debug = PropertiesUtil.toBoolean(properties.get(DEBUG), false);
        cssMinimize = PropertiesUtil.toBoolean(properties.get(CSS_MINIMIZE), true);
        cssLineBreak = PropertiesUtil.toInteger(properties.get(CSS_LINEBREAK), 0);
        cssTemplate = PropertiesUtil.toString(properties.get(CSS_TEMPLATE), CSS_DEFAULT_TEMPLATE);
        // JS configuration
        jsTemplate = PropertiesUtil.toString(properties.get(JS_TEMPLATE), JS_DEFAULT_TEMPLATE);
        // Link configuration
        linkTemplate = PropertiesUtil.toString(properties.get(LINK_TEMPLATE), LINK_DEFAULT_TEMPLATE);
        // general configuration
        mapClientlibURLs = PropertiesUtil.toBoolean(properties.get(MAP_CLIENTLIB_URLS), DEFAULT_MAP_CLIENTLIB_URLS);
        useMinifiedFiles = !debug && PropertiesUtil.toBoolean(properties.get(USE_MINIFIED_FILES), DEFAULT_USE_MINIFIED_FILES);
        gzipEnabled = PropertiesUtil.toBoolean(properties.get(GZIP_ENABLED), DEFAULT_GZIP_ENABLED);
        cacheRoot = PropertiesUtil.toString(properties.get(CACHE_ROOT), DEFAULT_CACHE_ROOT);
        threadPoolMin = PropertiesUtil.toInteger(properties.get(MIN_THREAD_POOL_SIZE), DEFAULT_THREAD_POOL_MIN);
        threadPoolMax = PropertiesUtil.toInteger(properties.get(MAX_THREAD_POOL_SIZE), DEFAULT_THREAD_POOL_MAX);
        if (threadPoolMin < DEFAULT_THREAD_POOL_MIN) threadPoolMin = DEFAULT_THREAD_POOL_MIN;
        if (threadPoolMax < threadPoolMin) threadPoolMax = threadPoolMin;
        resolverCachetime = PropertiesUtil.toInteger(properties.get(RESOLVER_CACHETIME), DEFAULT_RESOLVER_CACHETIME);
        rerenderOnNocache = PropertiesUtil.toBoolean(properties.get(RERENDER_ON_NOCACHE), false);
    }
}
