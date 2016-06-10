package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.clientlibs.processor.CssProcessor;
import com.composum.sling.clientlibs.processor.CssUrlMapper;
import com.composum.sling.clientlibs.processor.GzipProcessor;
import com.composum.sling.clientlibs.processor.JavascriptProcessor;
import com.composum.sling.clientlibs.processor.LinkRenderer;
import com.composum.sling.clientlibs.processor.ProcessorContext;
import com.composum.sling.clientlibs.processor.ProcessorPipeline;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component(
        label = "Clientlib Service",
        description = "Delivers clienlib content bundled and compressed.",
        immediate = true,
        metatype = true
)
@Service
public class DefaultClientlibService implements ClientlibService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientlibService.class);

    public static final boolean DEFAULT_GZIP_ENABLED = false;
    public static final String GZIP_ENABLED = "gzip.enabled";
    @Property(
            name = GZIP_ENABLED,
            label = "GZip enabled",
            description = "if 'true' the content is zippend if possible",
            boolValue = DEFAULT_GZIP_ENABLED
    )
    protected boolean gzipEnabled;

    public static final String DEFAULT_CACHE_ROOT = "/var/cache/clientlibs";
    public static final String CACHE_ROOT = "clientlibs.cache.root";
    @Property(
            name = CACHE_ROOT,
            label = "Cache Root",
            description = "the root folder for the Javascript clientlib cache",
            value = DEFAULT_CACHE_ROOT
    )
    protected String cacheRoot;

    public static final boolean DEFAULT_MAP_CLIENTLIB_URLS = false;
    public static final String MAP_CLIENTLIB_URLS = "clientlib.url.map";
    @Property(
            name = MAP_CLIENTLIB_URLS,
            label = "Map Clientlib URLs",
            description = "if 'on' all clientlib URLs are mapped by the Resource Resolver; default: 'off'",
            boolValue = DEFAULT_MAP_CLIENTLIB_URLS
    )
    private boolean mapClientlibURLs;

    public static final int DEFAULT_THREAD_POOL_MIN = 5;
    public static final String MIN_THREAD_POOL_SIZE = "clientlibs.threadpool.min";
    @Property(
            name = MIN_THREAD_POOL_SIZE,
            label = "Threadpool min",
            description = "the minimum size of the thread pool for clientlib processing (must be "
                    + DEFAULT_THREAD_POOL_MIN + " or greater)",
            intValue = DEFAULT_THREAD_POOL_MIN
    )
    protected int threadPoolMin;

    public static final int DEFAULT_THREAD_POOL_MAX = 20;
    public static final String MAX_THREAD_POOL_SIZE = "clientlibs.threadpool.max";
    @Property(
            name = MAX_THREAD_POOL_SIZE,
            label = "Threadpool max",
            description = "the size (maximum) of the thread pool for clientlib processing (must be equal or greater tahn the minimum)",
            intValue = DEFAULT_THREAD_POOL_MAX
    )
    protected int threadPoolMax;

    public static final Map<String, Object> CRUD_CACHE_FOLDER_PROPS;

    static {
        CRUD_CACHE_FOLDER_PROPS = new HashMap<>();
        CRUD_CACHE_FOLDER_PROPS.put(ResourceUtil.PROP_PRIMARY_TYPE, "sling:Folder");
    }

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Reference
    protected SequencerService sequencer;

    @Reference
    protected JavascriptProcessor javascriptProcessor;

    @Reference
    protected CssProcessor cssProcessor;

    @Reference
    protected LinkRenderer linkRenderer;

    @Reference
    protected GzipProcessor gzipProcessor;

    protected ThreadPoolExecutor executorService = null;

    protected EnumMap<Clientlib.Type, ClientlibRenderer> rendererMap;
    protected EnumMap<Clientlib.Type, ClientlibProcessor> processorMap;

    @Override
    public boolean mapClientlibURLs() {
        return mapClientlibURLs;
    }

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context)
            throws IOException {
        Clientlib.Type type = clientlib.getType();
        ClientlibRenderer renderer = rendererMap.get(type);
        if (renderer != null) {
            renderer.renderClientlibLinks(clientlib, properties, writer, context);
        }
    }

    protected String adjustEncoding(String encoding) {
        if (ENCODING_GZIP.equals(encoding) && !gzipEnabled) {
            encoding = null;
        }
        return encoding;
    }

    @Override
    public void resetContent(Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException {

        encoding = adjustEncoding(encoding);
        String cachePath = getCachePath(clientlib, encoding);

        FileHandle file = getCachedFile(clientlib, cachePath);
        if (file != null && file.isValid()) {

            ResourceResolver resolver = createResolverForChanges();
            try {
                resolver.delete(file.getResource());
                resolver.commit();
            } finally {
                resolver.close();
            }
        }
    }

    @Override
    public void deliverContent(final Clientlib clientlib, OutputStream outputStream, String encoding)
            throws IOException, RepositoryException {
        encoding = adjustEncoding(encoding);
        String cachePath = getCachePath(clientlib, encoding);
        FileHandle file = getCachedFile(clientlib, cachePath);
        if (file != null && file.isValid()) {
            InputStream content = file.getStream();
            if (content != null) {
                try {
                    IOUtils.copy(content, outputStream);
                } finally {
                    IOUtils.closeQuietly(content);
                }
            }
        }
    }

    @Override
    public Map<String, Object> prepareContent(final SlingHttpServletRequest request,
                                              final Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException {

        final Map<String, Object> hints = new HashMap<>();

        encoding = adjustEncoding(encoding);
        String cachePath = getCachePath(clientlib, encoding);

        SequencerService.Token token = sequencer.acquire(cachePath);
        try {
            FileHandle file = getCachedFile(clientlib, cachePath);

            if (file == null || !file.isValid()) {
                LOG.info("prepare clientlib '" + clientlib.getPath() + "'...");

                ResourceResolver resolver = createResolverForChanges();
                try {
                    final ProcessorContext context = new ProcessorContext(
                            request, resolver, executorService, hints, mapClientlibURLs());

                    final Clientlib.Type type = clientlib.getType();

                    Resource cacheEntry = resolver.getResource(cachePath);
                    if (cacheEntry != null) {
                        resolver.delete(cacheEntry);
                        resolver.commit();
                    }

                    String[] separated = Clientlib.splitPathAndName(cachePath);
                    Resource parent = giveParent(resolver, separated[0]);
                    cacheEntry = resolver.create(parent, separated[1], FileHandle.CRUD_FILE_PROPS);
                    resolver.create(cacheEntry, ResourceUtil.CONTENT_NODE, FileHandle.CRUD_CONTENT_PROPS);

                    file = new FileHandle(cacheEntry);
                    if (file.isValid()) {
                        LOG.debug("create clientlib cache content '" + file.getResource().getPath() + "'...");

                        final ClientlibProcessor processor = processorMap.get(type);
                        final PipedOutputStream outputStream = new PipedOutputStream();
                        InputStream inputStream = new PipedInputStream(outputStream);
                        context.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    clientlib.processContent(outputStream, processor, context);
                                } catch (IOException | RepositoryException ex) {
                                    LOG.error(ex.getMessage(), ex);
                                }
                            }
                        });
                        if (ENCODING_GZIP.equals(encoding)) {
                            inputStream = gzipProcessor.processContent(clientlib, inputStream, context);
                        }

                        file.storeContent(inputStream);

                        ModifiableValueMap contentValues = file.getContent().adaptTo(ModifiableValueMap.class);
                        contentValues.put(ResourceUtil.PROP_LAST_MODIFIED, clientlib.getLastModified());
                        contentValues.putAll(hints);

                        resolver.commit();
                        LOG.debug("clientlib cache content '" + file.getResource().getPath() + "' created.");

                        getFileHints(file, hints);

                    } else {
                        LOG.error("can't create cache content in '" +
                                (file != null ? file.getResource().getPath() : "null") + "'!");
                    }
                } finally {
                    resolver.close();
                }
            } else {
                getFileHints(file, hints);
            }

        } finally {
            sequencer.release(token);
        }

        return hints;
    }

    protected ResourceResolver createResolverForChanges() throws LoginException {
        // used for maximum backwards compatibility; TODO recheck and decide from time to time
        ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
        return resolver;
    }

    protected void getFileHints(final FileHandle file, final Map<String, Object> hints) {
        if (file.isValid()) {
            ValueMap contentValues = file.getContent().adaptTo(ValueMap.class);
            hints.put(ResourceUtil.PROP_LAST_MODIFIED, contentValues.get(ResourceUtil.PROP_LAST_MODIFIED));
            hints.put(ResourceUtil.PROP_MIME_TYPE, contentValues.get(ResourceUtil.PROP_MIME_TYPE));
            hints.put(ResourceUtil.PROP_ENCODING, contentValues.get(ResourceUtil.PROP_ENCODING));
            hints.put("size", file.getSize());
        }
    }

    protected FileHandle getCachedFile(Clientlib clientlib, String path) {
        ResourceResolver resolver = clientlib.getResolver();
        FileHandle file = new FileHandle(resolver.getResource(path));
        if (file.isValid()) {
            Calendar cacheTimestamp = file.getLastModified();
            if (cacheTimestamp != null) {
                Calendar libLastModified = clientlib.getLastModified();
                if (libLastModified.after(cacheTimestamp)) {
                    file = null;
                }
            } else {
                file = null;
            }
        }
        return file;
    }

    protected String getCachePath(Clientlib clientlib, String encoding) {
        String path = clientlib.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = cacheRoot + path;
        if (StringUtils.isNotBlank(encoding)) {
            path += '.' + encoding;
        }
        return path;
    }

    protected synchronized Resource giveParent(ResourceResolver resolver, String path) {
        Resource resource = null;
        SequencerService.Token token = sequencer.acquire(path);
        try {
            resource = resolver.getResource(path);
            if (resource == null) {
                String[] separated = Clientlib.splitPathAndName(path);
                Resource parent = giveParent(resolver, separated[0]);
                try {
                    resource = resolver.create(parent, separated[1], CRUD_CACHE_FOLDER_PROPS);
                    resolver.commit();
                } catch (PersistenceException pex) {
                    // catch it and hope that the parent is available
                    // necessary to continue on transaction isolation problems
                    LOG.error("clientlib giveParent('" + path + "'): " + pex.toString());
                }
            }
        } finally {
            sequencer.release(token);
        }
        return resource;
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        mapClientlibURLs = PropertiesUtil.toBoolean(properties.get(MAP_CLIENTLIB_URLS), DEFAULT_MAP_CLIENTLIB_URLS);
        gzipEnabled = PropertiesUtil.toBoolean(properties.get(GZIP_ENABLED), DEFAULT_GZIP_ENABLED);
        cacheRoot = PropertiesUtil.toString(properties.get(CACHE_ROOT), DEFAULT_CACHE_ROOT);
        threadPoolMin = PropertiesUtil.toInteger(properties.get(MIN_THREAD_POOL_SIZE), DEFAULT_THREAD_POOL_MIN);
        threadPoolMax = PropertiesUtil.toInteger(properties.get(MAX_THREAD_POOL_SIZE), DEFAULT_THREAD_POOL_MAX);
        if (threadPoolMin < DEFAULT_THREAD_POOL_MIN) threadPoolMin = DEFAULT_THREAD_POOL_MIN;
        if (threadPoolMax < threadPoolMin) threadPoolMax = threadPoolMin;
        executorService = new ThreadPoolExecutor(threadPoolMin, threadPoolMax,
                200L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        rendererMap = new EnumMap<>(Clientlib.Type.class);
        rendererMap.put(Clientlib.Type.js, javascriptProcessor);
        rendererMap.put(Clientlib.Type.css, cssProcessor);
        rendererMap.put(Clientlib.Type.link, linkRenderer);
        processorMap = new EnumMap<>(Clientlib.Type.class);
        processorMap.put(Clientlib.Type.js, javascriptProcessor);
        processorMap.put(Clientlib.Type.css, mapClientlibURLs()
                ? new ProcessorPipeline(new CssUrlMapper(), cssProcessor)
                : cssProcessor);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        synchronized (this) {
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
        }
    }
}
