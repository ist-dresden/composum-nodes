package com.composum.sling.core.util;

import com.composum.sling.core.RequestBundle;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;

/**
 * the static access for general I18N translation
 */
public class I18N {

    private static final Logger LOG = LoggerFactory.getLogger(I18N.class);

    public static String get(SlingHttpServletRequest request, String text) {
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

    private I18N() {
    }
}
