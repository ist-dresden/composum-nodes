package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;

/**
 * A basic class for all '/bin/{service}/path/to/resource' servlets.
 */
public class RequestUtil extends org.apache.sling.api.request.RequestUtil {

    public static Session getSession(SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        return session;
    }

    /**
     * Returns the enum value of the requests extension if appropriate otherwise the default value.
     *
     * @param request      the request object with the extension info
     * @param defaultValue the default enum value
     * @param <T>          the enum type derived from the default value
     */
    public static <T extends Enum> T getExtension(SlingHttpServletRequest request, T defaultValue) {
        String extension = request.getRequestPathInfo().getExtension();
        if (extension != null) {
            Class type = defaultValue.getClass();
            try {
                T value = (T) T.valueOf(type, extension.toLowerCase());
                return value;
            } catch (IllegalArgumentException iaex) {
                // ok, use default
            }
        }
        return defaultValue;
    }

    /**
     * Returns an enum value from selectors if an appropriate selector
     * can be found otherwise the default value given.
     *
     * @param request      the request object with the selector info
     * @param defaultValue the default enum value
     * @param <T>          the enum type derived from the default value
     */
    public static <T extends Enum> T getSelector(SlingHttpServletRequest request, T defaultValue) {
        String[] selectors = request.getRequestPathInfo().getSelectors();
        Class type = defaultValue.getClass();
        for (String selector : selectors) {
            try {
                T value = (T) T.valueOf(type, selector);
                return value;
            } catch (IllegalArgumentException iaex) {
                // ok, try next
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a key in the selectors and returns 'true' is the key is present.
     *
     * @param request the request object with the selector info
     * @param key     the selector key which is checked
     */
    public static boolean checkSelector(SlingHttpServletRequest request, String key) {
        String[] selectors = request.getRequestPathInfo().getSelectors();
        for (String selector : selectors) {
            if (selector.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a number in the selectors and returns it if present otherwise the default value.
     *
     * @param request      the request object with the selector info
     * @param defaultValue the default number value
     */
    public static int getIntSelector(SlingHttpServletRequest request, int defaultValue) {
        String[] selectors = request.getRequestPathInfo().getSelectors();
        for (String selector : selectors) {
            try {
                int value = Integer.parseInt(selector);
                return value;
            } catch (NumberFormatException nfex) {
                // ok, try next
            }
        }
        return defaultValue;
    }

    //
    // parameter getters
    //

    public static String getParameter(RequestParameterMap parameters, String name, String defaultValue) {
        RequestParameter parameter = parameters.getValue(name);
        String string;
        return parameter != null && StringUtils.isNotBlank(string = parameter.getString())
                ? string : defaultValue;
    }

    public static String getParameter(SlingHttpServletRequest request, String name, String defaultValue) {
        String result = request.getParameter(name);
        return result != null && StringUtils.isNotBlank(result) ? result : defaultValue;
    }

    public static Integer getParameter(RequestParameterMap parameters, String name, Integer defaultValue) {
        Integer result = defaultValue;
        RequestParameter parameter = parameters.getValue(name);
        String string;
        if (parameter != null && StringUtils.isNotBlank(string = parameter.getString())) {
            try {
                result = Integer.parseInt(string);
            } catch (NumberFormatException nfex) {
                // ok, use default value
            }
        }
        return result;
    }

    public static Integer getParameter(SlingHttpServletRequest request, String name, Integer defaultValue) {
        Integer result = defaultValue;
        String string = request.getParameter(name);
        if (StringUtils.isNotBlank(string)) {
            try {
                result = Integer.parseInt(string);
            } catch (NumberFormatException nfex) {
                // ok, use default value
            }
        }
        return result;
    }

    public static Boolean getParameter(SlingHttpServletRequest request, String name, Boolean defaultValue) {
        Boolean result = null;
        String string = request.getParameter(name);
        if (string != null) {
            result = StringUtils.isBlank(string) || name.equals(string) || Boolean.parseBoolean(string);
        }
        return result != null ? result : defaultValue;
    }

    public static <T extends Enum> T getParameter(RequestParameterMap parameters, String name, T defaultValue) {
        T result = null;
        RequestParameter parameter = parameters.getValue(name);
        String string;
        if (parameter != null && StringUtils.isNotBlank(string = parameter.getString())) {
            try {
                result = (T) T.valueOf(defaultValue.getClass(), string);
            } catch (IllegalArgumentException iaex) {
                // ok, use default value
            }
        }
        return result != null ? result : defaultValue;
    }

    public static <T extends Enum> T getParameter(SlingHttpServletRequest request, String name, T defaultValue) {
        T result = null;
        String string = request.getParameter(name);
        if (StringUtils.isNotBlank(string)) {
            try {
                result = (T) T.valueOf(defaultValue.getClass(), string);
            } catch (IllegalArgumentException iaex) {
                // ok, use default value
            }
        }
        return result != null ? result : defaultValue;
    }
}
