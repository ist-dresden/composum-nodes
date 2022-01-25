package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HttpConstants;

import org.jetbrains.annotations.NotNull;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
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
     * Checks whether we can skip transmission of a resource because of a recent enough {@link #HEADER_IF_MODIFIED_SINCE}
     * header.
     * Returns true if the given lastModified date is after the {ifModifiedSince} or if there is no lastModified date,
     * so that we don't know and have to transmit the resource, anyway.
     *
     * @param ifModifiedSince value of the {@link #HEADER_IF_MODIFIED_SINCE} header
     * @param lastModified    date of the resource to be submitted
     * @return if the resource transmission can be skipped since the browser has the current version
     */
    public static boolean notModifiedSince(long ifModifiedSince, Calendar lastModified) {
        return lastModified != null && notModifiedSince(ifModifiedSince, lastModified.getTimeInMillis());
    }

    /**
     * Checks whether we can skip transmission of a resource because of a recent enough {@link #HEADER_IF_MODIFIED_SINCE}
     * header.
     * Returns true if the given lastModified date is after the {ifModifiedSince} or if there is no lastModified date,
     * so that we don't know and have to transmit the resource, anyway.
     *
     * @param ifModifiedSince value of the {@link #HEADER_IF_MODIFIED_SINCE} header
     * @param lastModified    date of the resource to be submitted
     * @return if the resource transmission can be skipped since the browser has the current version
     */
    public static boolean notModifiedSince(long ifModifiedSince, Long lastModified) {
        if (ifModifiedSince != -1 && lastModified != null) {
            // ignore millis because 'ifModifiedSince' comes often without millis
            long lastModifiedTime = lastModified / 1000L * 1000L;
            ifModifiedSince = ifModifiedSince / 1000L * 1000L;
            return lastModifiedTime <= ifModifiedSince;
        }
        return false;
    }

    /**
     * @deprecated please use {@link #notModifiedSince(long, Calendar)} since that's cleaner; this will be removed
     * soon
     */
    // FIXME(hps,15.11.19) remove this
    @Deprecated
    public static boolean isModifiedSince(long ifModifiedSince, Calendar lastModified) {
        return !notModifiedSince(ifModifiedSince, lastModified);
    }

    /**
     * @deprecated please use {@link #notModifiedSince(long, Long)} since that's cleaner; this will be removed
     * soon
     */
    // FIXME(hps,15.11.19) remove this
    @Deprecated
    public static boolean isModifiedSince(long ifModifiedSince, Long lastModified) {
        if (ifModifiedSince != -1 && lastModified != null) {
            // ignore millis because 'ifModifiedSince' comes often without millis
            long lastModifiedTime = lastModified / 1000L * 1000L;
            ifModifiedSince = ifModifiedSince / 1000L * 1000L;
            return lastModifiedTime > ifModifiedSince;
        }
        return true;
    }

    public interface CachableInstance extends Serializable {

        long getCreated();
    }

    public interface InstanceFactory<Type extends CachableInstance> {

        Class<Type> getType();

        Type newInstance(SlingHttpServletRequest request);
    }

    @NotNull
    public static <Type extends CachableInstance> Type getInstance(@NotNull final SlingHttpServletRequest request,
                                                                   @NotNull final String attributeKey,
                                                                   @NotNull final InstanceFactory<Type> factory) {
        final HttpSession session = request.getSession(true);
        Class<Type> type = factory.getType();
        Type instance = null;
        try {
            Object object = session.getAttribute(attributeKey);
            if (type.isInstance(object)) {
                instance = type.cast(object);
            }
        } catch (ClassCastException ignore) {
        }
        if (instance != null) {
            final String cacheControlHeader;
            if (System.currentTimeMillis() - instance.getCreated() > 1800000L /* 30 min */
                    || (StringUtils.isNotBlank(cacheControlHeader = request.getHeader(HttpUtil.HEADER_CACHE_CONTROL))
                    && cacheControlHeader.contains(HttpUtil.VALUE_NO_CACHE))) {
                instance = null;
            }
        }
        if (instance == null) {
            instance = factory.newInstance(request);
            session.setAttribute(attributeKey, instance);
        }
        return instance;
    }
}
