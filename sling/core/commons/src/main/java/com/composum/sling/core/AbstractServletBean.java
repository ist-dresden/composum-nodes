package com.composum.sling.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractServletBean extends AbstractSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractServletBean.class);

    public AbstractServletBean(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public AbstractServletBean(BeanContext context) {
        super(context);
    }

    public AbstractServletBean() {
        super();
    }

    /**
     * extract the resource referenced to display in the browsers view as the components resource
     */
    @Override
    public void initialize(BeanContext context) {
        SlingHttpServletRequest request = context.getAttribute("slingRequest", SlingHttpServletRequest.class);
        if (request == null) {
            request = context.getAttribute("request", SlingHttpServletRequest.class);
        }
        String path = null;
        Resource resource = context.getAttribute("resource", Resource.class);
        if (resource != null) {
            path = resource.getPath();
        }
        if (StringUtils.isBlank(path) || path.startsWith("/bin/")) {
            RequestPathInfo requestPathInfo = request.getRequestPathInfo();
            path = requestPathInfo.getSuffix();
        }
        if (StringUtils.isNotBlank(path)) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            ResourceResolver resolver = request.getResourceResolver();
            resource = resolver.getResource(path);
        }
        if (resource == null) {
            resource = resolver.getResource("/");
        }
        initialize(context, ResourceHandle.use(resource));
    }
}
