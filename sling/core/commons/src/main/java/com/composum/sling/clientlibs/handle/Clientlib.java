package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.ProcessorContext;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.LinkUtil;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Clientlib {

    private static final Logger LOG = LoggerFactory.getLogger(Clientlib.class);

    public static final String RESOURCE_TYPE = "composum/sling/commons/clientlib";

    public enum Type {link, css, js}

    public static final String PROP_EXPANDED = "expanded";
    public static final String PROP_OPTIONAL = "optional";
    public static final String PROP_DEPENDS = "depends";
    public static final String PROP_EMBED = "embed";
    public static final String PROP_REL = "rel";

    public class Link {

        public final String url;
        public final Map<String, String> properties;
        private transient String key;

        public Link(String url, String... props) {
            this.url = url;
            properties = new LinkedHashMap<>();
            for (int i = 0; i + 1 < props.length; i += 2) {
                if (props[i + 1] != null) {
                    properties.put(props[i], props[i + 1]);
                }
            }
        }

        public String getKey() {
            if (key == null) {
                StringBuilder builder = new StringBuilder();
                builder.append(url);
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    builder.append(";").append(entry.getKey()).append("=").append(entry.getValue());
                }
                key = builder.toString();
            }
            return key;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Link
                    && url.equals(((Link) other).url)
                    && properties.equals(((Link) other).properties);
        }

        @Override
        public int hashCode() {
            return url.hashCode() | properties.hashCode();
        }
    }

    protected final SlingHttpServletRequest request;
    protected final ResourceResolver resolver;
    protected final ResourceHandle resource;
    protected final ResourceHandle definition;

    protected final String path;
    protected final Type type;

    private transient Calendar lastModified;

    public Clientlib(SlingHttpServletRequest request, String path, Type type) {
        this.request = request;
        this.resolver = request.getResourceResolver();
        this.path = path;
        this.type = type;
        this.resource = ResourceHandle.use(retrieveResource(path));
        this.definition = retrieveDefinition();
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    protected ResourceHandle retrieveDefinition() {
        String path = this.path + "/" + type;
        Resource resource = retrieveResource(path);
        return ResourceHandle.use(resource);
    }

    protected Resource retrieveResource(String path) {
        Resource resource;
        if (!path.startsWith("/")) {
            resource = resolver.getResource("/apps/" + path);
            if (resource == null) {
                resource = resolver.getResource("/libs/" + path);
            }
        } else {
            resource = resolver.getResource(path);
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
        return path + "." + type;
    }

    public Type getType() {
        return type;
    }

    public Calendar getLastModified() {
        if (lastModified == null) {
            lastModified = getLastModified(definition, null);
        }
        return lastModified;
    }

    protected Calendar getLastModified(ResourceHandle resource, Calendar lastModified) {
        if (resource.isValid()) {
            if (isFile(resource)) {
                lastModified = getLastModified(new FileHandle(resource), lastModified);
            } else {
                String reference = resource.getProperty(PROP_EMBED, "");
                if (StringUtils.isNotBlank(reference)) {
                    Resource target = retrieveResource(reference);
                    if (target != null) {
                        if (target.isResourceType(RESOURCE_TYPE)) {
                            Clientlib embedded = new Clientlib(request, reference, type);
                            lastModified = getLastModified(embedded.getLastModified(), lastModified);
                        } else {
                            lastModified = getLastModified(new FileHandle(target), lastModified);
                        }
                    }
                } else {
                    for (Resource child : resource.getChildren()) {
                        lastModified = getLastModified(ResourceHandle.use(child), lastModified);
                    }
                }
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

    public List<Link> getLinks(boolean expanded, boolean depends, boolean optional,
                               Map<String, String> properties, RendererContext context) {
        expanded = definition.getProperty(PROP_EXPANDED, expanded);
        optional = definition.getProperty(PROP_OPTIONAL, optional);
        List<Link> links = new ArrayList<>();
        if (definition.isValid()) {
            Link link = getLink(resource, resource, properties, type);
            if (context.tryAndRegister(link.getKey())) {
                if (expanded) {
                    getLinks(definition, properties, context, links, false, optional);
                } else {
                    getDependencyLinks(resource, properties, context, links, optional);
                    links.add(link);
                }
            } else {
                if (!depends) {
                    logDuplicate(link);
                }
            }
        }
        return links;
    }

    protected void logDuplicate(Link link) {
        LOG.warn("Clientlib entry '" + link.url + "' of '"
                + getPath() + "' already embedded - ignored here.");
    }

    protected void logNotAvailable(ResourceHandle resource, String reference, boolean optional) {
        if (!optional) {
            LOG.warn("Clientlib entry '" + reference + "' of '" + resource.getPath() + "' not available.");
        }
    }

    protected void getDependencyLinks(ResourceHandle resource, Map<String, String> properties,
                                      RendererContext context, List<Link> links, boolean optional) {
        if (resource.isValid() && !isFile(resource)) {
            optional = resource.getProperty(PROP_OPTIONAL, optional);
            for (String reference : resource.getProperty(PROP_DEPENDS, new String[0])) {
                Clientlib dependency = new Clientlib(request, reference, type);
                if (dependency.isValid()) {
                    links.addAll(dependency.getLinks(false, true, optional, properties, context));
                }
            }
            for (Resource child : resource.getChildren()) {
                getDependencyLinks(ResourceHandle.use(child), properties, context, links, optional);
            }
        }
    }

    protected void getLinks(ResourceHandle resource, Map<String, String> properties,
                            RendererContext context, List<Link> links, boolean depends, boolean optional) {
        if (resource.isValid()) {
            if (isFile(resource)) {
                FileHandle file = new FileHandle(resource);
                addFileToList(resource, file, properties, links, context, depends, optional);
            } else {
                optional = resource.getProperty(PROP_OPTIONAL, optional);
                for (String reference : resource.getProperty(PROP_DEPENDS, new String[0])) {
                    embedClientlib(resource, reference, properties, context, links, true, optional);
                }
                for (String reference : resource.getProperty(PROP_EMBED, new String[0])) {
                    embedClientlib(resource, reference, properties, context, links, depends, optional);
                }
                for (Resource child : resource.getChildren()) {
                    getLinks(ResourceHandle.use(child), properties, context, links, depends, optional);
                }
            }
        }
    }

    protected void embedClientlib(ResourceHandle resource, String reference, Map<String, String> properties,
                                  RendererContext context, List<Link> links, boolean depends, boolean optional) {
        Resource target = retrieveResource(reference);
        if (target != null) {
            if (target.isResourceType(RESOURCE_TYPE)) {
                Clientlib embedded = new Clientlib(request, reference, type);
                embedded.getLinks(embedded.definition, properties, context, links, depends, optional);
            } else {
                FileHandle file = new FileHandle(target);
                addFileToList(resource, file, properties, links, context, depends, optional);
            }
        } else {
            logNotAvailable(resource, reference, optional);
        }
    }

    protected void addFileToList(ResourceHandle reference, FileHandle file,
                                 Map<String, String> properties, List<Link> links,
                                 RendererContext context, boolean depends, boolean optional) {
        if (file.isValid()) {
            ResourceHandle fileRes = file.getResource();
            Link link = getLink(reference, fileRes, properties, null);
            if (context.tryAndRegister(link.getKey())) {
                links.add(link);
            } else {
                if (!depends) {
                    logDuplicate(link);
                }
            }
        } else {
            logNotAvailable(reference, file.getResource().getPath(), optional);
        }
    }

    protected Link getLink(ResourceHandle reference, Resource target, Map<String, String> properties, Type type) {
        String url = target.getPath();
        if (type != null) {
            url += "." + type;
        }
        url = LinkUtil.getUrl(request, url);
        String rel = reference.getProperty(PROP_REL, properties.get(PROP_REL));
        return new Link(url, PROP_REL, rel);
    }

    public void processContent(OutputStream output, ClientlibProcessor processor, ProcessorContext context)
            throws IOException, RepositoryException {
        processContent(definition, output, processor, context, false);
        output.close();
    }

    protected void processContent(ResourceHandle resource, OutputStream output,
                                  ClientlibProcessor processor, ProcessorContext context, boolean optional)
            throws RepositoryException, IOException {
        if (resource.isValid()) {
            if (isFile(resource)) {
                processFile(resource, output, processor, context, optional);
            } else {
                optional = resource.getProperty(PROP_OPTIONAL, optional);
                for (String reference : resource.getProperty(PROP_EMBED, new String[0])) {
                    Resource target = retrieveResource(reference);
                    if (target != null) {
                        if (target.isResourceType(RESOURCE_TYPE)) {
                            Clientlib embedded = new Clientlib(request, reference, type);
                            embedded.processContent(embedded.definition, output, processor, context, optional);
                        } else {
                            processFile(target, output, processor, context, optional);
                        }
                    } else {
                        logNotAvailable(resource, reference, optional);
                    }
                }
                for (Resource child : resource.getChildren()) {
                    processContent(ResourceHandle.use(child), output, processor, context, optional);
                }
            }
        }
    }

    protected void processFile(Resource resource, OutputStream output,
                               ClientlibProcessor processor, ProcessorContext context, boolean optional)
            throws RepositoryException, IOException {
        FileHandle file = new FileHandle(resource);
        InputStream content = file.getStream();
        if (content != null) {
            if (processor != null) {
                content = processor.processContent(this, content, context);
            }
            IOUtils.copy(content, output);
            output.write('\n');
            output.flush();
        } else {
            logNotAvailable(file.getResource(), "[content]", optional);
        }
    }

    public static String[] splitPathAndName(String path) {
        String[] result = new String[2];
        int nameSeparator = path.lastIndexOf('/');
        result[0] = path.substring(0, nameSeparator);
        result[1] = path.substring(nameSeparator + 1);
        return result;
    }

    public static boolean isFile(Resource resource) {
        return resource.isResourceType(ResourceUtil.TYPE_FILE) ||
                resource.isResourceType(ResourceUtil.TYPE_LINKED_FILE);
    }
}
