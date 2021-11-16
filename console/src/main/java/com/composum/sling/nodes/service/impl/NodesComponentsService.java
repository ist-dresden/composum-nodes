package com.composum.sling.nodes.service.impl;

import com.composum.sling.nodes.service.ComponentsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Component
public class NodesComponentsService implements ComponentsService {

    @Override
    @Nullable
    public Resource createOverlay(@NotNull final ResourceResolver resolver, @NotNull final String overlayType)
            throws PersistenceException {
        Resource overlay = null;
        String overlayPath = null;
        Resource template = null;
        List<String> searchPath = Arrays.asList(resolver.getSearchPath());
        Iterator<String> searchPathIterator = searchPath.iterator();
        if (overlayType.startsWith("/")) {
            template = getTemplate(resolver, overlayType, searchPathIterator);
            if (template != null) {
                overlayPath = overlayType;
            } else {
                List<String> reversePath = new ArrayList<>(searchPath);
                Collections.reverse(reversePath);
                template = getTemplate(resolver, overlayType, reversePath.iterator());
                if (template != null) {
                    overlayPath = overlayType;
                }
            }
        } else { // resource type - search search path for last entry that doesn't exist yet
            String lastEntry = null;
            String currentEntry = null;
            while (searchPathIterator.hasNext() && template == null) {
                lastEntry = currentEntry;
                currentEntry = searchPathIterator.next();
                template = resolver.getResource(currentEntry + overlayType);
            }
            overlayPath = lastEntry != null ? lastEntry + overlayType : null;
        }
        if (template != null && StringUtils.isNotBlank(overlayPath)) {
            overlay = resolver.getResource(overlayPath);
            if (overlay != null) { // safety check - do not create something without explicitly deleting it first.
                throw new IllegalArgumentException("Overlay already exists");
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
    public boolean removeOverlay(@NotNull final ResourceResolver resolver, @NotNull final String overlayType)
            throws PersistenceException {
        Resource overlay = null;
        for (String searchPathElement : resolver.getSearchPath()) {
            if (overlayType.startsWith("/")) {
                if (overlayType.startsWith(searchPathElement)) {
                    overlay = resolver.getResource(overlayType);
                }
            } else {
                overlay = resolver.getResource(searchPathElement + overlayType);
            }
        }
        if (overlay != null) {
            resolver.delete(overlay);
            return true;
        }
        return false;
    }

    protected Resource getTemplate(@NotNull final ResourceResolver resolver,
                                   @NotNull final String overlayType,
                                   @NotNull final Iterator<String> searchPathIterator) {
        String overlayPath = null;
        Resource template = null;
        String overlayEntry = null;
        while (searchPathIterator.hasNext() && overlayPath == null) {
            String thisEntry = searchPathIterator.next();
            if (overlayType.startsWith(thisEntry)) {
                overlayEntry = thisEntry;
                overlayPath = overlayType;
            }
        }
        while (searchPathIterator.hasNext() && template == null) {
            template = resolver.getResource(searchPathIterator.next() + overlayType.substring(overlayEntry.length()));
        }
        return template;
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

    protected Resource copyTemplate(@NotNull final ResourceResolver resolver,
                                    @NotNull final Resource template, @NotNull final Resource parent)
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

    protected Resource prepareParent(@NotNull final ResourceResolver resolver,
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
