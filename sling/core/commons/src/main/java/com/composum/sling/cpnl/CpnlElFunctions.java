package com.composum.sling.cpnl;

import com.composum.sling.core.RequestBundle;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;

/**
 * the set of taglib JSP EL functions
 */
public class CpnlElFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(CpnlElFunctions.class);

    public static String i18n(SlingHttpServletRequest request, String text) {
        String translated = null;
        try {
            translated = RequestBundle.get(request).getString(text);
        } catch (MissingResourceException mrex) {
            if (LOG.isInfoEnabled()) {
                LOG.info(mrex.toString());
            }
        }
        return translated != null ? translated : text;
    }

    /**
     * Returns the repository path of a child of a resource.
     *
     * @param base the parent resource object
     * @param path the relative path to the child resource
     * @return the absolute path of the child if found, otherwise the original path value
     */
    public static String child(Resource base, String path) {
        Resource child = base.getChild(path);
        return child != null ? child.getPath() : path;
    }

    /**
     * Builds the URL for a repository path using the LinkUtil.getURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String url(SlingHttpServletRequest request, String path) {
        return LinkUtil.getUrl(request, path);
    }

    /**
     * Builds the URL for a repository path using the LinkUtil.getMappedURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String mappedUrl(SlingHttpServletRequest request, String path) {
        return LinkUtil.getMappedUrl(request, path);
    }

    /**
     * Builds the URL for a repository path using the LinkUtil.getUnmappedURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String unmappedUrl(SlingHttpServletRequest request, String path) {
        return LinkUtil.getUnmappedUrl(request, path);
    }

    /**
     * Builds an external (full qualified) URL for a repository path using the LinkUtil.getURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String externalUrl(SlingHttpServletRequest request, String path) {
        return LinkUtil.getAbsoluteUrl(request, LinkUtil.getUrl(request, path));
    }

    /**
     * Builds an external (full qualified) URL for a repository path using the LinkUtil.getMappedURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String mappedExternalUrl(SlingHttpServletRequest request, String path) {
        return LinkUtil.getAbsoluteUrl(request, LinkUtil.getMappedUrl(request, path));
    }

    /**
     * Builds an external (full qualified) URL for a repository path using the LinkUtil.getUnmappedURL() method.
     *
     * @param request the current request (domain host hint)
     * @param path    the repository path
     * @return the URL built in the context of the requested domain host
     */
    public static String unmappedExternalUrl(SlingHttpServletRequest request, String path) {
        return LinkUtil.getAbsoluteUrl(request, LinkUtil.getUnmappedUrl(request, path));
    }

    /**
     * A 'placeholder' to signal 'avoid escaping' during text rendering.
     *
     * @param value the value to render
     * @return the unescaped value
     */
    public static Object value(Object value) {
        return value;
    }

    /**
     * Returns the escaped text of a value (HTML escaping to prevent from XSS).
     *
     * @param value the value to escape
     * @return the HTML escaped text of the value
     */
    public static String text(String value) {
        return StringEscapeUtils.escapeHtml4(value);
    }

    /**
     * Returns the escaped text of a rich text value (reduced HTML escaping).
     *
     * @param value the rich text value to escape
     * @return the escaped HTML code of the value
     */
    public static String rich(String value) {
        return value;
    }

    /**
     * Returns the encoded path of a of a repository path.
     *
     * @param value the path to encode
     * @return the encoded path
     */
    public static String path(String value) {
        return LinkUtil.encodePath(value);
    }

    /**
     * Returns the escaped script code of a value (Script escaping to prevent from XSS).
     *
     * @param value the value to escape
     * @return the Script escaped code of the value
     */
    public static String script(String value) {
        return StringEscapeUtils.escapeEcmaScript(value);
    }
}
