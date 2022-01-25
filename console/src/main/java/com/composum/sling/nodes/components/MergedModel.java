package com.composum.sling.nodes.components;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.nodes.console.ConsoleServletBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MergedModel extends ConsoleServletBean {

    public static final String METADATA_MERGED = "sling.mergedResource";
    public static final String METADATA_RESOURCES = "sling.mergedResources";

    private transient Boolean mergedResource;
    private transient List<Resource> mergedResources;
    private transient List<String> mergedPaths;

    public MergedModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public MergedModel(BeanContext context) {
        super(context);
    }

    public MergedModel() {
        super();
    }

    @Override
    @NotNull
    public ResourceHandle getResource() {
        Resource resource = null;
        final SlingHttpServletRequest request = context.getRequest();
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String suffix = pathInfo.getSuffix();
        if (StringUtils.isNotBlank(suffix)) {
            resource = request.getResourceResolver().getResource(suffix);
        }
        return resource != null ? ResourceHandle.use(resource) : super.getResource();
    }

    public static boolean isMergedResource(@NotNull final Resource resource) {
        return Boolean.TRUE.equals(resource.getResourceMetadata().get(METADATA_MERGED));
    }

    public boolean isMergedResource() {
        if (mergedResource == null) {
            mergedResource = isMergedResource(getResource());
        }
        return mergedResource;
    }

    public @NotNull List<Resource> getMergedResources() {
        if (mergedResources == null) {
            final ResourceResolver resolver = getResolver();
            mergedResources = new ArrayList<>();
            for (final String path : getMergedPaths()) {
                final Resource resource = resolver.getResource(path);
                if (resource != null) {
                    mergedResources.add(0, resource);
                }
            }
        }
        return mergedResources;
    }

    public @NotNull List<String> getMergedPaths() {
        if (mergedPaths == null) {
            final String[] paths = (String[]) getResource().getResourceMetadata().get(METADATA_RESOURCES);
            if (paths != null) {
                mergedPaths = Arrays.asList(paths);
            } else {
                mergedPaths = Collections.emptyList();
            }
        }
        return mergedPaths;
    }
}
