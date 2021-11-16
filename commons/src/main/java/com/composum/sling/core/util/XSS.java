package com.composum.sling.core.util;

import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.XSSFilter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * the static access for the Sling XSSAPI / XSSFilter - wraps the Sling XSS services
 */
public class XSS {

    protected static ServiceHandle<XSSAPI> XSSAPI_HANDLE = new ServiceHandle<>(XSSAPI.class);
    protected static ServiceHandle<XSSFilter> XSSFilter_HANDLE = new ServiceHandle<>(XSSFilter.class);

    @NotNull
    public static XSSAPI api() {
        return XSSAPI_HANDLE.getService();
    }

    @NotNull
    public static XSSFilter filter() {
        return XSSFilter_HANDLE.getService();
    }

    protected XSS() {
    }

    //
    // XSSAPI
    //

    /**
     * Validate a string which should contain an integer, returning a default value if the source is
     * {@code null}, empty, can't be parsed, or contains XSS risks.
     *
     * @param integer      the source integer
     * @param defaultValue a default value if the source can't be used, is {@code null} or an empty string
     * @return a sanitized integer
     */
    @Nullable
    public static Integer getValidInteger(@Nullable String integer, int defaultValue) {
        return api().getValidInteger(integer, defaultValue);
    }

    /**
     * Validate a string which should contain a long, returning a default value if the source is
     * {@code null}, empty, can't be parsed, or contains XSS risks.
     *
     * @param source       the source long
     * @param defaultValue a default value if the source can't be used, is {@code null} or an empty string
     * @return a sanitized integer
     */
    @Nullable
    public static Long getValidLong(@Nullable String source, long defaultValue) {
        return api().getValidLong(source, defaultValue);
    }

    /**
     * Validate a string which should contain an double, returning a default value if the source is
     * {@code null}, empty, can't be parsed, or contains XSS risks.
     *
     * @param source      the source double
     * @param defaultValue a default value if the source can't be used, is {@code null} or an empty string
     * @return a sanitized double
     */
    @Nullable
    public static Double getValidDouble(@Nullable String source, double defaultValue) {
        return api().getValidDouble(source, defaultValue);
    }

    /**
     * Validate a string which should contain a dimension, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.  Allows integer dimensions and the keyword "auto".
     *
     * @param dimension    the source dimension
     * @param defaultValue a default value if the source can't be used, is {@code null} or an empty string
     * @return a sanitized dimension
     */
    @Nullable
    public static String getValidDimension(@Nullable String dimension, @Nullable String defaultValue) {
        return api().getValidDimension(dimension, defaultValue);
    }

    /**
     * Sanitizes a URL for writing as an HTML href or src attribute value.
     *
     * @param url the source URL
     * @return a sanitized URL (possibly empty)
     */
    @NotNull
    public static String getValidHref(@Nullable String url) {
        return api().getValidHref(url);
    }

    /**
     * Validate a Javascript token.  The value must be either a single identifier, a literal number,
     * or a literal string.
     *
     * @param token        the source token
     * @param defaultValue a default value to use if the source is {@code null}, an empty string, or doesn't meet validity constraints.
     * @return a string containing a single identifier, a literal number, or a literal string token
     */
    @Nullable
    public static String getValidJSToken(@Nullable String token, @Nullable String defaultValue) {
        return api().getValidJSToken(token, defaultValue);
    }

    /**
     * Validate a style/CSS token. Valid CSS tokens are specified at http://www.w3.org/TR/css3-syntax/
     *
     * @param token        the source token
     * @param defaultValue a default value to use if the source is {@code null}, an empty string, or doesn't meet validity constraints.
     *
     * @return a string containing sanitized style token
     */
    @Nullable
    public static String getValidStyleToken(@Nullable String token, @Nullable String defaultValue) {
        return api().getValidStyleToken(token, defaultValue);
    }

    /**
     * Validate a CSS color value. Color values as specified at http://www.w3.org/TR/css3-color/#colorunits
     * are safe and definitively allowed. Vulnerable constructs will be disallowed. Currently known
     * vulnerable constructs include url(...), expression(...), and anything with a semicolon.
     *
     * @param color        the color value to be used.
     * @param defaultColor a default value to use if the input color value is {@code null}, an empty string, doesn't meet validity constraints.
     * @return a string a css color value.
     */
    @Nullable
    public static String getValidCSSColor(@Nullable String color, @Nullable String defaultColor) {
        return api().getValidCSSColor(color, defaultColor);
    }

    /**
     * Validate multi-line comment to be used inside a &lt;script&gt;...&lt;/script&gt; or &lt;style&gt;...&lt;/style&gt; block. Multi-line
     * comment end block is disallowed.
     *
     * @param comment           the comment to be used
     * @param defaultComment    a default value to use if the comment is {@code null} or not valid.
     * @return a valid multi-line comment
     */
    public static String getValidMultiLineComment(@Nullable String comment, @Nullable String defaultComment) {
        return api().getValidMultiLineComment(comment, defaultComment);
    }

    /**
     * Validate a JSON string
     *
     * @param json          the JSON string to validate
     * @param defaultJson   the default value to use if {@code json} is {@code null} or not valid
     * @return a valid JSON string
     */
    public static String getValidJSON(@Nullable String json, @Nullable String defaultJson) {
        return api().getValidJSON(json, defaultJson);
    }

    /**
     * Validate an XML string
     *
     * @param xml           the XML string to validate
     * @param defaultXml    the default value to use if {@code xml} is {@code null} or not valid
     * @return a valid XML string
     */
    public static String getValidXML(@Nullable String xml, @Nullable String defaultXml) {
        return api().getValidXML(xml, defaultXml);
    }

    /**
     * Encodes a source string for HTML element content.
     * DO NOT USE FOR WRITING ATTRIBUTE VALUES!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForHTML(@Nullable String source) {
        return api().encodeForHTML(source);
    }

    /**
     * Encodes a source string for writing to an HTML attribute value.
     * DO NOT USE FOR ACTIONABLE ATTRIBUTES (href, src, event handlers); YOU MUST USE A VALIDATOR FOR THOSE!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForHTMLAttr(@Nullable String source) {
        return api().encodeForHTMLAttr(source);
    }

    /**
     * Encodes a source string for XML element content.
     * DO NOT USE FOR WRITING ATTRIBUTE VALUES!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForXML(@Nullable String source) {
        return api().encodeForXML(source);
    }

    /**
     * Encodes a source string for writing to an XML attribute value.
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForXMLAttr(@Nullable String source) {
        return api().encodeForXMLAttr(source);
    }

    /**
     * Encodes a source string for writing to JavaScript string content.
     * DO NOT USE FOR WRITING TO ARBITRARY JAVASCRIPT; YOU MUST USE A VALIDATOR FOR THAT.
     * (Encoding only ensures that the source material cannot break out of its context.)
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForJSString(@Nullable String source) {
        return api().encodeForJSString(source);
    }

    /**
     * Encodes a source string for writing to CSS string content.
     * DO NOT USE FOR WRITING OUT ARBITRARY CSS TOKENS; YOU MUST USE A VALIDATOR FOR THAT!
     * (Encoding only ensures the source string cannot break out of its context.)
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    @Nullable
    public static String encodeForCSSString(@Nullable String source) {
        return api().encodeForCSSString(source);
    }

    /**
     * Filters potentially user-contributed HTML to meet the AntiSamy policy rules currently in
     * effect for HTML output (see the XSSFilter service for details).
     *
     * @param source a string containing the source HTML
     * @return a string containing the sanitized HTML which may be an empty string if {@code source} is {@code null} or empty
     */
    @NotNull
    public static String filterHTML(@Nullable String source) {
        return api().filterHTML(source);
    }

    //
    // XSSFilter
    //

    /**
     * Indicates whether or not a given source string contains XSS policy violations.
     *
     * @param context context to use for checking
     * @param src     source string
     * @return true if the source is violation-free
     * @throws NullPointerException if context is <code>null</code>
     */
    public static boolean check(ProtectionContext context, String src) {
        return filter().check(context, src);
    }

    /**
     * Prevents the given source string from containing XSS stuff.
     * <p>
     * The default protection context is used for checking.
     *
     * @param src source string
     * @return string that does not contain XSS stuff
     */
    public static String filter(String src) {
        return filter().filter(src);
    }

    /**
     * Prevents the given source strings from containing XSS stuff.
     * <p>
     * The default protection context is used for checking.
     *
     * @param src array of source strings
     * @return array of strings that does not contain XSS stuff
     */
    public static String[] filter(String[] src) {
        if (src != null) {
            for (int i = 0; i < src.length; i++) {
                src[i] = filter(src[i]);
            }
        }
        return src;
    }

    /**
     * Protects the given source string from containing XSS stuff.
     *
     * @param context context to use for checking
     * @param src     source string
     * @return string that does not contain XSS stuff
     * @throws NullPointerException if context is <code>null</code>
     */
    public static String filter(String contextName, String src) {
        ProtectionContext context = ProtectionContext.fromName(contextName);
        return context != null ? filter().filter(src) : filter().filter(context, src);
    }

    /**
     * Protects the given source string from containing XSS stuff.
     *
     * @param context context to use for checking
     * @param src     source string
     * @return string that does not contain XSS stuff
     * @throws NullPointerException if context is <code>null</code>
     */
    public static String filter(ProtectionContext context, String src) {
        return filter().filter(context, src);
    }

    /**
     * Checks if the given URL is valid to be used for the <code>href</code> attribute in a <code>a</code> tag.
     * <p>
     * The default protection context is used for checking.
     *
     * @param url the URL that should be validated
     * @return true if the URL is violation-free
     */
    public static boolean isValidHref(String url) {
        return filter().isValidHref(url);
    }
}
