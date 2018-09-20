package com.composum.sling.cpnl;

import com.composum.sling.core.RequestBundle;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the set of taglib JSP EL functions
 */
public class CpnlElFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(CpnlElFunctions.class);

    public static final Pattern HREF_PATTERN = Pattern.compile("(<a(\\s*[^>]*)?\\s*href\\s*=\\s*['\"])([^'\"]+)([\"'][^>]*>)");

    /** for the 'attr' escaping - the quotation type constants */
    public static final int QTYPE_QUOT = 0;
    public static final int QTYPE_APOS = 1;
    public static final String[] QTYPE_CHAR = new String[]{"\"", "'"};
    public static final String[] QTYPE_ESC = new String[]{"&quot;", "&apos;"};

    public static final String[] RICH_TEXT_TAGS = new String[]{
            "p", "br", "a", "ul", "li", "ol", "strong", "em", "u", "b", "i", "strike", "sub", "sup"
    };

    public static final String[][] RICH_TEXT_BASIC_ESCAPE = {
            // avoid double escape
            {"&amp;", "&amp;"},
            {"&lt;", "&lt;"},
            {"&gt;", "&gt;"},
            // escape if not skipped
            {"&", "&amp;"},
            {"<", "&lt;"},
            {">", "&gt;"},
    };

    protected static final List<String> RICH_TEXT_TAG_START;
    protected static final List<String> RICH_TEXT_TAG_CLOSED;
    protected static final int RICH_TEXT_TAG_MAX_LEN;

    static {
        int maxLen = 0;
        RICH_TEXT_TAG_START = new ArrayList<>();
        for (String tag : RICH_TEXT_TAGS) {
            RICH_TEXT_TAG_START.add("<" + tag + " ");
            int length = tag.length() + 2;
            if (maxLen < length) {
                maxLen = length;
            }
        }
        RICH_TEXT_TAG_CLOSED = new ArrayList<>();
        for (String tag : RICH_TEXT_TAGS) {
            RICH_TEXT_TAG_CLOSED.add("<" + tag + ">");
            RICH_TEXT_TAG_CLOSED.add("<" + tag + "/>");
            RICH_TEXT_TAG_CLOSED.add("</" + tag + ">");
            int length = tag.length() + 3;
            if (maxLen < length) {
                maxLen = length;
            }
        }
        RICH_TEXT_TAG_MAX_LEN = maxLen;
    }

    public static class RichTextTagsFilter extends LookupTranslator {

        protected boolean isInTagStart = false;

        public RichTextTagsFilter() {
            super(RICH_TEXT_BASIC_ESCAPE);
        }

        @Override
        public int translate(CharSequence input, int index, Writer out) throws IOException {
            if (isInTagStart) {
                char token = input.charAt(index);
                out.write(token);
                if (token == '>') {
                    isInTagStart = false;
                }
                return 1;
            } else {
                String subSeq = input.subSequence(index, index + Math.min(RICH_TEXT_TAG_MAX_LEN, input.length() - index))
                        .toString().toLowerCase();
                for (String pattern : RICH_TEXT_TAG_START) {
                    if (subSeq.startsWith(pattern)) {
                        out.write(pattern);
                        isInTagStart = true;
                        return pattern.length();
                    }
                }
                for (String pattern : RICH_TEXT_TAG_CLOSED) {
                    if (subSeq.startsWith(pattern)) {
                        out.write(pattern);
                        return pattern.length();
                    }
                }
                return super.translate(input, index, out);
            }
        }
    }


    public static final CharSequenceTranslator ESCAPE_RICH_TEXT =
            new AggregateTranslator(
                    new RichTextTagsFilter(),
                    new LookupTranslator(EntityArrays.ISO8859_1_ESCAPE()),
                    new LookupTranslator(EntityArrays.HTML40_EXTENDED_ESCAPE())
            );

    public static String escapeRichText(String input) {
        return ESCAPE_RICH_TEXT.translate(input);
    }

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
        return value != null ? StringEscapeUtils.escapeHtml4(value) : value;
    }

    /**
     * Returns the escaped text of a rich text value as html for a tag attribute.
     *
     * @param value the value to escape
     * @return the HTML escaped rich text of the value
     */
    public static String attr(SlingHttpServletRequest request, String value, int qType) {
        if (value != null) {
            value = rich(request, value);
            value = value.replaceAll(QTYPE_CHAR[qType], QTYPE_ESC[qType]);
        }
        return value;
    }

    /**
     * Returns the escaped text of a rich text value (reduced HTML escaping).
     *
     * @param value the rich text value to escape
     * @return the escaped HTML code of the value
     */
    public static String rich(SlingHttpServletRequest request, String value) {
        if (value != null) {
            // ensure that a rich text value is always enclosed with a HTML paragraph tag
            if (StringUtils.isNotBlank(value) && !value.trim().startsWith("<p>")) {
                value = "<p>" + value + "</p>";
            }
            // transform embedded resource links (paths) to mapped URLs
            value = map(request, value);
            value = escapeRichText(value);
        }
        return value;
    }

    /**
     * Replaces all 'href' attribute values found in the text value by the resolver mapped value.
     *
     * @param request the text (rich text) value
     * @param value   the text (rich text) value
     * @return the transformed text value
     */
    public static String map(SlingHttpServletRequest request, String value) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = HREF_PATTERN.matcher(value);
        int len = value.length();
        int pos = 0;
        while (matcher.find(pos)) {
            String unmapped = matcher.group(3);
            String mapped = url(request, unmapped);
            result.append(value, pos, matcher.start());
            result.append(matcher.group(1));
            result.append(mapped);
            result.append(matcher.group(4));
            pos = matcher.end();
        }
        if (pos >= 0 && pos < len) {
            result.append(value, pos, len);
        }
        return result.toString();
    }

    /**
     * Returns the encoded path of a of a repository path.
     *
     * @param value the path to encode
     * @return the encoded path
     */
    public static String path(String value) {
        return value != null ? LinkUtil.encodePath(value) : null;
    }

    /**
     * Returns the escaped script code of a value (Script escaping to prevent from XSS).
     *
     * @param value the value to escape
     * @return the Script escaped code of the value
     */
    public static String script(String value) {
        return value != null ? StringEscapeUtils.escapeEcmaScript(value) : value;
    }

    /**
     * Returns the encapsulated CDATA string of a value (no escaping!).
     *
     * @param value the value to encasulate
     * @return the string with <![CDATA[ ... ]]> around
     */
    public static String cdata(String value) {
        return value != null ? "<![CDATA[" + value + "]]>" : null;
    }
}
