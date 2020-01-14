package com.composum.sling.core.util;

import com.composum.sling.core.RequestBundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.MissingResourceException;

/**
 * the static access for general I18N translation
 */
public class I18N {

    private static final Logger LOG = LoggerFactory.getLogger(I18N.class);

    public static String get(@Nonnull SlingHttpServletRequest request, String text) {
        String translated = null;
        try {
            translated = RequestBundle.get(request).getString(text);
            if (StringUtils.isBlank(translated)) {
                LOG.warn("Suspicious translation to blank string ignored for '{}' locale {}", text,
                        request.getLocale());
                translated = null;
            }
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
