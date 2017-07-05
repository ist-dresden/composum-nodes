package com.composum.sling.core.concurrent;

import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.*;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the {@link LazyCreationService}. Uses {@link SequencerService} to
 * at least locally avoid conflicts by locking the parent of the created resource. If two nodes of a cluster
 * try to create the same resource, anyway, one of the transactions is rolled back. It will just be logged and
 * ignored.
 */
@Component(
        label = "Composum lazy creation service",
        description = "provides a cluster-safe 'get or create' pattern",
        immediate = true,
        metatype = true
)
@Service
public class LazyCreationServiceImpl implements LazyCreationService {

    private static final Logger LOG = getLogger(LazyCreationServiceImpl.class);

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Reference
    protected SequencerService sequencer;

    @Override
    public <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                             CreationStrategy creator, final Map<String, Object> parentProperties)
            throws RepositoryException {
        ParentCreationStrategy parentCreationStrategy = new ParentCreationStrategy() {
            @Override
            public Resource createParent(ResourceResolver resolver, Resource parentsParent, String parentName, int
                    level)
                    throws RepositoryException, PersistenceException {
                return resolver.create(parentsParent, parentName, parentProperties);
            }
        };
        return getOrCreate(resolver, path, getter, creator, parentCreationStrategy);
    }

    @Override
    public <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                             CreationStrategy creator, ParentCreationStrategy parentCreationStrategy)
            throws RepositoryException {
        Validate.notNull(path, "Path must not be null");
        Validate.isTrue(path.startsWith("/"), "Path must be absolute: %s", path);
        T result = getter.get(resolver, path);
        if (null == result) {
            String parentPath = ResourceUtil.getParent(path);
            SequencerService.Token token = sequencer.acquire(parentPath);
            try {
                refreshSession(resolver, true);
                if (null == (result = getter.get(resolver, path))) { // nobody created it during acquiring lock
                    ResourceResolver adminResolver = createAdministrativeResolver();
                    try {
                        Resource parentResource = resolver.getResource(parentPath);
                        if (null == parentResource) {
                            sequencer.release(token); // release lock temporarily to prevent deadlocks
                            token = null;
                            parentResource = safeCreateParent(adminResolver, parentPath, 1, parentCreationStrategy);
                            Validate.notNull(parentResource, "Parent creator didn't create " + parentPath);
                            token = sequencer.acquire(parentPath);
                        }
                        refreshSession(resolver, true);
                        if (null == (result = getter.get(resolver, path))) {
                            // nobody created it during re-acquiring lock
                            try {
                                refreshSession(adminResolver, false);
                                creator.create(adminResolver, parentResource, ResourceUtil.getName(path));
                                adminResolver.commit();
                                Resource resourceAsAdmin = adminResolver.getResource(path);
                                Validate.notNull(resourceAsAdmin, "Bug: could not find %s even after calling creator", path);
                                LOG.debug("Created {}", path);
                            } catch (ItemExistsException | PersistenceException e) { // ignore
                                LOG.info("Creation of {} aborted because of probably parallel creation {}", path, e);
                            } catch (RepositoryException e) { // others seem strange, though, but might be OK.
                                LOG.warn("Creation error for {}: {}", path, e);
                            }
                            refreshSession(resolver, true);
                            result = getter.get(resolver, path);
                        }
                    } finally {
                        adminResolver.close();
                    }
                }
            } finally {
                if (null != token) sequencer.release(token);
            }
        }
        if (null == result) LOG.warn("Still not present after trying to create it: {}", path);
        return result;
    }

    /**
     * Tries to create the parent while catching exceptions that could be triggered by someone having created it in
     * parallel in the meantime. Includes commit and locks path on this node.
     */
    protected Resource safeCreateParent(ResourceResolver adminResolver, String path, int level,
                                        ParentCreationStrategy parentCreationStrategy) throws RepositoryException {
        String[] separated = com.composum.sling.core.util.ResourceUtil.splitPathAndName(path);
        String parentPath = "".equals(separated[0]) ? "/" : separated[0];
        SequencerService.Token token = sequencer.acquire(parentPath);
        try {
            Resource resource = adminResolver.getResource(path);
            if (resource == null) {
                Resource parent = adminResolver.getResource(parentPath);
                if (parent == null) {
                    sequencer.release(token); // avoid any deadlock conditions by freeing lock temporarily
                    token = null;
                    parent = safeCreateParent(adminResolver, parentPath, level + 1, parentCreationStrategy);
                    token = sequencer.acquire(parentPath);
                    refreshSession(adminResolver, false);
                    resource = adminResolver.getResource(path); // could have been created when re-locking
                }
                if (null == resource) {
                    try {
                        resource = parentCreationStrategy.createParent(adminResolver, parent, separated[1], level);
                        Validate.notNull(parent, "Parent creator didn't create " + path);
                        adminResolver.commit();
                        LOG.debug("Created parent {}", path);
                    } catch (PersistenceException e) { // ignore
                        LOG.info("Creation of {} aborted because of probably parallel creation {}", path, e);
                        resource = adminResolver.getResource(path);
                        if (null == resource) {
                            LOG.error("Bug: creation aborted *and* resource is not there!", e);
                        }
                    }
                }
            }
            return resource;
        } finally {
            if (null != token) sequencer.release(token);
        }
    }

    /** Resets unmodified resources to the currently saved state. */
    protected void refreshSession(ResourceResolver resolver, boolean keepChanges) {
        try {
            Session session = resolver.adaptTo(Session.class);
            session.refresh(keepChanges);
        } catch (RepositoryException rex) {
            LOG.warn(rex.getMessage(), rex);
        }
    }

    /** Make administrative resolver with the necessary permissions to create stuff. Remember to close it! */
    protected ResourceResolver createAdministrativeResolver() {
        // used for maximum backwards compatibility; TODO recheck and decide from time to time
        try {
            return resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            throw new SlingException("Configuration problem: we cannot get an administrative resolver ", e);
        }
    }
}
