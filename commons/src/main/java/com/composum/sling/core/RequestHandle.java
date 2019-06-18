/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: ResourceHandle.java
 * Autor: Ralf Wunsch, Mirko Zeibig
 */
package com.composum.sling.core;

import com.composum.sling.core.filter.StringFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

/**
 * Wrapper that extends the functionality of {@link SlingHttpServletRequest}.
 */
public class RequestHandle extends SlingHttpServletRequestWrapper {

    public static RequestHandle use(SlingHttpServletRequest request) {
        return request instanceof RequestHandle ? ((RequestHandle) request) : new RequestHandle(request);
    }

    /**
     * Creates a new wrapper instance delegating all method calls to the given <code>request</code>.
     *
     * @param request the request to wrap
     */
    protected RequestHandle(SlingHttpServletRequest request) {
        super(request);
    }

    /**
     * Returns all request selectors with a leading '.' if selectors are present.
     */
    public String getSelectors() {
        return getSelectors(StringFilter.ALL);
    }

    public String getSelectors(StringFilter filter) {
        return getSelectors(null, filter, null);
    }

    public String getSelectors(String[] prepend, StringFilter filter, String[] append) {
        String[] selectors = getSlingRequest().getRequestPathInfo().getSelectors();
        StringBuilder result = new StringBuilder();
        if (prepend != null && prepend.length > 0) {
            for (String s : prepend) {
                result.append('.');
                result.append(s);
            }
        }
        if (selectors != null && selectors.length > 0) {
            for (String selector : selectors) {
                if (filter.accept(selector)) {
                    result.append('.');
                    result.append(selector);
                }
            }
        }
        if (append != null && append.length > 0) {
            for (String s : append) {
                result.append('.');
                result.append(s);
            }
        }
        return result.toString();
    }

    /**
     * Returns the request extension with a leading '.' if present.
     */
    public String getExtension() {
        String extension = getSlingRequest().getRequestPathInfo().getExtension();
        return StringUtils.isBlank(extension) ? "" : ("." + extension);
    }
}
