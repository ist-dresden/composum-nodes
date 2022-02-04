package com.composum.sling.nodes.mount.remote;

import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class RemoteResource extends SyntheticResource {

    public static class NonExisting extends RemoteResource {

        public NonExisting(@NotNull final RemoteResolver resolver, @NotNull String path) {
            super(resolver, path);
        }

        @NotNull
        @Override
        public String getResourceType() {
            return Resource.RESOURCE_TYPE_NON_EXISTING;
        }
    }

    protected RemoteResolver resolver;

    protected final String path;
    protected final String name;

    protected ValueMap values = new ValueMapDecorator(new TreeMap<>());
    protected Map<String, Resource> children = null;

    protected ModifiableValueMap modifiedValues;

    protected ResourceMetadata metadata = new ResourceMetadata();

    public RemoteResource(@NotNull final RemoteResolver resolver, @NotNull String path) {
        // We set a synthetic resource as super, to not implement Resource directly, which is a ProviderType and should not be implemented by custom code
        super(resolver, path, "remote:Resource");
        this.resolver = resolver;
        if (StringUtils.isBlank(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("an absolute path is required (" + path + ")");
        }
        if ("/".equals(path)) {
            this.path = path;
            this.name = path;
        } else {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            this.path = path;
            this.name = StringUtils.substringAfterLast(path, "/");
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("name not resolvable from '" + path + "'");
            }
        }
    }

    protected RemoteResource(@NotNull final RemoteResource template, @NotNull final String path) {
        this(template.resolver, path);
        this.values = new ValueMapDecorator(new HashMap<>(template.values));
        this.children = new LinkedHashMap<>();
        for (Map.Entry<String, Resource> entry : template.children().entrySet()) {
            String name = entry.getKey();
            this.children.put(name, new RemoteResource((RemoteResource) entry.getValue(), path + "/" + name));
        }
    }

    @Override
    public Resource getResource() {
        return this;
    }

    /**
     * the set of children is lazy loaded;
     * initially this set is 'null' which marks the resource as 'not loaded completely';
     *
     * @return the set of children, loaded if not done already
     */
    @NotNull
    protected Map<String, Resource> children() {
        if (children == null) {
            if (resolver.provider.remoteReader.loadResource(this, true) == null) {
                children = new LinkedHashMap<>(); // not readable but a well known child - make it valid
            }
        }
        return children;
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public Resource getParent() {
        return resolver.getParent(this);
    }

    @Override
    public boolean hasChildren() {
        return children().size() > 0;
    }

    @NotNull
    @Override
    public Iterator<Resource> listChildren() {
        return children().values().iterator();
    }

    @NotNull
    @Override
    public Iterable<Resource> getChildren() {
        return children().values();
    }

    @Nullable
    @Override
    public Resource getChild(@NotNull String relPath) {
        if (relPath.contains("/")) {
            return resolver.getResource(this, relPath);
        }
        return children().get(relPath);
    }

    @NotNull
    @Override
    public String getResourceType() {
        return values.get(ResourceUtil.PROP_RESOURCE_TYPE,
                values.get(ResourceUtil.PROP_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED));
    }

    @Nullable
    @Override
    public String getResourceSuperType() {
        return values.get(ResourceUtil.PROP_RESOURCE_SUPER_TYPE, String.class);
    }

    @Override
    public boolean isResourceType(String resourceType) {
        boolean result = false;
        if (StringUtils.isNotBlank(resourceType)) {
            result = resourceType.equals(getResourceType());
            if (!result) {
                result = resourceType.equals(getResourceSuperType());
            }
        }
        return result;
    }

    @NotNull
    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @NotNull
    @Override
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @NotNull
    @Override
    public ValueMap getValueMap() {
        return values;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(@NotNull Class<AdapterType> type) {
        if (ModifiableValueMap.class.equals(type)) {
            if (modifiedValues == null) {
                modifiedValues = new ModifiableValueMapDecorator(new HashMap<>(values));
                resolver.getChangeSet().addModify(this);
            }
            return (AdapterType) modifiedValues;
        }
        return null;
    }
}
