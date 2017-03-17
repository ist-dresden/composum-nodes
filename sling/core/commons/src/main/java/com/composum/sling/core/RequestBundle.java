package com.composum.sling.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class RequestBundle extends ResourceBundle {

    public static final String ATTRIBUTE_KEY = "composum-core-resource-bundles";

    /**
     * returns the requests instance
     */
    public static synchronized RequestBundle get(SlingHttpServletRequest request) {
        RequestBundle instance = (RequestBundle) request.getAttribute(ATTRIBUTE_KEY);
        if (instance == null) {
            instance = new RequestBundle(request);
            request.setAttribute(ATTRIBUTE_KEY, instance);
        }
        return instance;
    }

    protected final SlingHttpServletRequest request;
    protected final List<BundleItem> bundles;

    protected class BundleWrapper extends ResourceBundle {

        protected final ResourceBundle bundle;

        public BundleWrapper(ResourceBundle bundle) {
            this.bundle = bundle;
        }

        public void setParent(ResourceBundle bundle) {
            super.setParent(bundle);
        }

        @Override
        protected Object handleGetObject(String key) {
            return bundle.getObject(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return bundle.getKeys();
        }
    }

    protected class BundleItem {

        public final String basename;
        public final BundleWrapper bundle;

        public int stackDepth = 0;

        public BundleItem(String basename) {
            this.basename = basename;
            Locale locale = request.getLocale();
            bundle = new BundleWrapper(request.getResourceBundle(basename, locale));
        }
    }

    protected RequestBundle(SlingHttpServletRequest request) {
        this.request = request;
        bundles = new ArrayList<>();
        push(null);
    }

    public synchronized void push(String basename) {
        if (bundles.size() > 0) {
            BundleItem last = bundles.get(0);
            if (StringUtils.equals(basename, last.basename)) {
                last.stackDepth++;
            } else {
                pushBundle(basename);
            }
        } else {
            pushBundle(basename);
        }
    }

    protected void pushBundle(String basename) {
        BundleItem item = new BundleItem(basename);
        if (bundles.size() > 0) {
            BundleItem last = bundles.get(0);
            item.bundle.setParent(last.bundle);
        }
        bundles.add(0, item);
    }

    public synchronized void pop() {
        if (bundles.size() > 0) {
            BundleItem last = bundles.get(0);
            if (last.stackDepth > 0) {
                last.stackDepth--;
            } else {
                last.bundle.setParent(null);
                bundles.remove(0);
            }
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        return bundles.get(0).bundle.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return bundles.get(0).bundle.getKeys();
    }
}
