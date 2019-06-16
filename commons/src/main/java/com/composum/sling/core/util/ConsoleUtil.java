package com.composum.sling.core.util;

import com.composum.sling.core.BeanContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

public class ConsoleUtil extends SlingSafeMethodsServlet {

    /**
     * determines the addressed resource by the suffix if the requests resource is the servlet itself
     */
    public static Resource getConsoleResource(BeanContext context) {
        SlingHttpServletRequest request = context.getRequest();
        ResourceResolver resolver = request.getResourceResolver();
        String path = null;
        // use the resource set by a probably executed 'defineObjects' tag (supports including components)
        Resource resource = context.getAttribute("resource", Resource.class);
        if (resource == null) {
            resource = request.getResource();
        }
        if (resource != null) {
            path = resource.getPath();
        }
        // use the suffix as the resource path if the resource is not defined or references a servlet
        if (StringUtils.isBlank(path) || path.startsWith("/bin/")) {
            RequestPathInfo requestPathInfo = request.getRequestPathInfo();
            path = requestPathInfo.getSuffix();
            resource = null;
        }
        if (resource == null && StringUtils.isNotBlank(path)) {
            resource = resolver.getResource(path);
        }
        if (resource == null) {
            // fallback to the root node if the servlet request has no suffix
            resource = resolver.getResource("/");
        }
        return resource;
    }
}