package com.composum.sling.nodes.service.impl;

import com.composum.sling.nodes.service.ComponentsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class NodesComponentsService implements ComponentsService {

    @Override
    @Nullable
    public Resource createOverlay(@Nonnull final ResourceResolver resolver, @Nonnull final String overlayType)
            throws PersistenceException {
        Resource overlay = null;
        String overlayPath = null;
        Resource template = null;
        String[] searchPath = resolver.getSearchPath();
        for (int i = searchPath.length - 1; overlayPath == null && --i >= 0; ) {
            if (overlayType.startsWith("/")) {
                if (overlayType.startsWith(searchPath[i])) {
                    template = resolver.getResource(searchPath[i + 1] + overlayType.substring(searchPath[i].length()));
                    if (template != null) {
                        overlayPath = overlayType;
                    }
                }
            } else {
                template = resolver.getResource(searchPath[i + 1] + overlayType);
                if (template != null && (resolver.getResource(searchPath[i] + overlayType) == null || i == 0)) {
                    overlayPath = searchPath[i] + overlayType;
                }
            }
        }
        if (template != null && StringUtils.isNotBlank(overlayPath)) {
            overlay = resolver.getResource(overlayPath);
            if (overlay != null) {
                resolver.delete(overlay);
            }
            Resource parent = prepareParent(resolver, template.getParent(),
                    StringUtils.substringBeforeLast(overlayPath, "/"));
            if (parent != null) {
                overlay = copyTemplate(resolver, template, parent);
            }
        }
        return overlay;
    }

    @Override
    public boolean removeOverlay(@Nonnull final ResourceResolver resolver, @Nonnull final String overlayType)
            throws PersistenceException {
        Resource overlay = null;
        String[] searchPath = resolver.getSearchPath();
        for (int i = 0; overlay == null && i + 1 < searchPath.length; i++) {
            if (overlayType.startsWith("/")) {
                if (overlayType.startsWith(searchPath[i])) {
                    overlay = resolver.getResource(overlayType);
                }
            } else {
                overlay = resolver.getResource(searchPath[i] + overlayType);
            }
        }
        if (overlay != null) {
            resolver.delete(overlay);
            return true;
        }
        return false;
    }

    public static final List<String> IGNORED_PROPERTIES = new ArrayList<String>() {{
        add(JcrConstants.JCR_UUID);
        add(JcrConstants.JCR_CREATED);
        add(JcrConstants.JCR_CREATED + "By");
        add(JcrConstants.JCR_ISCHECKEDOUT);
        add(JcrConstants.JCR_VERSIONHISTORY);
        add(JcrConstants.JCR_BASEVERSION);
        add(JcrConstants.JCR_PREDECESSORS);
    }};

    protected Resource copyTemplate(@Nonnull final ResourceResolver resolver,
                                    @Nonnull final Resource template, @Nonnull final Resource parent)
            throws PersistenceException {
        ValueMap values = template.getValueMap();
        Resource resource = resolver.create(parent, template.getName(), new HashMap<String, Object>() {{
            for (String key : values.keySet()) {
                if (!IGNORED_PROPERTIES.contains(key)) {
                    put(key, values.get(key));
                }
            }
        }});
        for (Resource child : template.getChildren()) {
            copyTemplate(resolver, child, resource);
        }
        return resource;
    }

    protected Resource prepareParent(@Nonnull final ResourceResolver resolver,
                                     final Resource template, final String targetPath)
            throws PersistenceException {
        Resource resource = resolver.getResource(targetPath);
        if (resource == null && template != null) {
            String parentPath = StringUtils.substringBeforeLast(targetPath, "/");
            Resource parent = prepareParent(resolver, template.getParent(), parentPath);
            ValueMap values = template.getValueMap();
            resource = resolver.create(parent, template.getName(), new HashMap<String, Object>() {{
                put(JcrConstants.JCR_PRIMARYTYPE,
                        values.get(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
            }});
        }
        return resource;
    }
}
