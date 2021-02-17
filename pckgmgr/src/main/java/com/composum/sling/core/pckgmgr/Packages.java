package com.composum.sling.core.pckgmgr;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

public interface Packages {

    enum Mode {jcrpckg, regpckg}

    String SA_PCKGMGR_MODE = Packages.class.getName() + "#mode";

    static Mode getMode(SlingHttpServletRequest request) {
        Mode result = Mode.jcrpckg;
        HttpSession session = request.getSession();
        if (session != null) {
            try {
                String fromSession = (String) session.getAttribute(SA_PCKGMGR_MODE);
                if (StringUtils.isNotBlank(fromSession)) {
                    result = Mode.valueOf(fromSession);
                }
            } catch (RuntimeException ignore) {
            }
        }
        return result;
    }

    String REGISTRY_PATH_PREFIX = "@";

    Pattern REGISTRY_PATH = Pattern.compile("^/" + REGISTRY_PATH_PREFIX + "(?<ns>[^/]+)$");

    Pattern REGISTRY_BASED_PATH = Pattern.compile("^/" + REGISTRY_PATH_PREFIX + "(?<ns>[^/]+)(?<path>/.+)?$");

    Pattern PACKAGE_PATH = Pattern.compile("^(/" + REGISTRY_PATH_PREFIX + "(?<ns>[^/]+))?" +
            "(/(?<group>.+))?/(?<name>[^/]+)/(?<version>[^/]+)$");
}
