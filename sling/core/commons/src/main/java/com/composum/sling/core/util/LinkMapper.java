package com.composum.sling.core.util;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

public interface LinkMapper {

    String LINK_MAPPER_REQUEST_ATTRIBUTE = LinkMapper.class.getName() + "_linkMapper";

    String mapUri(SlingHttpServletRequest request, String uri);

    ContextMapper CONTEXT = new ContextMapper();
    ResolverMapper RESOLVER = new ResolverMapper();

    class ContextMapper implements LinkMapper {

        public String mapUri(SlingHttpServletRequest request, String uri) {
            return LinkUtil.isExternalUrl(uri) ? uri : request.getContextPath() + uri;
        }
    }

    class ResolverMapper implements LinkMapper {

        public String mapUri(SlingHttpServletRequest request, String uri) {
            ResourceResolver resolver = request.getResourceResolver();
            return LinkUtil.isExternalUrl(uri) ? uri : resolver.map(request, uri);
        }
    }
}
