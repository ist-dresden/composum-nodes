package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.processor.*;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.clientlibs.handle.Clientlib.PROP_CATEGORY;
import static com.composum.sling.clientlibs.handle.Clientlib.PROP_ORDER;
import static com.composum.sling.clientlibs.handle.Clientlib.RESOURCE_TYPE;
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
    public static final String PROP_HASH = ResourceUtil.PROP_DESCRIPTION; // TODO abused for now

    public static final Map<String, Object> CRUD_CACHE_FOLDER_PROPS;

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

    /**
     * Cache (String, Pair<Long, List<String>>) that maps categories to a pair of the query time and the resulting list
     * of paths to client libraries with that path. We set an arbitrary limit of 100 since otherwise we'd be open to a
     * DOS attack by retrieving random categories.
     */
    protected final LRUMap /*String, Pair<Long, List<String>>*/ categoryToPathCache = new LRUMap(100);

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
        if (ref.optional) LOG.debug("Could not resolve {}", ref);
        else LOG.warn("Could not resolve {}", ref);
        return null;
    }

    /** For files we use the correct sibling wrt. {@link ClientlibConfiguration#getUseMinifiedFiles()}. */
    protected Resource minificationVariant(Resource resource) {
        if (getClientlibConfig().getUseMinifiedFiles()) {
            return getMinifiedSibling(resource);
        }
        return resource;
    }

    /** Retrieve a resource from a resolver; if we don't find it, we try to retrieve the (un)minified sibling. */
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
    public Resource getMinifiedSibling(Resource resource) {
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
        for (String searchPathElement : resolver.getSearchPath()) {
            String xpath = "/jcr:root" + searchPathElement.replaceFirst("/+$", "") + "//element(*," +
                    TYPE_SLING_FOLDER + ")" + "[@" + PROP_RESOURCE_TYPE + "='" + RESOURCE_TYPE + "'" + " and @" +
                    PROP_CATEGORY + "='" + category + "']";
            for (Iterator<Resource> iterator = resolver.findResources(xpath, Query.XPATH); iterator.hasNext(); ) {
                Resource foundResource = iterator.next();
                ResourceHandle handle = ResourceHandle.use(foundResource);
                String libPath = handle.getPath();
                String key = libPath.substring(libPath.indexOf(searchPathElement) + searchPathElement.length());
                if (!foundlibs.contains(key)) { // first wins - e.g. /apps shadows /libs
                    foundlibs.add(key);
                    resources.add(foundResource);
                }
            }
        }
        Collections.sort(resources, orderResourceComparator);
        return resources;
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
    public void renderClientlibLinks(ClientlibElement clientlib, Writer writer, RendererContext context) throws
            IOException, RepositoryException {
        final Clientlib.Type type = clientlib.getType();
        final ClientlibRenderer renderer = rendererMap.get(type);
        if (renderer != null) {
            renderer.renderClientlibLinks(clientlib, writer, context);
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
            minified, String encoding, boolean forceRefreshCache, String requestedHash, long ifModifiedSince) throws
            IOException, RepositoryException {
        ClientlibElement element = resolve(clientlibRef, request.getResourceResolver());
        if (null == element) {
            LOG.error("No client libraries found for {}", clientlibRef);
            throw new FileNotFoundException("No client libraries for " + clientlibRef);
        }

        encoding = adjustEncoding(encoding);
        String cachePath = getCachePath(clientlibRef, minified, encoding);

        FileHandle cacheFile = new FileHandle(request.getResourceResolver().getResource(cachePath));
        ClientlibInfo fileHints = getFileHints(cacheFile, element.makeLink());

        boolean ifModifiedIsOlder = (ifModifiedSince > 0) && (null != cacheFile.getLastModified()) &&
                (ifModifiedSince < cacheFile.getLastModified().getTimeInMillis());
        boolean cacheAssumedRecent = (null != fileHints) && (null != fileHints.hash) &&
                (fileHints.hash.equals(requestedHash) || ifModifiedIsOlder);
        boolean refreshForced = forceRefreshCache || null == fileHints || (null == cacheFile.getLastModified());

        if (refreshForced || !cacheAssumedRecent) {
            ResourceResolver adminResolver = null;
            SequencerService.Token token = sequencer.acquire(cachePath);
            try {
                // recheck since the admin resolver could change things, and when acquiring the lock the
                // cache file might have been recreated.
                adminResolver = createAdministrativeResolver();
                element = resolve(clientlibRef, adminResolver);
                cacheFile = new FileHandle(adminResolver.getResource(cachePath));

                UpdateTimeVisitor updateTimeVisitor = new UpdateTimeVisitor(element, this, request
                        .getResourceResolver());
                updateTimeVisitor.execute();
                String hash = updateTimeVisitor.getHash();
                String cacheFileHash = cacheFile.getContent().getProperty(PROP_HASH);

                boolean refreshNeeded = refreshForced || !hash.equals(cacheFileHash);
                // if the clientlib seems newer than the clientlib last modified, we rather regenerate things, too.
                if (null != cacheFile.getLastModified() && null != updateTimeVisitor.getLastUpdateTime() &&
                        updateTimeVisitor.getLastUpdateTime().after(cacheFile.getLastModified()))
                    refreshNeeded = true;

                if (refreshNeeded) {
                    LOG.info("prepare ''{}''...", clientlibRef);

                    final ProcessorContext context = new ProcessorContext(request, adminResolver, executorService,
                            getClientlibConfig().getMapClientlibURLs(), minified && clientlibConfig
                            .getUseMinifiedFiles());

                    Resource cacheEntry = adminResolver.getResource(cachePath);
                    if (cacheEntry != null) {
                        adminResolver.delete(cacheEntry);
                        adminResolver.commit();
                    }

                    String[] separated = com.composum.sling.core.util.ResourceUtil.splitPathAndName(cachePath);
                    Resource parent = giveParent(adminResolver, separated[0]);
                    refreshSession(adminResolver, false);
                    cacheEntry = adminResolver.create(parent, separated[1], FileHandle.CRUD_FILE_PROPS);
                    adminResolver.create(cacheEntry, ResourceUtil.CONTENT_NODE, FileHandle.CRUD_CONTENT_PROPS)
                            .adaptTo(Node.class).addMixin("mix:title"); // TODO maybe use more sensible mixin


                    cacheFile = new FileHandle(cacheEntry);
                    if (cacheFile.isValid()) {
                        LOG.debug("create clientlib cache content ''{}''...", cacheFile.getResource().getPath());

                        InputStream inputStream = startProcessing(clientlibRef, encoding, context);
                        cacheFile.storeContent(inputStream);

                        ModifiableValueMap contentValues = cacheFile.getContent().adaptTo(ModifiableValueMap.class);
                        contentValues.put(com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED, Calendar
                                .getInstance());
                        contentValues.putAll(context.getHints());
                        contentValues.put(PROP_HASH, hash);

                        adminResolver.commit();

                        LOG.info("clientlib cache content ''{}'' created", cacheFile.getResource().getPath());
                    } else {
                        LOG.error("can't create cache content in '{}'!", cacheFile != null ? cacheFile.getResource()
                                .getPath() : "null");
                    }
                }

                fileHints = getFileHints(cacheFile, element.makeLink());
            } finally {
                sequencer.release(token);
                if (null != adminResolver) adminResolver.close();
            }
        }
        LOG.debug("Hints: {}", fileHints);
        return fileHints;
    }


    /** Starts the processing (generation of the embedded content) of the clientlib / -category in the background. */
    protected InputStream startProcessing(final ClientlibRef clientlibRef, String encoding, final ProcessorContext
            context) throws IOException {
        final PipedOutputStream outputStream = new PipedOutputStream();
        InputStream inputStream = new PipedInputStream(outputStream);
        final ClientlibProcessor processor = processorMap.get(clientlibRef.type);

        context.execute(new Runnable() {
            @Override
            public void run() {
                ResourceResolver processingAdminResolver = null;
                try {
                    processingAdminResolver = createAdministrativeResolver();
                    // retrieve clientlibs again since we need another resolver (not threadsafe)
                    // and need to retrieve the clientlibs again with an admin resolver, anyway
                    // to ensure they are independent of the login.
                    ClientlibElement adminElement = resolve(clientlibRef, processingAdminResolver);
                    ProcessingVisitor visitor = new ProcessingVisitor(adminElement, DefaultClientlibService.this,
                            outputStream, processor, context);
                    visitor.execute();
                } catch (IOException | RepositoryException | RuntimeException ex) {
                    LOG.error(ex.getMessage(), ex);
                } finally {
                    IOUtils.closeQuietly(outputStream);
                    if (null != processingAdminResolver) processingAdminResolver.close();
                }
            }
        });

        if (ENCODING_GZIP.equals(encoding)) {
            inputStream = gzipProcessor.processContent(inputStream, context);
        }
        return inputStream;
    }

    protected Resource giveParent(ResourceResolver resolver, String path) {
        Resource resource = null;
        SequencerService.Token token = sequencer.acquire(path);
        try {
            refreshSession(resolver, true);
            resource = resolver.getResource(path);
            if (resource == null) {
                String[] separated = com.composum.sling.core.util.ResourceUtil.splitPathAndName(path);
                Resource parent = "".equals(separated[0]) ? resolver.getResource("/") : giveParent(resolver,
                        separated[0]);
                try {
                    refreshSession(resolver, true);
                    resource = resolver.create(parent, separated[1], CRUD_CACHE_FOLDER_PROPS);
                    resolver.commit();
                } catch (PersistenceException pex) {
                    // catch it and hope that the parent is available. necessary to continue on transaction isolation
                    // problems
                    LOG.error("clientlib giveParent('{}'): {}", path, pex);
                }
            }
        } finally {
            sequencer.release(token);
        }
        return resource;
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
        FileHandle file = new FileHandle(resolver.getResource(cachePath));
        if (!file.isValid()) { // this normally means there is another thread recreating the cachefile - wait for it
            SequencerService.Token token = sequencer.acquire(cachePath);
            sequencer.release(token);
            refreshSession(resolver, false);
            file = new FileHandle(resolver.getResource(cachePath));
        }
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
        cacheKey = ref.isCategory() ? "/categorycache/" + ref.category : ref.path;
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

    /** Resets unmodified resources to the currently saved state. */
    protected void refreshSession(ResourceResolver resolver, boolean logwarning) {
        try {
            resolver.adaptTo(Session.class).refresh(true);
        } catch (RepositoryException rex) {
            if (logwarning) LOG.warn(rex.getMessage(), rex);
        }
    }

}
