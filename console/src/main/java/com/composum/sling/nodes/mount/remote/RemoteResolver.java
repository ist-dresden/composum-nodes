package com.composum.sling.nodes.mount.remote;

import com.composum.sling.nodes.mount.ExtendedResolver;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * the resolver of the remote Sling instance resources mounted by the resource provider;
 * supports extended CRUD operations for resource ordering and file uploads (updates)
 */
public class RemoteResolver implements ExtendedResolver {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteResolver.class);

    protected final LRUMap resourceCache = new LRUMap(200);

    protected final RemoteProvider provider;
    protected final ResourceResolver parentDelegate;

    protected RemoteWriter.ChangeSet changeSet = new RemoteWriter.ChangeSet();

    public RemoteResolver(@NotNull final RemoteProvider provider,
                          @NotNull final ResourceResolver parentDelegate) {
        this.provider = provider;
        this.parentDelegate = parentDelegate;
    }

    /**
     * @return the 'parent' resource resolver (from the ResolveContext)
     */
    @Nullable
    protected ResourceResolver getParentDelegate() {
        return parentDelegate;
    }

    /**
     * @return the current set of CRUD changes
     */
    protected RemoteWriter.ChangeSet getChangeSet() {
        return changeSet;
    }

    /**
     * Resolves the resource specified by the path and caches the result.
     *
     * @param path the path (the local path) of the mounted remote resource
     * @return the resource, maybe a 'non existing' instance if the resource can't be read
     */
    @Nullable
    protected Resource _resolve(@NotNull final String path) {
        if (provider.ignoreIt(path)) {
            return null;
        }
        try {
            if (provider.localRoot.startsWith(path) && !path.equals(provider.localRoot)) {
                LOG.debug("delegating '{}' to default resolver...", path);
                return parentDelegate.getResource(path);
            }
            String localPath = provider.localPath(path);
            RemoteResource resource = (RemoteResource) resourceCache.get(localPath);
            if (resource != null) {
                LOG.debug("from cache: '{}'", localPath);
                return resource;
            }
            if (!path.equals(localPath)) {
                LOG.warn("reading '{}' ({})...", localPath, path);
            } else {
                LOG.debug("reading '{}'...", localPath);
            }
            RemoteResource parent = null;
            String parentPath = _parentPath(path);
            // try to use a loaded parent to optimize caching
            if (parentPath != null && provider.isLocal(parentPath)) {
                parent = (RemoteResource) getResource(parentPath);
            }
            if (parent != null) {
                resource = (RemoteResource) parent.getChild(_pathName(path));
                if (resource != null) {
                    resource.children(); // ensure that the rsource is regular loaded
                }
            } else {
                resource = provider.remoteReader.loadResource(
                        new RemoteResource(this, localPath), false);
            }
            if (resource == null) {
                resource = new RemoteResource.NonExisting(this, localPath);
            }
            resourceCache.put(localPath, resource);
            return resource;

        } catch (IllegalArgumentException ignore) {
        }
        return null;
    }

    /**
     * @return the 'non existing' replacement of the discarded resource
     */
    protected RemoteResource _discard(RemoteResource resource) {
        Resource parent = resource.getParent();
        if (parent instanceof RemoteResource) {
            RemoteResource remoteParent = (RemoteResource) parent;
            if (remoteParent.children != null) {
                remoteParent.children.remove(resource.name);
            }
        }
        RemoteResource placeholder = new RemoteResource.NonExisting(this, resource.path);
        resourceCache.put(resource.path, placeholder);
        return placeholder;
    }

    @Nullable
    RemoteResource _parent(String path) {
        String parentPath = _parentPath(path);
        return parentPath != null && provider.isLocal(parentPath)
                ? (RemoteResource) resourceCache.get(parentPath) : null;
    }

    @Nullable
    protected String _parentPath(@NotNull final String path) {
        if (StringUtils.isNotBlank(path)) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                return path.substring(0, lastSlash);
            }
            if (!"/".equals(path)) {
                return "/";
            }
        }
        return null;
    }

    @NotNull
    protected String _pathName(@NotNull final String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    @Nullable
    @Override
    public String getResolverRootPath() {
        return provider.localRoot;
    }

    @NotNull
    @Override
    public Resource resolve(@NotNull HttpServletRequest request, @NotNull String absPath) {
        return resolve(absPath);
    }

    @NotNull
    @Override
    public Resource resolve(@NotNull String absPath) {
        Resource resource = _resolve(absPath);
        return resource != null ? resource
                : new NonExistingResource(parentDelegate, provider.localPath(absPath));
    }

    @NotNull
    @Override
    public Resource resolve(@NotNull HttpServletRequest request) {
        return resolve(request.getPathInfo());
    }

    @NotNull
    @Override
    public String map(@NotNull String resourcePath) {
        return provider.remoteClient.getHttpUrl(provider.remotePath(resourcePath));
    }

    @Nullable
    @Override
    public String map(@NotNull HttpServletRequest request, @NotNull String resourcePath) {
        return map(resourcePath);
    }

    @Nullable
    @Override
    public Resource getResource(@NotNull final String path) {
        Resource resource;
        RemoteResource parent = _parent(path);
        // try to use a loaded parent to optimize caching
        if (parent != null) {
            resource = parent.getChild(_pathName(path));
            if (resource instanceof RemoteResource) {
                ((RemoteResource) resource).children(); // ensure that the rsource is regular loaded
            }
        } else {
            resource = _resolve(path);
        }
        // if the resource can't be resolved 'non existing' is cached...
        return resource instanceof RemoteResource.NonExisting ? null : resource;
    }

    @Nullable
    @Override
    public Resource getResource(Resource base, @NotNull String path) {
        return getResource(base.getPath() + "/" + path);
    }

    @NotNull
    @Override
    public String[] getSearchPath() {
        return provider.searchPath;
    }

    @Nullable
    @Override
    public Resource getParent(@NotNull Resource child) {
        String parentPath = _parentPath(child.getPath());
        return parentPath != null ? getResource(parentPath) : null;
    }

    @NotNull
    @Override
    public Iterator<Resource> listChildren(@NotNull Resource parent) {
        return parent.listChildren();
    }

    @NotNull
    @Override
    public Iterable<Resource> getChildren(@NotNull Resource parent) {
        return parent.getChildren();
    }

    @NotNull
    @Override
    public Iterator<Resource> findResources(@NotNull String query, String language) {
        return Collections.emptyIterator();
    }

    @NotNull
    @Override
    public Iterator<Map<String, Object>> queryResources(@NotNull String query, String language) {
        return Collections.emptyIterator();
    }

    @Override
    public boolean hasChildren(@NotNull Resource resource) {
        return resource.hasChildren();
    }

    @NotNull
    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        return new RemoteResolver(provider, parentDelegate);
    }

    @Override
    public boolean isLive() {
        return parentDelegate.isLive();
    }

    @Override
    public void close() {
    }

    @Nullable
    @Override
    public String getUserID() {
        return parentDelegate.getUserID();
    }

    @NotNull
    @Override
    public Iterator<String> getAttributeNames() {
        return Collections.emptyIterator();
    }

    @Nullable
    @Override
    public Object getAttribute(@NotNull String name) {
        return null;
    }

    @Override
    public void delete(@NotNull Resource resource) throws PersistenceException {
        if (resource instanceof RemoteResource) {
            changeSet.addDelete(_discard((RemoteResource) resource));
        }
    }

    @NotNull
    @Override
    public Resource create(@NotNull Resource parent, @NotNull String name, Map<String, Object> properties) throws PersistenceException {
        RemoteResource resource = new RemoteResource(this, parent.getPath() + "/" + name);
        resource.children = new LinkedHashMap<>();
        resource.modifiedValues = new ModifiableValueMapDecorator(new HashMap<>(properties));
        changeSet.addCreate(resource);
        return resource;
    }

    @Override
    public void revert() {
        changeSet.clear();
        resourceCache.clear();
    }

    @Override
    public void commit() throws PersistenceException {
        try {
            provider.remoteWriter.commitChanges(changeSet);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new PersistenceException(ex.getMessage());
        } finally {
            changeSet.clear();
            resourceCache.clear();
        }
    }

    @Override
    public boolean hasChanges() {
        return changeSet.size() > 0;
    }

    @Nullable
    @Override
    public String getParentResourceType(Resource resource) {
        Resource parent = resource != null ? getParent(resource) : null;
        return parent != null ? parent.getResourceType() : null;
    }

    @Nullable
    @Override
    public String getParentResourceType(String resourceType) {
        return null;
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return resource != null && resource.isResourceType(resourceType);
    }

    @Override
    public void refresh() {
    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        Resource designated;
        Resource source = getResource(srcAbsPath);
        Resource destParent = getResource(destAbsPath);
        if (source instanceof RemoteResource && destParent instanceof RemoteResource) {
            String name = source.getName();
            if (destParent.getChild(name) == null) {
                designated = new RemoteResource((RemoteResource) source, destParent.getPath() + "/" + name);
                changeSet.addCopy((RemoteResource) designated, source);
            } else {
                throw new PersistenceException(" a resource with the designated name exsits already at destination");
            }
        } else {
            designated = parentDelegate.copy(srcAbsPath, destAbsPath);
        }
        return designated;
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return move(srcAbsPath, destAbsPath, null, null);
    }

    @Override
    public Resource move(@NotNull final String srcAbsPath, @NotNull final String destParentAbsPath,
                         @Nullable final String destChildName, @Nullable final String order)
            throws PersistenceException {
        Resource designated;
        Resource source = getResource(srcAbsPath);
        Resource destParent = getResource(destParentAbsPath);
        if (source instanceof RemoteResource && destParent instanceof RemoteResource) {
            String name = StringUtils.isNotBlank(destChildName) ? destChildName : source.getName();
            if (destParent.getChild(name) == null || StringUtils.isNotBlank(order)) {
                designated = new RemoteResource((RemoteResource) source, destParent.getPath() + "/" + name);
                changeSet.addMove((RemoteResource) designated, destParent.getChild(name) == null
                        ? _discard((RemoteResource) source) : source, order);
            } else {
                throw new PersistenceException("a resource with the designated name exsits already at destination");
            }
        } else if (parentDelegate instanceof ExtendedResolver) {
            designated = ((ExtendedResolver) parentDelegate).move(srcAbsPath, destParentAbsPath, destChildName, order);
        } else {
            designated = parentDelegate.move(srcAbsPath, destParentAbsPath);
        }
        return designated;
    }

    @Override
    public Resource upload(@NotNull final String absPath, @NotNull final InputStream content,
                           @Nullable final String filename, @Nullable final String mimeType,
                           @Nullable final String charset)
            throws PersistenceException {
        Resource resource = getResource(absPath);
        if (resource != null && resource.getName().equals(JcrConstants.JCR_CONTENT) &&
                JcrConstants.NT_RESOURCE.equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE))) {
            Resource parent = resource.getParent();
            if (parent != null &&
                    JcrConstants.NT_FILE.equals(parent.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE))) {
                resource = parent;
            }
        }
        if (resource instanceof RemoteResource || (resource == null && provider.isLocal(absPath))) {
            if (resource == null) {
                resource = new RemoteResource(this, absPath);
            }
            changeSet.addUpload((RemoteResource) resource, content, filename, mimeType, charset);
        } else if (parentDelegate instanceof ExtendedResolver) {
            resource = ((ExtendedResolver) parentDelegate).upload(absPath, content, filename, mimeType, charset);
        } else {
            throw new PersistenceException("can't handle upload for resources out of the resolvers scope");
        }
        return resource;
    }

    @Nullable
    @Override
    public <AdapterType> AdapterType adaptTo(@NotNull Class<AdapterType> type) {
        if (type == Session.class) {
            return parentDelegate.adaptTo(type);
        }
        return null;
    }
}
