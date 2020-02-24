package com.composum.sling.core.util;

import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.XSSFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * the static access for the Sling XSSAPI / XSSFilter - wraps the Sling XSS services
 */
public class XSS {

    protected static ServiceHandle<XSSAPI> XSSAPI_HANDLE = new ServiceHandle<>(XSSAPI.class);
    protected static ServiceHandle<XSSFilter> XSSFilter_HANDLE = new ServiceHandle<>(XSSFilter.class);

    @Nonnull
    public static XSSAPI api() {
        return XSSAPI_HANDLE.getService();
    }

    @Nonnull
    public static XSSFilter filter() {
        return XSSFilter_HANDLE.getService();
    }

    protected XSS() {
    }

    //
    // XSSAPI
    //

    @Nullable
    public static Integer getValidInteger(@Nullable String integer, int defaultValue) {
        return api().getValidInteger(integer, defaultValue);
    }

    @Nullable
    public static Long getValidLong(@Nullable String source, long defaultValue) {
        return api().getValidLong(source, defaultValue);
    }

    @Nullable
    public static Double getValidDouble(@Nullable String source, double defaultValue) {
        return api().getValidDouble(source, defaultValue);
    }

    @Nullable
    public static String getValidDimension(@Nullable String dimension, @Nullable String defaultValue) {
        return api().getValidDimension(dimension, defaultValue);
    }

    @Nonnull
    public static String getValidHref(@Nullable String url) {
        return api().getValidHref(url);
    }

    @Nullable
    public static String getValidJSToken(@Nullable String token, @Nullable String defaultValue) {
        return api().getValidJSToken(token, defaultValue);
    }

    @Nullable
    public static String getValidStyleToken(@Nullable String token, @Nullable String defaultValue) {
        return api().getValidStyleToken(token, defaultValue);
    }

    @Nullable
    public static String getValidCSSColor(@Nullable String color, @Nullable String defaultColor) {
        return api().getValidCSSColor(color, defaultColor);
    }

    public static String getValidMultiLineComment(@Nullable String comment, @Nullable String defaultComment) {
        return null;
    }

    public static String getValidJSON(@Nullable String json, @Nullable String defaultJson) {
        return api().getValidJSON(json, defaultJson);
    }

    public static String getValidXML(@Nullable String xml, @Nullable String defaultXml) {
        return api().getValidXML(xml, defaultXml);
    }

    @Nullable
    public static String encodeForHTML(@Nullable String source) {
        return api().encodeForHTML(source);
    }

    @Nullable
    public static String encodeForHTMLAttr(@Nullable String source) {
        return api().encodeForHTMLAttr(source);
    }

    @Nullable
    public static String encodeForXML(@Nullable String source) {
        return api().encodeForXML(source);
    }

    @Nullable
    public static String encodeForXMLAttr(@Nullable String source) {
        return api().encodeForXMLAttr(source);
    }

    @Nullable
    public static String encodeForJSString(@Nullable String source) {
        return api().encodeForJSString(source);
    }

    @Nullable
    public static String encodeForCSSString(@Nullable String source) {
        return api().encodeForCSSString(source);
    }

    @Nonnull
    public static String filterHTML(@Nullable String source) {
        return api().filterHTML(source);
    }

    //
    // XSSFilter
    //

    public static boolean check(ProtectionContext context, String src) {
        return filter().check(context, src);
    }

    public static String filter(String src) {
        return filter().filter(src);
    }

    public static String[] filter(String[] src) {
        if (src != null) {
            for (int i = 0; i < src.length; i++) {
                src[i] = filter(src[i]);
            }
        }
        return src;
    }

    public static String filter(String contextName, String src) {
        ProtectionContext context = ProtectionContext.fromName(contextName);
        return context != null ? filter().filter(src) : filter().filter(context, src);
    }

    public static String filter(ProtectionContext context, String src) {
        return filter().filter(context, src);
    }

    public static boolean isValidHref(String url) {
        return filter().isValidHref(url);
    }
}
