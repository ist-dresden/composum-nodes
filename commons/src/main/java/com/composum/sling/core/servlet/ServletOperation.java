package com.composum.sling.core.servlet;

import com.composum.sling.core.ResourceHandle;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Implementation of one operation of an {@link AbstractServiceServlet}.
 */
public interface ServletOperation {

    void doIt(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
              @Nullable ResourceHandle resource)
            throws RepositoryException, IOException, ServletException;
}

