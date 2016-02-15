package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.clientlibs.processor.CssProcessor;
import com.composum.sling.clientlibs.processor.GzipProcessor;
import com.composum.sling.clientlibs.processor.JavascriptProcessor;
import com.composum.sling.clientlibs.processor.LinkRenderer;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import java.io.Writer;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

@Component(
        label = "Clientlib Service",
        description = "Delivers clienlib content bundled and compressed.",
        immediate = true,
        metatype = true
)
@Service
public class DefaultClientlibService implements ClientlibService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientlibService.class);

    public static final String GZIP_ENABLED = "gzip.enabled";
    @Property(
            name = GZIP_ENABLED,
            label = "GZip enabled",
            description = "if 'true' the content is zippend if possible",
            boolValue = false
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

    public static final Map<String, Object> CRUD_CACHE_FOLDER_PROPS;

    static {
        CRUD_CACHE_FOLDER_PROPS = new HashMap<>();
        CRUD_CACHE_FOLDER_PROPS.put(ResourceUtil.PROP_PRIMARY_TYPE, "sling:Folder");
    }

    @Reference
    ResourceResolverFactory resolverFactory;

    @Reference
    protected JavascriptProcessor javascriptProcessor;

    @Reference
    protected CssProcessor cssProcessor;

    @Reference
    protected LinkRenderer linkRenderer;

    @Reference
    protected GzipProcessor gzipProcessor;

    protected Map<Clientlib.Type, ClientlibRenderer> rendererMap;
    protected Map<Clientlib.Type, ClientlibProcessor> processorMap;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties, Writer writer)
            throws IOException {
        Clientlib.Type type = clientlib.getType();
        ClientlibRenderer renderer = rendererMap.get(type);
        if (renderer != null) {
            renderer.renderClientlibLinks(clientlib, properties, writer);
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
        FileHandle file = getCachedFile(clientlib, encoding);
        if (file != null && file.isValid()) {
            ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
            resolver.delete(file.getResource());
            resolver.commit();
        }
    }

    @Override
    public void deliverContent(final Clientlib clientlib, Writer writer, String encoding)
            throws IOException, RepositoryException {
        encoding = adjustEncoding(encoding);
        FileHandle file = getCachedFile(clientlib, encoding);
        if (file != null && file.isValid()) {
            InputStream content = file.getStream();
            if (content != null) {
                IOUtils.copy(content, writer);
            }
        }
    }

    @Override
    public Map<String, Object> prepareContent(final Clientlib clientlib, String encoding)
            throws IOException, RepositoryException, LoginException {
        encoding = adjustEncoding(encoding);
        final Map<String, Object> hints = new HashMap<>();
        FileHandle file = getCachedFile(clientlib, encoding);
        if (file == null || !file.isValid()) {
            Clientlib.Type type = clientlib.getType();
            final ClientlibProcessor processor = processorMap.get(type);
            ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
            String path = getCachePath(clientlib, encoding);
            Resource cacheEntry = resolver.getResource(path);
            if (cacheEntry != null) {
                resolver.delete(cacheEntry);
                resolver.commit();
            }
            String[] separated = Clientlib.splitPathAndName(path);
            Resource parent = giveParent(resolver, separated[0]);
            cacheEntry = resolver.create(parent, separated[1], FileHandle.CRUD_FILE_PROPS);
            resolver.create(cacheEntry, ResourceUtil.CONTENT_NODE, FileHandle.CRUD_CONTENT_PROPS);
            file = new FileHandle(cacheEntry);
            if (file.isValid()) {
                final PipedOutputStream outputStream = new PipedOutputStream();
                InputStream inputStream = new PipedInputStream(outputStream);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            clientlib.processContent(outputStream, processor, hints);
                        } catch (IOException | RepositoryException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                }).start();
                if (ENCODING_GZIP.equals(encoding)) {
                    inputStream = gzipProcessor.processContent(clientlib, inputStream, hints);
                }
                file.storeContent(inputStream);
                ModifiableValueMap contentValues = file.getContent().adaptTo(ModifiableValueMap.class);
                contentValues.put(ResourceUtil.PROP_LAST_MODIFIED, clientlib.getLastModified());
                contentValues.putAll(hints);
                resolver.commit();
            }
        }
        if (file.isValid()) {
            ValueMap contentValues = file.getContent().adaptTo(ValueMap.class);
            hints.put(ResourceUtil.PROP_LAST_MODIFIED, contentValues.get(ResourceUtil.PROP_LAST_MODIFIED));
            hints.put(ResourceUtil.PROP_MIME_TYPE, contentValues.get(ResourceUtil.PROP_MIME_TYPE));
            hints.put(ResourceUtil.PROP_ENCODING, contentValues.get(ResourceUtil.PROP_ENCODING));
            hints.put("size", file.getSize());
        }
        return hints;
    }

    protected FileHandle getCachedFile(Clientlib clientlib, String encoding) {
        String path = getCachePath(clientlib, encoding);
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

    protected Resource giveParent(ResourceResolver resolver, String path)
            throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            String[] separated = Clientlib.splitPathAndName(path);
            Resource parent = giveParent(resolver, separated[0]);
            resource = resolver.create(parent, separated[1], CRUD_CACHE_FOLDER_PROPS);
        }
        return resource;
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        gzipEnabled = PropertiesUtil.toBoolean(properties.get(GZIP_ENABLED), false);
        cacheRoot = PropertiesUtil.toString(properties.get(CACHE_ROOT), DEFAULT_CACHE_ROOT);
        rendererMap = new HashMap<>();
        rendererMap.put(Clientlib.Type.js, javascriptProcessor);
        rendererMap.put(Clientlib.Type.css, cssProcessor);
        rendererMap.put(Clientlib.Type.link, linkRenderer);
        processorMap = new HashMap<>();
        processorMap.put(Clientlib.Type.js, javascriptProcessor);
        processorMap.put(Clientlib.Type.css, cssProcessor);
    }
}
