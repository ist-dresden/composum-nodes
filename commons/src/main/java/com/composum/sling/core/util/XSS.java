package com.composum.sling.core.util;

import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.XSSFilter;

import javax.annotation.Nonnull;

/**
 * the static access for the Sling XSSAPI - use XSS.api()...
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

    private XSS() {
    }
}
