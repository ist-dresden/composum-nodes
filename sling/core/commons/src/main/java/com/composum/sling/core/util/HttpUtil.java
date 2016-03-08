package com.composum.sling.core.util;

import org.apache.sling.api.servlets.HttpConstants;

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
}
