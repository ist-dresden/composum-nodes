package com.composum.sling.core.util;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.http.HttpServletRequest;

public interface LinkMapper {

    String LINK_MAPPER_REQUEST_ATTRIBUTE = LinkMapper.class.getName() + "_linkMapper";

    /**
     * If uri is an URL, it is returned unmodified; if it's a path, it's mapped to an URL without scheme, but validly URL-encoded.
     * we have to say that it returns it URL-encoded since {@link ResourceResolver#map(HttpServletRequest, String)} encodes
     * characters not valid in an URL.
     */
    String mapUri(SlingHttpServletRequest request, String uri);

    ContextMapper CONTEXT = new ContextMapper();
    ResolverMapper RESOLVER = new ResolverMapper();

    class ContextMapper implements LinkMapper {

        @Override
        public String mapUri(SlingHttpServletRequest request, String uri) {
            return LinkUtil.isExternalUrl(uri) ? uri : request.getContextPath() + LinkUtil.encodePath(uri);
        }
    }

    class ResolverMapper implements LinkMapper {

        @Override
        public String mapUri(SlingHttpServletRequest request, String uri) {
            ResourceResolver resolver = request.getResourceResolver();
            return LinkUtil.isExternalUrl(uri) ? uri : resolver.map(request, uri);
        }
    }
}
