package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.ProcessorContext;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Clientlib {

    private static final Logger LOG = LoggerFactory.getLogger(Clientlib.class);

    public static final String RESOURCE_TYPE = "composum/nodes/commons/clientlib";

    public enum Type {link, css, js, img}

    public static final String PROP_EXPANDED = "expanded";
    public static final String PROP_OPTIONAL = "optional";
    public static final String PROP_DEPENDS = "depends";
    public static final String PROP_EMBED = "embed";

    public static final String MINIFIED_SELECTOR = ".min";
    public static final Pattern UNMINIFIED_PATTERN = Pattern.compile("^(.+/)([^/]+)(\\.min)?(\\.[^.]+)$");
    public static final Pattern MINIFIED_PATTERN = Pattern.compile("^(.+/)([^/]+)(\\.min)(\\.[^.]+)?$");

    public static final String[] LINK_PROPERTIES = new String[]{ClientlibKey.PROP_REL};

    protected final SlingHttpServletRequest request;
    protected final ResourceResolver resolver;
    protected final ResourceHandle resource;
    protected final ResourceHandle definition;

    protected final String path;
    protected final ClientlibRef clientlibRef;

    private transient Calendar lastModified;

    public Clientlib(SlingHttpServletRequest request, String path, Type type) {
        this.request = request;
        this.resolver = request.getResourceResolver();
        this.path = path;
        clientlibRef = new ClientlibRef(type, path, false, false);
        this.resource = ResourceHandle.use(retrieveResource(clientlibRef.keyPath));
        this.definition = retrieveDefinition();
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    protected ResourceHandle retrieveDefinition() {
        String path = clientlibRef.keyPath + "/" + clientlibRef.type;
        Resource resource = retrieveResource(path);
        return ResourceHandle.use(resource);
    }

    protected Resource retrieveResource(String path) {
        Resource resource = null;
        if (!path.startsWith("/")) {
            String[] searchPath = resolver.getSearchPath();
            for (int i = 0; resource == null && i < searchPath.length; i++) {
                String absolutePath = searchPath[i] + path;
                resource = resolver.getResource(absolutePath);
            }
        } else {
            resource = resolver.getResource(path);
        }
        if (LOG.isDebugEnabled()) {
            if (resource != null) {
                LOG.debug("retrieveResource.use: '" + path + "': " + resource);
            } else {
                LOG.debug("retrieveResource.failed: '" + path + "'");
            }
        }
        return resource;
    }

    public boolean isValid() {
        return resource.isResourceType(RESOURCE_TYPE) && definition.isValid();
    }

    public String getPath(String path) {
        Resource resource = retrieveResource(path);
        return resource != null ? resource.getPath() : "";
    }

    public String getPath() {
        return path + "." + clientlibRef.type.name();
    }

    public Type getType() {
        return clientlibRef.type;
    }

    public Calendar getLastModified() {
        if (lastModified == null) {
            lastModified = getLastModified(definition, null);
        }
        return lastModified;
    }

    //
    // Clientlib modification date
    //

    protected Calendar getLastModified(ResourceHandle resource, Calendar lastModified) {
        if (resource.isValid()) {
            if (isFile(resource)) {
                lastModified = getLastModified(new FileHandle(resource), lastModified);
            } else {
                for (String embedRule : resource.getProperty(PROP_EMBED, new String[0])) {
                    embedRule = embedRule.trim();
                    ClientlibRef reference = new ClientlibRef(this.clientlibRef, embedRule, false, false);
                    Resource target = retrieveResource(reference.keyPath);
                    if (target != null) {
                        if (target.isResourceType(RESOURCE_TYPE)) {
                            Clientlib embedded = new Clientlib(request, reference.keyPath, reference.type);
                            lastModified = getLastModified(embedded.getLastModified(), lastModified);
                        } else {
                            lastModified = getLastModified(new FileHandle(target), lastModified);
                        }
                    }
                }
                for (Resource child : resource.getChildren()) {
                    lastModified = getLastModified(ResourceHandle.use(child), lastModified);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(resource.getPath() + ".lastModified: " + (lastModified != null
                        ? new SimpleDateFormat("yyyy-mm-dd.HH:MM:ss").format(lastModified.getTime())
                        : "<null>"));
            }
        }
        return lastModified;
    }

    protected Calendar getLastModified(FileHandle file, Calendar lastModified) {
        return getLastModified(file.getLastModified(), lastModified);
    }

    protected Calendar getLastModified(Calendar value, Calendar lastModified) {
        if (lastModified == null || lastModified.before(value)) {
            lastModified = value;
        }
        return lastModified;
    }

    //
    // retrieve Clientlib links
    //

    public List<ClientlibLink> getLinks(RendererContext context, boolean expanded) {
        List<ClientlibLink> links = new ArrayList<>();
        getLinks(links, context, expanded, null, false);
        return links;
    }

    public boolean getLinks(List<ClientlibLink> links, RendererContext context,
                            boolean expanded, Boolean depends, boolean optional) {
        boolean hasEmbeddedContent = false;
        if (definition.isValid()) {
            expanded = definition.getProperty(PROP_EXPANDED, expanded);
            optional = definition.getProperty(PROP_OPTIONAL, optional);
            ClientlibRef reference = new ClientlibRef(clientlibRef, depends, optional);
            hasEmbeddedContent = getLinks(links, context, expanded, depends, reference, definition);
            if (!expanded && (depends == null || depends) && hasEmbeddedContent) {
                if (!context.isClientlibRendered(clientlibRef)) {
                    ClientlibLink link = new ClientlibLink(clientlibRef, resource, true);
                    context.registerClientlibLink(link);
                    links.add(link);
                } else {
                    if (depends == null) {
                        logDuplicate(clientlibRef);
                    }
                }
            }
        }
        return hasEmbeddedContent;
    }

    protected boolean getLinks(List<ClientlibLink> links, RendererContext context,
                               boolean expanded, Boolean depends,
                               ClientlibRef reference, ResourceHandle resource) {
        boolean hasEmbeddedContent = false;
        if (resource.isValid()) {
            Map<String, String> properties = new HashMap<>();
            for (String key : LINK_PROPERTIES) {
                String value = resource.getProperty(key, (String) null);
                if (value != null) properties.put(key, value);
            }
            if (isFile(resource)) {
                addFileToList(links, context, expanded || (depends != null && depends),
                        reference, properties, resource);
            } else {
                boolean optional = resource.getProperty(PROP_OPTIONAL, reference.optional);
                for (String dependsRule : resource.getProperty(PROP_DEPENDS, new String[0])) {
                    dependsRule = dependsRule.trim();
                    ClientlibRef clientlibRef = new ClientlibRef(reference, dependsRule, true, optional);
                    getClientlibLink(links, context, expanded, true, clientlibRef, properties);
                }
                for (String embedRule : resource.getProperty(PROP_EMBED, new String[0])) {
                    embedRule = embedRule.trim();
                    ClientlibRef clientlibRef = new ClientlibRef(reference, embedRule, optional);
                    if (getClientlibLink(links, context, expanded, false, clientlibRef, properties)) {
                        hasEmbeddedContent = true;
                    }
                }
                for (Resource child : resource.getChildren()) {
                    hasEmbeddedContent =
                            getLinks(links, context, expanded, depends, reference, ResourceHandle.use(child))
                                    || hasEmbeddedContent;
                }
            }
        }
        return hasEmbeddedContent;
    }

    protected boolean getClientlibLink(List<ClientlibLink> links, RendererContext context,
                                       boolean expanded, Boolean depends,
                                       ClientlibRef reference, Map<String, String> properties) {
        String path = reference.keyPath;
        Resource target = retrieveResource(path);
        if (target != null) {
            if (target.isResourceType(RESOURCE_TYPE)) {
                Clientlib embedded = new Clientlib(request, path, clientlibRef.type);
                return embedded.getLinks(links, context, expanded, depends, reference.optional);
            } else {
                addFileToList(links, context, expanded || (depends != null && depends), reference, properties, target);
                return true;
            }
        } else {
            logNotAvailable(resource, path, reference.optional);
        }
        return false;
    }

    protected void addFileToList(List<ClientlibLink> links, RendererContext context, boolean depends,
                                 ClientlibRef reference, Map<String, String> properties, Resource target) {
        if (!context.isClientlibRendered(reference)) {
            if (context.useMinifiedFiles()) {
                target = getMinifiedSibling(target);
            }
            FileHandle file = new FileHandle(target);
            if (file.isValid()) {
                ClientlibLink link = new ClientlibLink(reference, target, properties, false /* already done */);
                context.registerClientlibLink(link);
                if (depends) {
                    links.add(link);
                }
            } else {
                logNotAvailable(resource, file.getPath(), reference.optional);
            }
        } else {
            if (!depends) {
                logDuplicate(reference);
            }
        }
    }

    //
    // generate Clientlib content
    //

    public List<ClientlibLink> processContent(OutputStream output,
                                              ClientlibProcessor processor, ProcessorContext context)
            throws IOException, RepositoryException {
        List<ClientlibLink> contentSet = new ArrayList<>();
        processContent(output, processor, context, definition, false, contentSet);
        output.close();
        return contentSet;
    }

    protected void processContent(OutputStream output,
                                  ClientlibProcessor processor, ProcessorContext context,
                                  ResourceHandle resource, boolean optional, List<ClientlibLink> contentSet)
            throws RepositoryException, IOException {
        if (resource.isValid()) {
            if (!resource.getProperty(PROP_EXPANDED, false)) {
                if (!optional) {
                    optional = resource.getProperty(PROP_OPTIONAL, false);
                }
                for (String refRule : resource.getProperty(PROP_EMBED, new String[0])) {
                    refRule = refRule.trim();
                    ClientlibRef ref = new ClientlibRef(clientlibRef.type, refRule, false, optional);
                    Resource target = retrieveResource(ref.keyPath);
                    if (target != null) {
                        if (target.isResourceType(RESOURCE_TYPE)) {
                            ResourceHandle handle = ResourceHandle.use(target);
                            Clientlib embedded = new Clientlib(request, ref.keyPath, ref.type);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("embedded clientlib: " + handle.getPath());
                            }
                            embedded.processContent(output,
                                    processor, context, embedded.definition, optional, contentSet);
                        } else {
                            processFile(output, processor, context, target, optional, contentSet);
                        }
                    } else {
                        logNotAvailable(resource, ref.keyPath, optional);
                    }
                }
                if (isFile(resource)) {
                    processFile(output, processor, context, resource, optional, contentSet);
                }
            }
            for (Resource child : resource.getChildren()) {
                ResourceHandle handle = ResourceHandle.use(child);
                processContent(output, processor, context, handle, optional, contentSet);
            }
        } else {
            logNotAvailable(resource, "[clientlib]", optional);
        }
    }

    protected void processFile(OutputStream output,
                               ClientlibProcessor processor, ProcessorContext context,
                               Resource resource, boolean optional, List<ClientlibLink> contentSet)
            throws RepositoryException, IOException {
        if (context.useMinifiedFiles()) {
            resource = getMinifiedSibling(resource);
        }
        FileHandle file = new FileHandle(resource);
        InputStream content = file.getStream();
        if (content != null) {
            try {
                if (processor != null) {
                    content = processor.processContent(this, content, context);
                }
                IOUtils.copy(content, output);
                output.write('\n');
                output.write('\n');
                output.flush();
                ClientlibLink link = new ClientlibLink(clientlibRef.type, resource, false /* already done */);
                contentSet.add(link);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("embedded file: " + resource.getPath());
                }
            } finally {
                content.close();
            }
        } else {
            logNotAvailable(resource, "[content]", optional);
        }
    }

    //
    //
    //

    public static String getMinifiedSibling(String path) {
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

    public static String getUnminifiedSibling(String path) {
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

    public static Resource getMinifiedSibling(Resource resource) {
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

    protected void logDuplicate(ClientlibRef reference) {
        String message = "Clientlib entry '" + reference + "' of '"
                + getPath() + "' already embedded - ignored here.";
        if (LOG.isDebugEnabled()) {
            LOG.warn(message, new RuntimeException());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(message);
            }
        }
    }

    protected void logNotAvailable(Resource resource, String reference, boolean optional) {
        if (!optional) {
            String message = "Clientlib entry '" + reference + "' of '" + resource.getPath() + "' not available.";
            if (LOG.isDebugEnabled()) {
                LOG.warn(message, new RuntimeException());
            } else {
                LOG.warn(message);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clientlib entry '" + reference + "' of '"
                        + resource.getPath() + "' not available but optional.");
            }
        }
    }

    public static boolean isFile(Resource resource) {
        return resource.isResourceType(ResourceUtil.TYPE_FILE) ||
                resource.isResourceType(ResourceUtil.TYPE_LINKED_FILE);
    }
}
