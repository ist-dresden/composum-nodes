package com.composum.sling.nodes.scene;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.CoreConstants;
import com.composum.sling.core.util.ValueEmbeddingReader;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.scene.SceneConfigurations.Config;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Scene {

    private static final Logger LOG = LoggerFactory.getLogger(Scene.class);

    @NotNull
    protected final BeanContext context;
    @NotNull
    protected final Config sceneConfig;
    @NotNull
    protected final Resource configResource;
    @NotNull
    protected final String scenePath;
    @NotNull
    protected final String contentPath;
    @NotNull
    protected final String contentType;

    private transient Map<String, Object> sceneProperties;
    private transient Resource contentResource;
    private transient String frameUrl;
    private transient String elementPath;

    public Scene(@NotNull final BeanContext context,
                 @NotNull final Config sceneConfig,
                 @NotNull final String contentType) {
        this.context = context;
        this.sceneConfig = sceneConfig;
        this.contentType = contentType;
        final ResourceResolver resolver = this.context.getResolver();
        this.configResource = Objects.requireNonNull(resolver.getResource(this.sceneConfig.getPath()));
        String scenesRoot = sceneConfig.getScenesRoot();
        if (StringUtils.isBlank(scenesRoot)) {
            final NodesConfiguration nodesConfig = context.getService(NodesConfiguration.class);
            scenesRoot = nodesConfig.getScenesContentRoot();
        }
        this.scenePath = scenesRoot + (contentType.startsWith("/") ? contentType : "/mnt/overlay/" + contentType);
        this.contentPath = this.scenePath + "/_/" + this.sceneConfig.getKey();
    }

    @NotNull
    public String getFrameUrl(String toolId) {
        if (frameUrl == null) {
            frameUrl = "";
            if (isContentPrepared()) {
                Config.Tool tool = getConfig().getTool(toolId);
                if (tool != null) {
                    String uri = tool.getUri();
                    if (StringUtils.isNotBlank(uri)) {
                        try {
                            frameUrl = IOUtils.toString(
                                    new ValueEmbeddingReader(new StringReader(uri), getSceneProperties()));
                        } catch (IOException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                }
                if (StringUtils.isBlank(frameUrl)) {
                    frameUrl = contentPath + ".html";
                }
            } else {
                String placeholder = getConfig().getPlaceholder();
                if (StringUtils.isNotBlank(placeholder)) {
                    try {
                        frameUrl = IOUtils.toString(
                                new ValueEmbeddingReader(new StringReader(placeholder), getSceneProperties()));
                    } catch (IOException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                }
            }
            if (StringUtils.isNotBlank(frameUrl)) {
                frameUrl = context.getRequest().getContextPath() + frameUrl;
            }
        }
        return frameUrl;
    }

    @NotNull
    public Config getConfig() {
        return sceneConfig;
    }

    @NotNull
    public String getElementPath() {
        if (elementPath == null) {
            String contentPath = getContentPath();
            if (StringUtils.isNotBlank(contentPath)) {
                @SuppressWarnings("deprecation")
                Iterator<Resource> found = context.getResolver().findResources(
                        "/jcr:root" + contentPath + "//sample", Query.XPATH);
                if (found.hasNext()) {
                    elementPath = found.next().getPath();
                }
            }
            if (elementPath == null) {
                elementPath = "";
            }
        }
        return elementPath;
    }

    @NotNull
    public String getContentPath() {
        return contentPath;
    }

    public boolean isContentPrepared() {
        return !ResourceUtil.isNonExistingResource(getContentResource());
    }

    @NotNull
    public Resource getContentResource() {
        if (contentResource == null) {
            final ResourceResolver resolver = context.getResolver();
            contentResource = resolver.getResource(contentPath);
            if (contentResource == null) {
                contentResource = resolver.resolve(contentPath);
            }
        }
        return contentResource;
    }

    @Nullable
    protected Resource getContentTemplate() {
        final ResourceResolver resolver = context.getResolver();
        final ValueMap configValues = configResource.getValueMap();
        String templatePath = configValues.get("template", String.class);
        return templatePath != null ? resolver.getResource(templatePath) : null;
    }

    protected Map<String, Object> getSceneProperties() {
        if (sceneProperties == null) {
            sceneProperties = new HashMap<>();
            sceneProperties.put("path", contentPath);
            sceneProperties.put("name", sceneConfig.getKey());
            sceneProperties.put("type", contentType);
        }
        return sceneProperties;
    }

    @Nullable
    public Resource prepareContent(final boolean resetContent)
            throws IOException {
        final ResourceResolver resolver = context.getResolver();
        final Resource sceneResource = giveSceneContent(resolver, scenePath + "/_");
        final Resource contentTemplate = getContentTemplate();
        return contentTemplate != null
                ? applyTemplate(resolver, getSceneProperties(),
                sceneResource, this.sceneConfig.getKey(),
                contentTemplate, resetContent)
                : null;
    }

    protected Resource applyTemplate(@NotNull final ResourceResolver resolver,
                                     @NotNull final Map<String, Object> properties,
                                     @NotNull final Resource contentParent,
                                     @NotNull final String name,
                                     @NotNull final Resource template,
                                     final boolean resetContent)
            throws IOException {
        Resource content = contentParent.getChild(name);
        if (content == null) {
            final ValueMap templateValues = template.getValueMap();
            final ModifiableValueMap contentValues = new ModifiableValueMapDecorator(new HashMap<>());
            contentValues.put(JcrConstants.JCR_PRIMARYTYPE, templateValues.get(JcrConstants.JCR_PRIMARYTYPE));
            applyProperties(properties, contentValues, templateValues);
            content = resolver.create(contentParent, name, contentValues);
        } else {
            if (!resetContent) {
                return content;
            }
            final ModifiableValueMap contentValues = content.adaptTo(ModifiableValueMap.class);
            if (contentValues == null) {
                throw new PersistenceException("can't modify '" + content.getPath() + "'");
            }
            applyProperties(properties, contentValues, template.getValueMap());
            for (Resource contentChild : content.getChildren()) {
                if (template.getChild(contentChild.getName()) == null) {
                    resolver.delete(contentChild);
                }
            }
        }
        for (Resource templateChild : template.getChildren()) {
            applyTemplate(resolver, properties, content, templateChild.getName(), templateChild, resetContent);
        }
        return content;
    }

    StringFilter PROPERTY_FILTER = new StringFilter.BlackList(
            "^jcr:(primaryType|created.*|uuid)$",
            "^jcr:(baseVersion|predecessors|versionHistory|isCheckedOut)$"
    );

    protected void applyProperties(@NotNull final Map<String, Object> properties,
                                   @NotNull final ModifiableValueMap content,
                                   @NotNull final ValueMap template)
            throws IOException {
        List<String> forRemoval = new ArrayList<>();
        for (String name : content.keySet()) {
            if (PROPERTY_FILTER.accept(name) && template.get(name) == null) {
                forRemoval.add(name);
            }
        }
        for (String name : forRemoval) {
            content.remove(name);
        }
        for (String name : template.keySet()) {
            if (PROPERTY_FILTER.accept(name)) {
                Object value = template.get(name);
                if (value instanceof String) {
                    value = IOUtils.toString(new ValueEmbeddingReader(new StringReader((String) value), properties));
                } else if (value instanceof String[]) {
                    String[] values = (String[]) value;
                    for (int i = 0; i < values.length; i++) {
                        values[i] = IOUtils.toString(new ValueEmbeddingReader(new StringReader(values[i]), properties));
                    }
                }
                content.put(name, value);
            }
        }
    }

    protected static final Map<String, Object> CRUD_SCENE_FOLDER = new HashMap<String, Object>() {{
        put(JcrConstants.JCR_PRIMARYTYPE, CoreConstants.TYPE_SLING_FOLDER);
    }};

    protected Resource giveSceneContent(@NotNull final ResourceResolver resolver,
                                        @NotNull String path)
            throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            String name = StringUtils.substringAfterLast(path, "/");
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            Resource parent = giveSceneContent(resolver, parentPath);
            resource = resolver.create(parent, name, CRUD_SCENE_FOLDER);
        }
        return resource;
    }
}
