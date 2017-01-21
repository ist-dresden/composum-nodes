package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.resource.Resource;

import java.util.HashMap;
import java.util.Map;

import static com.composum.sling.clientlibs.handle.Clientlib.Type.img;
import static com.composum.sling.clientlibs.handle.Clientlib.Type.link;

public class ClientlibLink extends ClientlibKey {

    protected final String libPath;
    protected final boolean minified;

    public ClientlibLink(final Clientlib clientlib, boolean minified) {
        this(clientlib.clientlibRef, clientlib.resource, minified);
    }

    public ClientlibLink(final ClientlibRef reference, final Resource resource, boolean minified) {
        this(reference.type, resource, reference.properties, minified);
    }

    public ClientlibLink(final ClientlibRef reference, final Resource resource,
                         final Map<String, String> properties, boolean minified) {
        this(reference.type, resource, properties, minified);
    }

    public ClientlibLink(final Clientlib.Type type, final Resource resource, boolean minified) {
        this(type, resource, new HashMap<String, String>(), minified);
    }

    public ClientlibLink(final Clientlib.Type type, final Resource resource,
                         Map<String, String> properties, boolean minified) {
        super(type, resource.getPath(), properties);
        this.libPath = resource.getPath();
        this.minified = minified &&
                (!Clientlib.isFile(resource) || Clientlib.getMinifiedSibling(resource) != resource);
    }

    public String getUrl(RendererContext context) {
        StringBuilder builder = new StringBuilder(libPath);
        if (!libPath.endsWith("." + type.name()) && type != img && type != link) {
            builder.append('.').append(type.name());
        }
        String uri = builder.toString();
        if (minified && context.useMinifiedFiles()) {
            uri = Clientlib.getMinifiedSibling(uri);
        }
        String url;
        if (context.mapClientlibURLs()) {
            url = LinkUtil.getUrl(context.request, uri);
        } else {
            url = LinkUtil.getUnmappedUrl(context.request, uri);
        }
        return url;
    }
}
