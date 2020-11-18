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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Scene {

    private static final Logger LOG = LoggerFactory.getLogger(Scene.class);

    public static final String NAME_PREFIX = "@";

    @Nonnull
    protected final BeanContext context;
    @Nonnull
    protected final Config sceneConfig;
    @Nonnull
    protected final Resource configResource;
    @Nonnull
    protected final String scenePath;
    @Nonnull
    protected final String contentPath;
    @Nonnull
    protected final String contentType;

    private transient Map<String, Object> sceneProperties;
    private transient Resource contentResource;
    private transient String frameUrl;

    public Scene(@Nonnull final BeanContext context,
                 @Nonnull final Config sceneConfig,
                 @Nonnull final String contentType) {
        NodesConfiguration nodesConfig = context.getService(NodesConfiguration.class);
        this.context = context;
        this.sceneConfig = sceneConfig;
        this.contentType = contentType;
        ResourceResolver resolver = this.context.getResolver();
        this.configResource = Objects.requireNonNull(resolver.getResource(this.sceneConfig.getPath()));
        this.scenePath = nodesConfig.getScenesContentRoot()
                + (contentType.startsWith("/") ? contentType : "/mnt/overlay/" + contentType);
        this.contentPath = this.scenePath + "/" + NAME_PREFIX + this.sceneConfig.getKey();
    }

    @Nonnull
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
                frameUrl = context.getRequest().getContextPath() + frameUrl;
            }
        }
        return frameUrl;
    }

    @Nonnull
    public Config getConfig() {
        return sceneConfig;
    }

    @Nonnull
    public String getContentPath() {
        return contentPath;
    }

    public boolean isContentPrepared() {
        return !ResourceUtil.isNonExistingResource(getContentResource());
    }

    @Nonnull
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
        final Resource sceneResource = giveSceneContent(resolver, scenePath);
        final Resource contentTemplate = getContentTemplate();
        return contentTemplate != null
                ? applyTemplate(resolver, getSceneProperties(),
                sceneResource, NAME_PREFIX + this.sceneConfig.getKey(),
                contentTemplate, resetContent)
                : null;
    }

    protected Resource applyTemplate(@Nonnull final ResourceResolver resolver,
                                     @Nonnull final Map<String, Object> properties,
                                     @Nonnull final Resource contentParent,
                                     @Nonnull final String name,
                                     @Nonnull final Resource template,
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

    protected void applyProperties(@Nonnull final Map<String, Object> properties,
                                   @Nonnull final ModifiableValueMap content,
                                   @Nonnull final ValueMap template)
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

    protected Resource giveSceneContent(@Nonnull final ResourceResolver resolver,
                                        @Nonnull String path)
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
