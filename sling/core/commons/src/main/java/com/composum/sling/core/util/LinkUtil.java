package com.composum.sling.core.util;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.tika.mime.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class LinkUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LinkUtil.class);

    public static final String EXT_HTML = ".html";

    public static final String PROP_TARGET = "sling:target";
    public static final String PROP_REDIRECT = "sling:redirect";

    public static final Pattern URL_PATTERN = Pattern.compile("^(https?)://([^/]+)(:\\d+)?(/.*)?$");

    public static final Pattern SELECTOR_PATTERN = Pattern.compile("^(.*/[^/]+)(\\.[^.]+)$");

    /**
     * Builds a mapped link to a path (resource path) without selectors and a determined extension.
     *
     * @param request the request context for path mapping (the result is always mapped)
     * @param url     the URL to use (complete) or the path to an addressed resource (without any extension)
     * @return the mapped url for the referenced resource
     */
    public static String getUrl(SlingHttpServletRequest request, String url) {
        return getUrl(request, url, null, null);
    }

    /**
     * Builds a unmapped link to a path (resource path) without selectors and a determined extension.
     *
     * @param request the request context for path mapping (the result is always mapped)
     * @param url     the URL to use (complete) or the path to an addressed resource (without any extension)
     * @return the mapped url for the referenced resource
     */
    public static String getUnmappedUrl(SlingHttpServletRequest request, String url) {
        return getUrl(request, url, null, null, false);
    }

    /**
     * Builds a mapped link to a path (resource path) without selectors and with the given extension.
     *
     * @param request   the request context for path mapping (the result is always mapped)
     * @param url       the URL to use (complete) or the path to an addressed resource (without any extension)
     * @param extension the extension (can be 'null'; should be 'html or '.html' by default)
     * @return the mapped url for the referenced resource
     */
    public static String getUrl(SlingHttpServletRequest request, String url, String extension) {
        return getUrl(request, url, null, extension);
    }

    /**
     * Builds a mapped link to the path (resource path) with optional selectors and extension.
     *
     * @param request   the request context for path mapping (the result is always mapped)
     * @param url       the URL to use (complete) or the path to an addressed resource (without any extension)
     * @param selectors an optional selector string with all necessary selectors (can be 'null')
     * @param extension an optional extension (can be 'null' for extension determination)
     * @return the mapped url for the referenced resource
     */
    public static String getUrl(SlingHttpServletRequest request, String url,
                                String selectors, String extension) {
        return getUrl(request, url, selectors, extension, true);
    }

    /**
     * Builds a mapped link to the path (resource path) with optional selectors and extension.
     *
     * @param request   the request context for path mapping (the result is always mapped)
     * @param url       the URL to use (complete) or the path to an addressed resource (without any extension)
     * @param selectors an optional selector string with all necessary selectors (can be 'null')
     * @param extension an optional extension (can be 'null' for extension determination)
     * @param mapUrl    if 'true': use Resolver for a final URL mapping
     * @return the mapped url for the referenced resource
     */
    public static String getUrl(SlingHttpServletRequest request, String url,
                                String selectors, String extension, boolean mapUrl) {

        // skip blank urls
        if (StringUtils.isBlank(url)) {
            return url;
        }

        // rebuild URL if not always external only
        if (!isExternalUrl(url)) {

            ResourceResolver resolver = request.getResourceResolver();
            ResourceHandle resource = ResourceHandle.use(resolver.getResource(url));

            // it's possible that the resource can not be resolved / is virtual but is valid...
            if (resource.isValid()) {
                // forwards and extensions are resolvable for real resources only...

                // check for a target and 'forward' to this target if found
                try {
                    String redirect = getFinalTarget(resource);
                    if (StringUtils.isNotBlank(redirect)) {
                        return getUrl(request, redirect, selectors, extension);
                    }
                } catch (RedirectLoopException rlex) {
                    LOG.error(rlex.toString());
                }

                // check for a necessary extension and determine it if not specified
                extension = getExtension(resource, extension);
            }

            // map the path (the url) with the resource resolver (encodes the url)
            if (mapUrl) {
                url = resolver.map(request, url);
            } else {
                url = request.getContextPath() + url;
            }

            if (StringUtils.isNotBlank(extension)) {
                url += extension;   // extension starts with a '.'
            }

            // inject selectors into the complete URL because
            // it's possible, that the name always contains the extension...
            if (StringUtils.isNotBlank(selectors)) {
                if (!selectors.startsWith(".")) {
                    selectors = "." + selectors;
                }

                Matcher matcher = SELECTOR_PATTERN.matcher(url);
                if (matcher.matches()) {
                    url = matcher.group(1) + selectors + matcher.group(2);
                }
            }
        }
        return url;
    }

    /**
     * Makes a URL already built external; the url should be built by the 'getUrl' method.
     *
     * @param request the request as the externalization context
     * @param url     the url value (the local URL)
     * @return
     */
    public static String getAbsoluteUrl(SlingHttpServletRequest request, String url) {
        if (!isExternalUrl(url) && url.startsWith("/")) {
            String scheme = request.getScheme().toLowerCase();
            url = scheme + "://" + getAuthority(request) + url;
        }
        return url;
    }

    /**
     * Builds the 'authority' part (host:port) of an absolute URL.
     *
     * @param request the current request with the 'host' and 'port' values
     */
    public static String getAuthority(SlingHttpServletRequest request) {
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isSecure = request.isSecure();
        return port > 0 && ((!isSecure && port != 80) || (isSecure && port != 443))
                ? (host + ":" + port) : host;
    }

    /**
     * Returns 'true' if the url is an 'external' url (starts with 'https?://')
     */
    public static boolean isExternalUrl(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * Returns the resource referenced by an URL.
     */
    public static Resource resolveUrl(SlingHttpServletRequest request, String url) {
        return request.getResourceResolver().getResource(url);
    }

    /**
     * Retrieves the target for a resource if there are redirects declared.
     *
     * @return the target path or url (can be external); 'null' if no redirect detected
     * @throws RedirectLoopException if a 'loop' has been detected during redirect resolving
     */
    public static String getFinalTarget(Resource resource) throws RedirectLoopException {
        ResourceHandle handle = ResourceHandle.use(resource);
        String finalTarget = getFinalTarget(handle, new ArrayList<String>());
        return finalTarget;
    }

    protected static String getFinalTarget(ResourceHandle resource, List<String> trace)
            throws RedirectLoopException {
        String finalTarget = null;
        if (resource.isValid()) {
            String path = resource.getPath();
            if (trace.contains(path)) {
                throw new RedirectLoopException(trace, path);
            }
            String redirect = resource.getProperty(PROP_TARGET);
            if (StringUtils.isBlank(redirect)) {
                redirect = resource.getProperty(PROP_REDIRECT);
            }
            if (StringUtils.isNotBlank(redirect)) {
                trace.add(path);
                finalTarget = redirect;
                if (!URL_PATTERN.matcher(finalTarget).matches()) {
                    ResourceResolver resolver = resource.getResourceResolver();
                    Resource targetResource = resolver.getResource(finalTarget);
                    if (targetResource != null) {
                        String target = getFinalTarget(ResourceHandle.use(targetResource), trace);
                        if (StringUtils.isNotBlank(target)) {
                            finalTarget = target;
                        }
                    }
                }
            }
        }
        return finalTarget;
    }

    public static class RedirectLoopException extends Exception {

        public final List<String> trace;
        public final String target;

        public RedirectLoopException(List<String> trace, String target) {
            super("redirect loop detected in '" + trace.get(trace.size() - 1) +
                    "' which redirects to '" + target + "'");
            this.trace = trace;
            this.target = target;
        }
    }

    /**
     * Returns the extension for a URL to a resource based on a predefined value (can be null or '').
     * The result is always not 'null' and can be added without check; it starts with a '.' if not blank.
     *
     * @param resource  the referenced resource
     * @param extension the predefined extension (can be 'null' or blank for determination)
     * @return the string which has to add to the resources path; '' if nothing should add
     */
    public static String getExtension(ResourceHandle resource, String extension) {
        return getExtension(resource, extension, false);
    }

    /**
     * Returns the extension for a URL to a resource based on a predefined value (can be null or '').
     * The result is always not 'null' and can be added without check; it starts with a '.' if not blank.
     *
     * @param resource                the referenced resource
     * @param extension               the predefined extension (can be 'null' or blank for determination)
     * @param detectMimeTypeExtension if 'true' an extension according to the mime type will be detected
     * @return the string which has to add to the resources path; '' if nothing should add
     */
    public static String getExtension(ResourceHandle resource, String extension, boolean detectMimeTypeExtension) {
        if (StringUtils.isBlank(extension) && detectMimeTypeExtension) {
            if (resource.isFile()) {
                MimeType mimeType = MimeTypeUtil.getMimeType(resource);
                if (mimeType != null) {
                    extension = mimeType.getExtension();
                }
            }
        }
        if (StringUtils.isNotBlank(extension)) {
            String name = resource.getName();
            if (name.toLowerCase().endsWith(extension.toLowerCase())) {
                extension = ""; // no extension necessary to add
            }
        }
        if (StringUtils.isBlank(extension)) {
            if (!resource.getResourceType().equals(resource.getPrimaryType())) {
                extension = EXT_HTML; // use '.html' by default if a real resource type is present
            } else {
                ResourceHandle content = resource.getContentResource();
                if (content.isValid() && !content.getResourceType().equals(content.getPrimaryType())) {
                    extension = EXT_HTML; // use '.html' by default if a content resource exists with a real resource type
                }
            }
        }
        if (StringUtils.isNotBlank(extension)) {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
        }
        return extension != null ? extension : "";
    }

    /**
     * URL encoding for a resource path (without the encoding for the '/' path delimiters).
     *
     * @param path the path to encode
     * @return the URL encoded path
     */
    public static String encodePath(String path) {
        if (path != null) {
            path = path.replaceAll("/jcr:", "/_jcr_");
            path = path.replaceAll("&", "%26");
            path = path.replaceAll(":", "%3A");
            path = path.replaceAll(";", "%3B");
            path = path.replaceAll(" ", "%20");
        }
        return path;
    }
}
