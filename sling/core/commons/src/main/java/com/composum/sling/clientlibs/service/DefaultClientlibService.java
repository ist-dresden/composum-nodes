package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibCategory;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibExternalUri;
import com.composum.sling.clientlibs.handle.ClientlibFile;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.handle.ClientlibResourceFolder;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.clientlibs.processor.CssProcessor;
import com.composum.sling.clientlibs.processor.CssUrlMapper;
import com.composum.sling.clientlibs.processor.GzipProcessor;
import com.composum.sling.clientlibs.processor.JavascriptProcessor;
import com.composum.sling.clientlibs.processor.LinkRenderer;
import com.composum.sling.clientlibs.processor.ProcessingVisitor;
import com.composum.sling.clientlibs.processor.ProcessorContext;
import com.composum.sling.clientlibs.processor.ProcessorPipeline;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.processor.UpdateTimeVisitor;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.concurrent.LazyCreationService;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.clientlibs.handle.Clientlib.PROP_CATEGORY;
import static com.composum.sling.clientlibs.handle.Clientlib.PROP_ORDER;
import static com.composum.sling.clientlibs.handle.Clientlib.RESOURCE_TYPE;
import static com.composum.sling.clientlibs.handle.Clientlib.Type;
import static com.composum.sling.core.util.ResourceUtil.PROP_RESOURCE_TYPE;
import static com.composum.sling.core.util.ResourceUtil.TYPE_SLING_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service related to {@link Clientlib} .
 */
@Component(label = "Composum Core Clientlib Service 2", description = "Delivers the composed clientlib content " +
        "bundled and compressed.", immediate = true)
@Service
public class DefaultClientlibService implements ClientlibService {

    public static final String MINIFIED_SELECTOR = ".min";
    public static final Pattern UNMINIFIED_PATTERN = Pattern.compile("^(.+/)([^/]+)(\\.min)?(\\.[^.]+)$");
    public static final Pattern MINIFIED_PATTERN = Pattern.compile("^(.+/)([^/]+)(\\.min)(\\.[^.]+)?$");

    /**
     * Property at content node of cache files that contains the hash value usable as etag that determines the
     * current content of the client library and changes on each added / updated file.
     */
    public static final String PROP_HASH = ResourceUtil.PROP_DESCRIPTION;

    public static final Map<String, Object> CRUD_CACHE_FOLDER_PROPS;

    /** Top node for the category cache within the {@link ClientlibConfiguration#getCacheRoot()}. */
    protected static final String CATEGORYCACHE = "categorycache";

    static {
        CRUD_CACHE_FOLDER_PROPS = new HashMap<>();
        CRUD_CACHE_FOLDER_PROPS.put(com.composum.sling.core.util.ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil
                .TYPE_SLING_FOLDER);
    }

    private static final Logger LOG = getLogger(DefaultClientlibService.class);

    @Reference
    protected ClientlibConfiguration clientlibConfig;

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Reference
    protected SequencerService sequencer;

    @Reference
    protected LazyCreationService lazyCreationService;

    @Reference
    protected JavascriptProcessor javascriptProcessor;

    @Reference
    protected CssProcessor cssProcessor;

    @Reference
    protected LinkRenderer linkRenderer;

    @Reference
    protected GzipProcessor gzipProcessor;

    @Reference(referenceInterface = ClientlibPermissionPlugin.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            bind = "bindPermissionPlugin", unbind = "unbindPermissionPlugin"
    )
    protected final List<ClientlibPermissionPlugin> permissionPlugins = new CopyOnWriteArrayList<>();

    protected ThreadPoolExecutor executorService = null;

    protected EnumMap<Clientlib.Type, ClientlibRenderer> rendererMap;
    protected EnumMap<Clientlib.Type, ClientlibProcessor> processorMap;

    /**
     * Cache (String, Pair<Long, List<String>>) that maps categories to a pair of the query time and the resulting list
     * of paths to client libraries with that path. We set an arbitrary limit of 100 since otherwise we'd be open to a
     * DOS attack by retrieving random categories.
     */
    protected final LRUMap /*String, Pair<Long, List<String>>*/ categoryToPathCache = new LRUMap(100);

    protected synchronized void bindPermissionPlugin(ClientlibPermissionPlugin permissionPlugin) {
        permissionPlugins.add(permissionPlugin);
        categoryToPathCache.clear();
    }

    protected synchronized void unbindPermissionPlugin(ClientlibPermissionPlugin permissionPlugin) {
        permissionPlugins.remove(permissionPlugin);
        categoryToPathCache.clear();
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        executorService = new ThreadPoolExecutor(clientlibConfig.getThreadPoolMin(), clientlibConfig.getThreadPoolMax
                (), 200L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        rendererMap = new EnumMap<>(Clientlib.Type.class);
        rendererMap.put(Clientlib.Type.js, javascriptProcessor);
        rendererMap.put(Clientlib.Type.css, cssProcessor);
        rendererMap.put(Clientlib.Type.link, linkRenderer);
        processorMap = new EnumMap<>(Clientlib.Type.class);
        processorMap.put(Clientlib.Type.js, javascriptProcessor);
        processorMap.put(Clientlib.Type.css, getClientlibConfig().getMapClientlibURLs() ? new ProcessorPipeline(new
                CssUrlMapper(), cssProcessor) : cssProcessor);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }


    @Override
    public ClientlibElement resolve(ClientlibRef ref, ResourceResolver resolver) {
        if (ref != null /* shouldn't happen but was happened */) {
            if (ref.isCategory()) {
                List<Resource> resources = retrieveCategoryResources(ref.category, resolver);
                if (!resources.isEmpty()) return new ClientlibCategory(ref, resources);
            } else if (ref.isExternalUri()) {
                return new ClientlibExternalUri(ref.type, ref.externalUri, ref.properties);
            } else {
                Resource resource = retrieveResource(ref.path, resolver);
                if (null != resource) {
                    if (resource.isResourceType(RESOURCE_TYPE)) return new Clientlib(ref.type, resource);
                    if (ClientlibFile.isFile(resource)) {
                        resource = minificationVariant(resource);
                        return new ClientlibFile(ref, ref.type, resource, ref.properties);
                    }
                    LOG.warn("Ignored resource {} with unknown type {}", resource.getPath(), resource.getResourceType());
                }
            }
            if (ref.optional) {
                LOG.debug("Could not resolve {}", ref);
            } else {
                LOG.warn("Could not resolve {}", ref);
            }
        } else {
            try {
                throw new RuntimeException("Clientlib ref is NULL!");
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * For files we use the correct sibling wrt. {@link ClientlibConfiguration#getUseMinifiedFiles()}.
     */
    protected Resource minificationVariant(Resource resource) {
        if (getClientlibConfig().getUseMinifiedFiles()) {
            return getMinifiedSibling(resource);
        }
        return resource;
    }

    /**
     * Retrieve a resource from a resolver; if we don't find it, we try to retrieve the (un)minified sibling.
     */
    protected Resource retrieveResource(String path, ResourceResolver resolver) {
        Resource pathResource = retrieveResourceRaw(path, resolver);
        if (null == pathResource) {
            String unminifiedPath = getUnminifiedSibling(path);
            if (!path.equals(unminifiedPath)) pathResource = retrieveResourceRaw(unminifiedPath, resolver);
            if (null == pathResource) {
                String minifiedPath = getMinifiedSibling(path);
                if (!minifiedPath.equals(path)) pathResource = retrieveResourceRaw(minifiedPath, resolver);
            }
        }
        if (pathResource != null) LOG.debug("retrieveResource {} uses {}", path, pathResource);
        else LOG.debug("retrieveResource.failed: {}", path);
        return pathResource;
    }

    protected Resource retrieveResourceRaw(String path, ResourceResolver resolver) {
        Resource pathResource = null;
        if (path.startsWith("/")) {
            pathResource = resolver.getResource(path);
        } else {
            String[] searchPath = resolver.getSearchPath();
            for (int i = 0; pathResource == null && i < searchPath.length; i++) {
                String absolutePath = searchPath[i] + path;
                pathResource = resolver.getResource(absolutePath);
            }
        }
        return pathResource;
    }

    protected String getMinifiedSibling(String path) {
        Matcher matcher = UNMINIFIED_PATTERN.matcher(path);
        if (matcher.matches() && StringUtils.isBlank(matcher.group(3))) {
            String ext = matcher.group(4);
            String minified = matcher.group(1) + matcher.group(2) + MINIFIED_SELECTOR;
            if (StringUtils.isNotBlank(ext)) {
                minified += ext;
            }
            return minified;
        }
        return path;
    }

    protected String getUnminifiedSibling(String path) {
        Matcher matcher = MINIFIED_PATTERN.matcher(path);
        if (matcher.matches() && StringUtils.isNotBlank(matcher.group(3))) {
            String ext = matcher.group(4);
            String unminified = matcher.group(1) + matcher.group(2);
            if (StringUtils.isNotBlank(ext)) {
                unminified += ext;
            }
            return unminified;
        }
        return path;
    }

    @Override
    @Nonnull
    public Resource getMinifiedSibling(@Nonnull Resource resource) {
        String path = resource.getPath();
        String minifiedPath = getMinifiedSibling(path);
        if (!path.equals(minifiedPath)) {
            Resource minified = resource.getResourceResolver().getResource(minifiedPath);
            if (minified != null) {
                return minified;
            }
        }
        return resource;
    }

    /**
     * Retrieves the Clientlib-resources for a category. Uses the {@link #categoryToPathCache} for caching the paths
     * to avoid executing a query each time. We retrieve the paths with an {@link #createAdministrativeResolver()}
     * to find everything independent of the users rights, and re-retrieve the resources with the users resolver
     * afterwards, filtering out inaccessible things.
     */
    protected List<Resource> retrieveCategoryResources(String category, ResourceResolver resolver) {
        long cacheTime = TimeUnit.SECONDS.toMillis(clientlibConfig.getResolverCachetime());
        if (cacheTime <= 0) return retrieveResourcesForCategoryUncached(category, resolver);

        List<String> paths = null;
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (categoryToPathCache) {
            Pair<Long, List<String>> cacheEntry = (Pair<Long, List<String>>) categoryToPathCache.get(category);
            if (null != cacheEntry && cacheEntry.getLeft() >= (currentTimeMillis - cacheTime))
                paths = cacheEntry.getRight();
        }

        if (null == paths) {
            paths = new ArrayList<>();
            ResourceResolver administrativeResolver = createAdministrativeResolver();
            try {
                List<Resource> resourcesForAdmin = retrieveResourcesForCategoryUncached(category,
                        administrativeResolver);
                for (Resource resource : resourcesForAdmin) paths.add(resource.getPath());
            } finally {
                administrativeResolver.close();
            }

            synchronized (categoryToPathCache) {
                categoryToPathCache.put(category, Pair.of(currentTimeMillis, paths));
            }
        }

        // retrieve with the users resolver
        List<Resource> resources = new ArrayList<>();
        for (String path : paths) {
            Resource resource = resolver.getResource(path);
            if (null != resource) { // user actually has rights for this resource
                resources.add(resource);
            }
        }
        return resources;
    }

    protected List<Resource> retrieveResourcesForCategoryUncached(String category, ResourceResolver resolver) {
        List<Resource> resources = new ArrayList<>();
        Set<String> foundlibs = new HashSet<>();
        List<ResourceFilter> permissionFilters = getCategoryPermissionFilters(category);
        for (String searchPathElement : resolver.getSearchPath()) {
            String xpath = "/jcr:root" + searchPathElement.replaceFirst("/+$", "") + "//element(*," +
                    TYPE_SLING_FOLDER + ")" + "[@" + PROP_RESOURCE_TYPE + "='" + RESOURCE_TYPE + "'" + " and @" +
                    PROP_CATEGORY + "='" + category + "']";
            for (Iterator<Resource> iterator = resolver.findResources(xpath, Query.XPATH); iterator.hasNext(); ) {
                Resource foundResource = iterator.next();
                ResourceHandle handle = ResourceHandle.use(foundResource);
                String libPath = handle.getPath();
                String key = libPath.substring(libPath.indexOf(searchPathElement) + searchPathElement.length());
                if (!foundlibs.contains(key) && isClientlibPermitted(permissionFilters, handle)) { // first wins - e.g. /apps shadows /libs
                    foundlibs.add(key);
                    resources.add(handle);
                }
            }
        }
        Collections.sort(resources, orderResourceComparator);
        return resources;
    }

    /** Retrieves the {@link ResourceFilter}s for a category from all {@link ClientlibPermissionPlugin}s. */
    @Nonnull
    protected List<ResourceFilter> getCategoryPermissionFilters(String category) {
        List<ResourceFilter> result = new ArrayList<>();
        if (permissionPlugins != null) {
            for (ClientlibPermissionPlugin plugin : permissionPlugins) {
                result.add(plugin.categoryFilter(category));
            }
        }
        return result;
    }

    /** Checks whether a clientlib matches all {@link ResourceFilter}s for the given category. */
    protected boolean isClientlibPermitted(@Nonnull List<ResourceFilter> filters, Resource clientlibResource) {
        boolean permitted = true;
        for (ResourceFilter filter : filters) {
            permitted = permitted && filter.accept(clientlibResource);
        }
        return permitted;
    }

    /**
     * Compares two client libraries by the {@link Clientlib#PROP_ORDER} attribute, resorting to path to at least
     * ensure a predictable order when there is no order attribute.
     */
    protected static final Comparator<Resource> orderResourceComparator = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            int order1 = ResourceHandle.use(o1).getProperty(PROP_ORDER, 0);
            int order2 = ResourceHandle.use(o2).getProperty(PROP_ORDER, 0);
            int res = Integer.compare(order1, order2);
            return res != 0 ? res : o1.getPath().compareTo(o2.getPath());
        }
    };

    protected ResourceResolver createAdministrativeResolver() {
        // used for maximum backwards compatibility; TODO recheck and decide from time to time
        try {
            return resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            throw new SlingException("Configuration problem: we cannot get an administrative resolver ", e);
        }
    }

    @Override
    public ClientlibConfiguration getClientlibConfig() {
        return clientlibConfig;
    }

    @Override
    public void renderClientlibLinks(ClientlibElement clientlib, Writer writer, SlingHttpServletRequest request,
                                     RendererContext context) throws
            IOException, RepositoryException {
        final Clientlib.Type type = clientlib.getType();
        final ClientlibRenderer renderer = rendererMap.get(type);
        if (renderer != null) {
            renderer.renderClientlibLinks(clientlib, writer, request, context);
        }
    }

    /**
     * {@inheritDoc}
     * <p> If the requestedHash (generated by the rendering process) equal to the saved hash, we have no
     * reason check whether the cache file is current. If it is not equal we also do not check if the
     * If-Modified-Since header ifModified is older than the files modification date.
     * <p>
     * We try to handle the following conditions. If the user cannot access the cache, the cache-file should not be
     * recreated. If a parallel process recreates the cache-file before this process manages to acquire the lock to
     * recreate it, it should not be recreated again.
     */
    @Override
    public ClientlibInfo prepareContent(SlingHttpServletRequest request, final ClientlibRef clientlibRef, boolean
            minified, String rawEncoding, boolean forceRefreshCache, String requestedHash, long ifModifiedSince) throws
            IOException, RepositoryException {
        ClientlibElement element = resolve(clientlibRef, request.getResourceResolver());
        if (null == element) {
            LOG.error("No client libraries found for {}", clientlibRef);
            throw new FileNotFoundException("No client libraries for " + clientlibRef);
        }

        final String encoding = adjustEncoding(rawEncoding);
        String cachePath = getCachePath(clientlibRef, minified, encoding);

        FileHandle cacheFile = new FileHandle(lazyCreationService.waitForInitialization(request.getResourceResolver(), cachePath));
        ClientlibInfo fileHints = getFileHints(cacheFile, element.makeLink());

        boolean ifModifiedIsOlder = (ifModifiedSince > 0) && (null != cacheFile.getLastModified()) &&
                (ifModifiedSince < cacheFile.getLastModified().getTimeInMillis());
        boolean cacheAssumedRecent = (null != fileHints) && (null != fileHints.hash) &&
                (fileHints.hash.equals(requestedHash) || ifModifiedIsOlder);
        boolean refreshForced = forceRefreshCache || null == fileHints || (null == cacheFile.getLastModified());

        if (refreshForced || !cacheAssumedRecent) {
            ResourceResolver adminResolver = null;
            try {
                // recheck since the admin resolver could change things, and when acquiring the lock the
                // cache file might have been recreated.
                adminResolver = createAdministrativeResolver();
                cacheFile = new FileHandle(adminResolver.getResource(cachePath));
                if (cacheFile.isValid() && null == request.getResourceResolver().getResource(cachePath)) {
                    refreshSession(request.getResourceResolver(), true);
                    if (null == request.getResourceResolver().getResource(cachePath)) { // shouldn't happen
                        LOG.warn("Cache file exists but is not accessible for user: {}", cachePath);
                        return null;
                    }
                }
                element = resolve(clientlibRef, adminResolver);

                UpdateTimeVisitor updateTimeVisitor = new UpdateTimeVisitor(element, this, adminResolver);
                updateTimeVisitor.execute();
                final String hash = updateTimeVisitor.getHash();
                String cacheFileHash = cacheFile.getContent().getProperty(PROP_HASH);

                if (!StringUtils.equals(requestedHash, hash)) {
                    // safety check to make sure continual up to date checks because of wrong permissions get noticed
                    UpdateTimeVisitor updateTimeVisitorAsUser = new UpdateTimeVisitor(element, this, request.getResourceResolver());
                    updateTimeVisitorAsUser.execute();
                    if (!StringUtils.equals(hash, updateTimeVisitorAsUser.getHash())) {
                        LOG.error("Clientlib hash for {} as {} and admin disagree - " +
                                        "likely a permission problem that results in performance problems",
                                request.getUserPrincipal(), clientlibRef);
                    }
                }

                boolean refreshNeeded = refreshForced || !hash.equals(cacheFileHash);
                // if the clientlib seems newer than the clientlib last modified, we rather regenerate things, too.
                if (null != cacheFile.getLastModified() && null != updateTimeVisitor.getLastUpdateTime() &&
                        updateTimeVisitor.getLastUpdateTime().after(cacheFile.getLastModified()))
                    refreshNeeded = true;

                if (refreshNeeded) {
                    LOG.info("prepare ''{}''...", clientlibRef);

                    Resource cacheEntry = adminResolver.getResource(cachePath);
                    if (cacheEntry != null) {
                        LOG.info("deleting to be refreshed ''{}''...", cacheEntry);
                        adminResolver.delete(cacheEntry);
                        adminResolver.commit();
                    }

                    final ProcessorContext context = new ProcessorContext(request, adminResolver, executorService,
                            getClientlibConfig().getMapClientlibURLs(), minified && clientlibConfig
                            .getUseMinifiedFiles());

                    LazyCreationService.InitializationStrategy initializer = initializationStrategy(clientlibRef,
                            encoding, hash, context);

                    Resource resource = lazyCreationService.getOrCreate(request.getResourceResolver(), cachePath,
                            LazyCreationService.IDENTITY_RETRIEVER, creationStrategy(), initializer,
                            CRUD_CACHE_FOLDER_PROPS);
                    cacheFile = new FileHandle(resource);
                }

                fileHints = getFileHints(cacheFile, element.makeLink());
            } finally {
                if (null != adminResolver) adminResolver.close();
            }
        }
        LOG.debug("Hints: {}", fileHints);
        return fileHints;
    }

    protected LazyCreationService.CreationStrategy creationStrategy() {
        return new LazyCreationService.CreationStrategy() {
            @Override
            public Resource create(ResourceResolver adminResolver, Resource parent, String name) throws
                    RepositoryException, PersistenceException {
                Resource cacheEntry = adminResolver.create(parent, name, FileHandle.CRUD_FILE_PROPS);
                adminResolver.create(cacheEntry, ResourceUtil.CONTENT_NODE, FileHandle.CRUD_CONTENT_PROPS)
                        .adaptTo(Node.class).addMixin(ResourceUtil.TYPE_TITLE);
                FileHandle cacheFile = new FileHandle(cacheEntry);
                cacheFile.storeContent(new ByteArrayInputStream("".getBytes())); // mandatory node jcr:data
                return cacheEntry;
            }
        };
    }

    protected LazyCreationService.InitializationStrategy initializationStrategy(
            final ClientlibRef clientlibRef, final String encoding, final String hash, final ProcessorContext context) {
        return new LazyCreationService.InitializationStrategy() {
            @Override
            public void initialize(ResourceResolver adminResolver, Resource cacheEntry) throws
                    RepositoryException, PersistenceException {

                try {
                    FileHandle cacheFile = new FileHandle(cacheEntry);
                    if (cacheFile.isValid()) {
                        LOG.debug("create clientlib cache content ''{}''...", cacheFile.getResource()
                                .getPath());

                        final PipedOutputStream outputStream = new PipedOutputStream();
                        InputStream inputStream = new PipedInputStream(outputStream);
                        Future<Void> result = startProcessing(clientlibRef, encoding, context, outputStream);
                        if (ENCODING_GZIP.equals(encoding)) {
                            inputStream = gzipProcessor.processContent(inputStream, context);
                        }
                        cacheFile.storeContent(inputStream);

                        ModifiableValueMap contentValues = cacheFile.getContent().adaptTo
                                (ModifiableValueMap.class);
                        contentValues.put(ResourceUtil.PROP_LAST_MODIFIED,
                                Calendar.getInstance());
                        contentValues.putAll(context.getHints());
                        contentValues.put(PROP_HASH, hash);

                        adminResolver.commit();
                        result.get(); // transport any exceptions here

                        LOG.info("clientlib cache content ''{}'' created", cacheFile.getResource()
                                .getPath());
                    } else {
                        LOG.error("can't create cache content in '{}'!", cacheFile != null ? cacheFile
                                .getResource().getPath() : "null");
                    }
                } catch (Exception e) {
                    LOG.error("Error when initializing content in " + cacheEntry + "; deleting the file", e);
                    refreshSession(adminResolver, false);
                    adminResolver.delete(cacheEntry);
                    throw new PersistenceException("" + e, e);
                }
            }
        };
    }

    /**
     * Starts the processing (generation of the embedded content) of the clientlib / -category in the background.
     */
    protected Future<Void> startProcessing(final ClientlibRef clientlibRef, String encoding,
                                           final ProcessorContext context, final OutputStream outputStream)
            throws IOException {
        final ClientlibProcessor processor = processorMap.get(clientlibRef.type);

        Future<Void> callable = context.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ResourceResolver processingAdminResolver = null;
                try {
                    processingAdminResolver = createAdministrativeResolver();
                    // retrieve clientlibs again since we need another resolver (not threadsafe)
                    ClientlibElement adminElement = resolve(clientlibRef, processingAdminResolver);
                    ProcessingVisitor visitor = new ProcessingVisitor(adminElement, DefaultClientlibService.this,
                            outputStream, processor, context);
                    visitor.execute();
                } finally {
                    IOUtils.closeQuietly(outputStream);
                    if (null != processingAdminResolver) processingAdminResolver.close();
                }
                return null;
            }
        });

        return callable;
    }

    protected ClientlibInfo getFileHints(final FileHandle file, ClientlibLink link) {
        if (file.isValid()) {
            ClientlibInfo hints = new ClientlibInfo();
            ValueMap contentValues = file.getContent().adaptTo(ValueMap.class);
            hints.lastModified = contentValues.get(com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED,
                    Calendar.class);
            hints.mimeType = contentValues.get(com.composum.sling.core.util.ResourceUtil.PROP_MIME_TYPE, String.class);
            hints.encoding = contentValues.get(com.composum.sling.core.util.ResourceUtil.PROP_ENCODING, String.class);
            hints.hash = contentValues.get(PROP_HASH, String.class);
            hints.size = file.getSize();
            hints.link = link.withHash(hints.hash);
            return hints;
        }
        return null;
    }

    @Override
    public void deliverContent(ResourceResolver resolver, ClientlibRef clientlibRef, boolean minified, OutputStream
            outputStream, String encoding) throws IOException, RepositoryException {
        encoding = adjustEncoding(encoding);
        String cachePath = getCachePath(clientlibRef, minified, encoding);
        Resource resource = lazyCreationService.waitForInitialization(resolver, cachePath);
        FileHandle file = new FileHandle(resource);
        if (file.isValid()) {
            InputStream content = file.getStream();
            if (content != null) {
                try {
                    IOUtils.copy(content, outputStream);
                } finally {
                    IOUtils.closeQuietly(content);
                }
            }
        } else { // abort the request rather than delivering faulty data
            throw new FileNotFoundException("No cached file found for " + clientlibRef);
        }
    }

    /**
     * Uses the category if given, or the single clientlib otherwise.
     *
     * @param ref what we create the cache for
     * @return the name of the file that is used for caching this resource. Not null.
     */
    protected String getCachePath(ClientlibRef ref, boolean minified, String encoding) {
        String cacheRoot = clientlibConfig.getCacheRoot();
        String cacheKey;
        cacheKey = ref.isCategory() ? "/" + CATEGORYCACHE + "/" + ref.category : ref.path;
        if (StringUtils.isNotBlank(encoding)) {
            cacheKey += '.' + encoding.trim();
        }
        if (minified && clientlibConfig.getUseMinifiedFiles()) cacheKey = cacheKey + ".min";
        cacheKey = cacheKey + "." + ref.type.name();
        return cacheRoot + cacheKey;
    }

    protected String adjustEncoding(String encoding) {
        if (ENCODING_GZIP.equals(encoding) && !clientlibConfig.getGzipEnabled()) {
            encoding = null;
        }
        return encoding;
    }

    /**
     * Resets unmodified resources to the currently saved state.
     */
    protected void refreshSession(ResourceResolver resolver, boolean logwarning) {
        try {
            resolver.adaptTo(Session.class).refresh(true);
        } catch (RepositoryException rex) {
            if (logwarning) LOG.warn(rex.getMessage(), rex);
        }
    }

    /**
     * XPath Query that matches all clientlibs.
     */
    protected static final String QUERY_CLIENTLIBS = "/jcr:root/(apps|libs)//*[sling:resourceType='composum/nodes/commons/clientlib']";

    /**
     * Xpath Query suffix for a query that matches all clientlib folders referencing other stuff: {@value #QUERY_SUFFIX_REFERENCERS}.
     */
    protected static final String QUERY_SUFFIX_REFERENCERS = "[embed or depends]";

    @Override
    @Nullable
    public String verifyClientlibPermissions(@Nullable Clientlib.Type requestedType, @Nullable ResourceResolver userResolver, boolean onlyErrors) {
        String querySuffix = requestedType != null ? "/" + requestedType.name() + "//*" : "//*";
        String onlyClientlibQuerySuffix = requestedType != null ? "/" + requestedType.name() + "/.." : "";

        StringBuilder buf = new StringBuilder();
        ResourceResolver impersonationResolver = userResolver;
        ResourceResolver administrativeResolver = null;
        try {
            if (impersonationResolver == null)
                impersonationResolver = resolverFactory.getResourceResolver(null);
            administrativeResolver = createAdministrativeResolver();
            List<String> unreachablePaths = new ArrayList<>();
            Iterator<Resource> it = administrativeResolver.findResources(QUERY_CLIENTLIBS + onlyClientlibQuerySuffix + " order by path", Query.XPATH);

            // Check clientlibraries themselves and their categories
            Set<String> categoriesWithReachableClientlibs = new HashSet<>();
            Set<String> categoriesWithUnreachableClientlibs = new HashSet<>();
            Set<String> allCategories = new HashSet<>();
            while (it.hasNext()) {
                Resource clientlibElement = it.next();
                List<String> categories = Arrays.asList(ResourceHandle.use(clientlibElement).getProperty(PROP_CATEGORY, new String[0]));
                allCategories.addAll(categories);
                if (impersonationResolver.getResource(clientlibElement.getPath()) == null) {
                    unreachablePaths.add(clientlibElement.getPath());
                    categoriesWithUnreachableClientlibs.addAll(categories);
                } else {
                    categoriesWithReachableClientlibs.addAll(categories);
                }
            }
            Collection troubledCategories = CollectionUtils.intersection(categoriesWithReachableClientlibs, categoriesWithUnreachableClientlibs);
            if (!troubledCategories.isEmpty())
                buf.append("ERROR: Categories with both readable AND unreadable elements: ").append(troubledCategories).append("\n");

            // look for unreadable elements of readable client libraries -> error
            it = administrativeResolver.findResources(QUERY_CLIENTLIBS + querySuffix + " order by path", Query.XPATH);
            while (it.hasNext()) {
                Resource clientlibElement = it.next();
                if (impersonationResolver.getResource(clientlibElement.getPath()) == null) {
                    if (!isReachableFrom(unreachablePaths, clientlibElement.getPath())) {
                        // clientlibs are already there -> this element is surprisingly unreadable.
                        buf.append("ERROR: unreadable element of readable client library: ")
                                .append(clientlibElement.getPath()).append("\n");
                    }
                    unreachablePaths.add(clientlibElement.getPath());
                }
            }
            unreachablePaths = removeChildren(unreachablePaths);
            if (!onlyErrors && !unreachablePaths.isEmpty())
                buf.insert(0, "INFO: Unreadable for this user: " + unreachablePaths + "\n");

            // look for unreadable references of readable elements
            it = administrativeResolver.findResources(QUERY_CLIENTLIBS + querySuffix + QUERY_SUFFIX_REFERENCERS + " order by path", Query.XPATH);
            while (it.hasNext()) {
                Resource clientlibElement = it.next();
                if (isReachableFrom(unreachablePaths, clientlibElement.getPath()))
                    continue;
                Resource clientlibFolderResource = clientlibElement;
                while (!clientlibFolderResource.getParent().isResourceType(RESOURCE_TYPE)) {
                    clientlibFolderResource = clientlibFolderResource.getParent();
                }
                Type type = null;
                try {
                    type = Type.valueOf(clientlibFolderResource.getName());
                } catch (IllegalArgumentException e) { // very unusual case - folder name other than type?
                    buf.append("WARN: Cannot recognize type of ").append(clientlibElement.getPath()).append("\n");
                }
                if (type != null) {
                    ClientlibResourceFolder resourceFolder = new ClientlibResourceFolder(type, clientlibElement);
                    for (ClientlibRef ref : resourceFolder.getDependencies()) {
                        verifyRef(resourceFolder, ref, administrativeResolver, impersonationResolver, allCategories, buf);
                    }
                    for (ClientlibRef ref : resourceFolder.getEmbedded()) {
                        verifyRef(resourceFolder, ref, administrativeResolver, impersonationResolver, allCategories, buf);
                    }
                    for (ClientlibRef ref : (Collection<ClientlibRef>) CollectionUtils.intersection(resourceFolder.getDependencies(), resourceFolder.getEmbedded())) {
                        buf.append("ERROR: both a dependency and embedded: ").append(ref).append(" in ").append(resourceFolder).append("\n");
                    }
                }
            }
        } catch (LoginException e) {
            buf.append("Cannot create anonymous or administrative resolver - " + e);
            LOG.error("Cannot create anonymous or administrative resolver - " + e, e);
        } catch (Exception e) {
            LOG.error("Error checking clientlibs", e);
        } finally {
            if (null != administrativeResolver) administrativeResolver.close();
            if (userResolver == null && null != impersonationResolver)
                impersonationResolver.close(); // else it's just resolver coming from outside
        }
        return buf.length() == 0 ? null : buf.toString();
    }

    @Override
    public void clearCache(ResourceResolver resolver) throws PersistenceException {
        LOG.info("Clear cache requested.");
        String cacheRootPath = clientlibConfig.getCacheRoot();
        Resource cacheRoot = resolver.getResource(cacheRootPath);
        List<String> subpaths = new ArrayList<>();
        subpaths.addAll(Arrays.asList(resolver.getSearchPath()));
        subpaths.add(CATEGORYCACHE);
        for (String child : subpaths) {
            Resource childResource = cacheRoot.getChild(StringUtils.removeStart(child, "/"));
            if (childResource != null) {
                if (StringUtils.countMatches(childResource.getPath(), "/") < 3) // safety check
                    throw new IllegalArgumentException("Suspicious path for clientlib cache to delete: " + childResource.getPath());
                LOG.info("Deleting {}", childResource.getPath());
                resolver.delete(childResource);
            }
        }
    }

    /** For each path contained here, remove all paths that are children of it, thus removing consequential errors. */
    protected List<String> removeChildren(List<String> unreachablePaths) {
        Collections.sort(unreachablePaths); // ancestors appear before children
        List<String> result = new ArrayList<>();
        for (String path : unreachablePaths) {
            if (!isReachableFrom(result, path))
                result.add(path);
        }
        return result;
    }

    protected boolean isReachableFrom(List<String> paths, String path) {
        for (String ancestorPath : paths)
            if (isAncestorOrSelf(ancestorPath, path))
                return true;
        return false;
    }

    /** Returns true if the parent is an {ancestor} of the {resource} (and both are not null, of course. */
    protected boolean isAncestorOrSelf(@Nullable String parentPath, @Nullable String childPath) {
        return parentPath != null && childPath != null && (
                parentPath.equals(childPath) ||
                        childPath.startsWith(parentPath + "/")
        );
    }

    private void verifyRef(ClientlibResourceFolder resourceFolder, ClientlibRef ref, ResourceResolver administrativeResolver, ResourceResolver userResolver, Set<String> allCategories, StringBuilder buf) {
        if (ref.isExternalUri()) return;
        if (ref.isCategory()) {
            if (!allCategories.contains(ref.category)) {
                if (ref.optional)
                    buf.append("INFO: empty optional category ").append(ref).append(" referenced\n");
                else
                    buf.append("WARN: empty mandatory category ").append(ref).append(" referenced\n");
            }
            return;
        }
        Resource resourceAsAdmin = retrieveResource(ref.path, administrativeResolver);
        if (resourceAsAdmin != null) {
            Resource resourceAsUser = retrieveResource(ref.path, userResolver);
            if (resourceAsUser == null) {
                buf.append("ERROR: unreadable reference ").append(resourceAsAdmin.getPath())
                        .append(" of readable client library resource folder ").append(resourceFolder.resource.getPath())
                        .append("\n");
            } else if (!resourceAsAdmin.getPath().equals(resourceAsUser.getPath())) {
                buf.append("ERROR: Permission problem: resource different for admin and anonymous for ")
                        .append(ref.toString())
                        .append(" : ").append(resourceAsAdmin.getPath())
                        .append(" vs. ").append(resourceAsUser.getPath())
                        .append("\n");
            } else if (new FileHandle(resourceAsAdmin).isValid() && !new FileHandle(resourceAsUser).isValid()) {
                buf.append("ERROR: Content resource not readable: ").append(resourceAsAdmin.getPath()).append("\n");
            }
        } else if (!resourceFolder.getOptional()) {
            buf.append("ERROR: can't find element ").append(ref.path)
                    .append(" of resource folder ").append(resourceFolder.resource.getPath()).append("\n");
        }
    }

}
