package com.composum.sling.core.servlet;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 *
 */
public interface ServletOperation {

    void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
              ResourceHandle resource)
            throws RepositoryException, IOException, ServletException;
}

