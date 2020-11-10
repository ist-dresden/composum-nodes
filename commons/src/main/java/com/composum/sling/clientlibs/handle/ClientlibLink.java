package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.servlet.ClientlibCategoryServlet;
import com.composum.sling.clientlibs.servlet.ClientlibServlet;
import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.composum.sling.clientlibs.handle.ClientlibLink.Kind.CATEGORY;
import static com.composum.sling.clientlibs.handle.ClientlibLink.Kind.EXTERNALURI;

/**
 * Represents data for linking to an actually referred file / client library / category of client libraries.
 * <p>
 * Since this is kept alive during the processing of the request by {@link RendererContext} and we don't want to keep
 * references to the corresponding Sling Resources around for longer than needed, this does not contain a reference to
 * the resource, just the path. In the case of files this however means that we have already to put in the correct
 * minified selector according to {@link RendererContext#useMinifiedFiles()}, since there might or might not be a
 * minified or unminified sibling and we don't want to re-resolve the Resource to determine this. To make things
 * consistent, we do this always.
 */
public class ClientlibLink {

    /** The kind of resource we link to: {@link #CATEGORY}, {@link #CLIENTLIB}, {@link #FILE}, {@link #EXTERNALURI}. */
    public enum Kind {
        /** A link to a category of clientlibs which is rendered by the {@link com.composum.sling.clientlibs.servlet.ClientlibCategoryServlet}. */
        CATEGORY,
        /** A link to a client library that is rendered by the {@link com.composum.sling.clientlibs.servlet.ClientlibServlet}. */
        CLIENTLIB,
        /** A link to a file in the system. */
        FILE,
        /** URI to external system. */
        EXTERNALURI
    }

    /**
     * Name of the additional property to set the link relation in case of {@link Clientlib.Type#link} or {@link
     * Clientlib.Type#img}.
     */
    public static final String PROP_REL = "rel";

    /** List of supported additional properties for links etc. */
    public static final String[] LINK_PROPERTIES = new String[]{PROP_REL};

    /** Full path to the resource, the category if this refers to category, or an external URI. */
    public final String path;

    /** Resource-type of the link. */
    public final Clientlib.Type type;

    /** Kind of {@link ClientlibElement} this was created for. */
    public final Kind kind;

    /** Additional properties, e.g. {@link #PROP_REL}. */
    public final Map<String, String> properties;

    /**
     * An optional hash that is encoded in the URL to ensure that each change of a resource will lead to a new URL
     * avoiding caching problems. Used only on Clientlibs / categories since external URLs and file URLs don't
     * support the used format.
     */
    public final String hash;

    /**
     * Creates a new link.
     *
     * @param type       the type of the linked resource - js, css, ...
     * @param kind       the kind of linked resource - clientlib, file, ...
     * @param path       the path / category name / url to the linked resource
     * @param properties additional properties used in rendering
     * @param hash       optional hash for the resource to be encoded in the URL for clientlibs / - categories.
     */
    protected ClientlibLink(Clientlib.Type type, Kind kind, String path, Map<String, String> properties, String hash) {
        this.path = path;
        this.type = type;
        this.kind = kind;
        this.properties = Collections.unmodifiableMap(properties != null ? new HashMap<>(properties) : new HashMap<>());
        this.hash = hash;
    }

    /**
     * Creates a new link.
     *
     * @param type       the type of the linked resource - js, css, ...
     * @param kind       the kind of linked resource - clientlib, file, ...
     * @param path       the path / category name / url to the linked resource
     * @param properties additional properties used in rendering
     */
    public ClientlibLink(Clientlib.Type type, Kind kind, String path, Map<String, String> properties) {
        this(type, kind, path, properties, null);
    }

    /**
     * Creates a new link with parameters as this except hash.
     *
     * @param newHash new nullable hash for the resource to be encoded in the URL for clientlibs / - categories.
     */
    public ClientlibLink withHash(String newHash) {
        return new ClientlibLink(type, kind, path, properties, newHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientlibLink)) return false;

        ClientlibLink that = (ClientlibLink) o;

        if (!Objects.equals(kind, that.kind)) return false;
        if (!Objects.equals(path, that.path)) return false;
        if (type != that.type) return false;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 92821 * result + (path != null ? path.hashCode() : 0);
        result = 92821 * result + (kind != null ? kind.hashCode() : 0);
        result = 92821 * result + (type != null ? type.hashCode() : 0);
        result = 92821 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    public static ClientlibLink forCategory(Clientlib.Type type, String category) {
        return new ClientlibLink(type, CATEGORY, category, null);
    }

    boolean isCategory() {
        return CATEGORY == kind;
    }

    boolean isExternalUri() {
        return EXTERNALURI == kind;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(type).append(":");
        if (CATEGORY == kind) builder.append(ClientlibRef.PREFIX_CATEGORY);
        builder.append(path);
        if (null != hash) builder.append("@").append(hash);
        if (null != properties && !properties.isEmpty()) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                builder.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return builder.toString();
    }

    /**
     * Determines the URL we render into the page. We don't want to access resources here, so at least for files we need
     * already to know the exact path with .min or not.
     * <p>
     * Cases: <ul> <li>Clientlib category: refers to the {@link com.composum.sling.clientlibs.servlet.ClientlibCategoryServlet},
     * parameterized by type and minified according to {@link RendererContext#useMinifiedFiles()}. </li> <li>Clientlib:
     * path to the client library plus minified and type.</li> <li>File:</li> </ul>
     *
     * @param request the request
     * @param context the context
     * @return the url
     */
    public String getUrl(SlingHttpServletRequest request, RendererContext context) {
        String uri;
        switch (kind) {
            case FILE: // we can only refer to that exact resource.
            case EXTERNALURI:
                uri = path;
                break;
            case CLIENTLIB:
                uri = ClientlibServlet.makePath(path, type, context.useMinifiedFiles(), hash);
                break;
            case CATEGORY:
                uri = ClientlibCategoryServlet.makePath(path, type, context.useMinifiedFiles(), hash);
                break;
            default:
                throw new UnsupportedOperationException("Bug - impossible.");
        }
        String url;
        if (context.mapClientlibURLs()) {
            url = LinkUtil.getUrl(request, uri);
        } else {
            url = LinkUtil.getUnmappedUrl(request, uri);
        }
        return url;
    }

}
