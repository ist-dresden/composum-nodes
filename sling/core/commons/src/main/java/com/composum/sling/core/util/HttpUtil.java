package com.composum.sling.core.util;

import org.apache.sling.api.servlets.HttpConstants;

import java.util.Calendar;

/**
 * A basic class for all '/bin/{service}/path/to/resource' servlets.
 */
public class HttpUtil extends HttpConstants {

    public static final String HEADER_LOCATION = "Location";

    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String VALUE_NO_CACHE = "no-cache";

    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";

    public static final String HEADER_VARY = "Vary";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    /**
     * returns 'true' if 'lastModified' is after 'ifModifiedSince' (and both values are valid)
     */
    public static boolean isModifiedSince(long ifModifiedSince, Calendar lastModified) {
        return lastModified != null && isModifiedSince(ifModifiedSince, lastModified.getTimeInMillis());
    }

    public static boolean isModifiedSince(long ifModifiedSince, Long lastModified) {
        if (ifModifiedSince != -1 && lastModified != null) {
            // ignore millis because 'ifModifiedSince' comes often without millis
            long lastModifiedTime = lastModified / 1000L * 1000L;
            ifModifiedSince = ifModifiedSince / 1000L * 1000L;
            return lastModifiedTime > ifModifiedSince;
        }
        return true;
    }
}
