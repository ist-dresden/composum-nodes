package com.composum.sling.core.concurrent;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import java.util.Calendar;
import java.util.Map;

import static com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED;
import static com.composum.sling.core.util.ResourceUtil.TYPE_CREATED;
import static com.composum.sling.core.util.ResourceUtil.TYPE_LAST_MODIFIED;
import static com.composum.sling.core.util.ResourceUtil.TYPE_LOCKABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the {@link LazyCreationService}. Uses {@link SequencerService} to
 * at least locally avoid conflicts by locking the parent of the created resource. If two nodes of a cluster
 * try to create the same resource, anyway, one of the transactions is rolled back. It will just be logged and
 * ignored.
 */
@Component(
        label = "Composum Lazy Creation Service",
        description = "Provides a cluster-safe 'get or create' pattern",
        immediate = true,
        metatype = true
)
@Service
public class LazyCreationServiceImpl implements LazyCreationService {

    private static final Logger LOG = getLogger(LazyCreationServiceImpl.class);

    public static final String MAXIMUM_LOCKWAIT_TIME_SEC = "lazycreation.maximumlockwait";
    @Property(
            name = MAXIMUM_LOCKWAIT_TIME_SEC,
            label = "Maximum lock wait time",
            description = "Maximum time in seconds for which the service waits until it assumes another cluster node " +
                    "tried to create a resource and the attempt hangs. The lock is broken after that and another " +
                    "attempt is started.",
            intValue = 30
    )
    protected int maximumLockWaitTimeSec;


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
    public <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter, CreationStrategy
            creator, InitializationStrategy initializer, final Map<String, Object> parentProperties)
            throws RepositoryException, PersistenceException {
        ParentCreationStrategy parentCreationStrategy = new ParentCreationStrategy() {
            @Override
            public Resource createParent(ResourceResolver resolver, Resource parentsParent, String parentName, int
                    level)
                    throws RepositoryException, PersistenceException {
                return resolver.create(parentsParent, parentName, parentProperties);
            }
        };
        return getOrCreate(resolver, path, getter, creator, initializer, parentCreationStrategy);
    }

    @Override
    public <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                             CreationStrategy creator, ParentCreationStrategy parentCreationStrategy)
            throws RepositoryException {
        Validate.notNull(path, "Path must not be null");
        Validate.isTrue(path.startsWith("/"), "Path must be absolute: %s", path);
        T result = getter.get(resolver, path);
        if (null != result) return result;

        String parentPath = ResourceUtil.getParent(path);
        ResourceResolver adminResolver = null;
        SequencerService.Token token = sequencer.acquire(path);
        try {
            refreshSession(resolver, true);
            // check nobody created it during acquiring lock:
            if (null != (result = getter.get(resolver, path))) return result;

            adminResolver = createAdministrativeResolver();
            Resource parentResource = adminResolver.getResource(parentPath);
            if (null == parentResource) {
                sequencer.release(token); // release lock temporarily to prevent deadlocks
                token = null;
                parentResource = safeCreateParent(adminResolver, parentPath, 1, parentCreationStrategy);
                Validate.notNull(parentResource, "Parent creator didn't create " + parentPath);
                token = sequencer.acquire(path);

                refreshSession(resolver, true);
                // check nobody created it during re-acquiring lock:
                if (null != (result = getter.get(resolver, path))) return result;
            }

            try {
                refreshSession(adminResolver, false);
                creator.create(adminResolver, parentResource, ResourceUtil.getName(path));
                adminResolver.commit();

                Resource resourceAsAdmin = adminResolver.getResource(path);
                Validate.notNull(resourceAsAdmin, "Bug: could not find %s even after calling creator", path);
                LOG.debug("Created {}", path);
            } catch (ItemExistsException | PersistenceException e) { // ignore
                LOG.info("Creation of {} aborted - probably parallel creation {}", path, e.toString()
                        + "/" + String.valueOf(e.getCause()));
            } catch (RepositoryException e) { // others seem strange, though, but might be OK.
                LOG.warn("Creation error for {}: {}", path, e);
            }

            refreshSession(resolver, true);
            result = getter.get(resolver, path);
        } finally {
            if (null != token) sequencer.release(token);
            if (null != adminResolver) adminResolver.close();
        }
        if (null == result) LOG.warn("Still not present after trying to create it: {}", path);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>We create the item in two steps. First, it is created and JCR-locked. Then it is initialized and JCR-unlocked.
     * So, when trying to retrieve the item, we check that it is not locked, too, to verify that it is not in
     * construction. The resource is assumed fully initialized if it exists and is not locked. We put the current time
     * into lastupdatetime to keep track how long it is locked and whether the lock must be broken. Since we can only
     * lock the item after the creation is committed, we set jcr:lastModified only after locking and check this must be
     * set when retrieving the item.
     * <p>
     * If the item exists but is locked, we wait until it is unlocked and then return what's there. If we exceed the
     * {@link #maximumLockWaitTimeSec} when waiting for the lock, we break the lock and create it ourselves.
     */
    @Override
    public <T> T getOrCreate(final ResourceResolver resolver, final String path, RetrievalStrategy<T> getter,
                             final CreationStrategy creator, final InitializationStrategy initializer,
                             final ParentCreationStrategy parentCreationStrategy) throws
            RepositoryException, PersistenceException {
        Validate.notNull(path, "Path must not be null");
        Validate.isTrue(path.startsWith("/"), "Path must be absolute: %s", path);
        if (resourceIsInitialized(resolver, path)) return getter.get(resolver, path);
        LOG.debug("Going to create and init {}", path);

        String parentPath = ResourceUtil.getParent(path);
        ResourceResolver adminResolver = null;
        Lock lock = null;
        try {
            refreshSession(resolver, true);
            // check that nobody created it during acquiring lock
            if (resourceIsInitialized(resolver, path)) return getter.get(resolver, path);

            adminResolver = createAdministrativeResolver();
            Resource parentResource = adminResolver.getResource(parentPath);
            if (null == parentResource) {
                parentResource = safeCreateParent(adminResolver, parentPath, 1, parentCreationStrategy);
                Validate.notNull(parentResource, "Parent creator didn't create " + parentPath);
            }

            refreshSession(adminResolver, false);
            Resource resource = adminResolver.getResource(path);

            if (null == resource) {
                resource = createUninitializedResource(adminResolver, parentResource, path, creator);
            }

            LockManager lockManager = adminResolver.adaptTo(Session.class).getWorkspace().getLockManager();
            lock = tryToLockResource(path, adminResolver);

            if (null != lock) {
                initializeResource(adminResolver, path, initializer, lockManager);
            }

            refreshSession(resolver, true);
            T result = getter.get(resolver, path);
            if (null == result)
                LOG.warn("Still not present after trying to create it: {}", path);
            return result;
        } finally {
            if (null != adminResolver) adminResolver.close();
        }
    }

    protected Resource createUninitializedResource(ResourceResolver adminResolver, Resource parentResource, String path,
                                                   CreationStrategy creator) {
        SequencerService.Token token = sequencer.acquire(path);
        try {
            refreshSession(adminResolver, false);
            Resource resource = adminResolver.getResource(path);
            if (null == resource) {
                try {
                    resource = creator.create(adminResolver, parentResource, ResourceUtil.getName(path));
                    Node node = resource.adaptTo(Node.class);
                    node.addMixin(TYPE_CREATED);
                    node.addMixin(TYPE_LAST_MODIFIED);
                    node.addMixin(TYPE_LOCKABLE);
                    node.setProperty(PROP_LAST_MODIFIED, (Value) null); // marker that is not initialized yet
                    adminResolver.commit();
                    LOG.debug("Created uninitialized {}", path);
                } catch (ItemExistsException | PersistenceException e) { // ignore
                    LOG.info("Creation of uninitialized {} aborted - probably parallel creation: {}", path, e
                            .toString() + "/" + String.valueOf(e.getCause()));
                } catch (RepositoryException e) { // others seem strange, though, but might be OK.
                    LOG.warn("Creation error for uninitialized {}: {}", path, e);
                }
                resource = adminResolver.getResource(path);
                Validate.notNull(resource, "Bug: could not find %s after trying to create it: %s", path);
            }
            return resource;
        } finally {
            sequencer.release(token);
        }
    }

    /**
     * If it is not initialized, try to lock it for up to {@link #maximumLockWaitTimeSec}.
     *
     * @return the lock it is locked, null if it is already initialized by someone else
     * @throws javax.jcr.lock.LockException if we couldn't get a lock
     */
    protected Lock tryToLockResource(String path, ResourceResolver adminResolver)
            throws RepositoryException, PersistenceException {
        LockManager lockManager = adminResolver.adaptTo(Session.class).getWorkspace().getLockManager();
        Calendar resourceLockTime = ResourceHandle.use(adminResolver.getResource(path))
                .getProperty(PROP_LAST_MODIFIED, Calendar.getInstance());
        long lockTime = Math.max(resourceLockTime.getTimeInMillis(), System.currentTimeMillis());
        final long stopPollingTime = lockTime + maximumLockWaitTimeSec * 1000;
        long waitStep = 0;
        long restWait;
        Exception lastFail = null;
        do {
            try {
                Thread.sleep(waitStep);
            } catch (InterruptedException e) {
            }
            // We need sequencer because the JCR locking doesn't seem to distinguish between sessions on one instance, or something. Hard to test.
            SequencerService.Token token = sequencer.acquire(path);
            try {
                refreshSession(adminResolver, false);
                if (resourceIsInitialized(adminResolver, path)) return null;
                boolean locked = lockManager.holdsLock(path);
                LOG.debug("Path {} is locked={}", path, locked);
                if (!locked) try {
                    Lock lock = lockManager.lock(path, true, false, Long.MAX_VALUE, null);
                    ResourceHandle.use(adminResolver.getResource(path)).setProperty(PROP_LAST_MODIFIED, Calendar
                            .getInstance());
                    adminResolver.commit();
                    LOG.debug("Got lock on {} token {}", path, lock.getLockToken());
                    return lock;
                } catch (LockException | PersistenceException ex) {
                    LOG.info("Could not lock {} : {}", path, ex.toString());
                    lastFail = ex;
                }
            } finally {
                sequencer.release(token);
            }

            restWait = stopPollingTime - System.currentTimeMillis();
            waitStep = Math.min(waitStep * 2 + 100, restWait); // iterative doubling to not try too often
        } while (restWait > 0);

        // we take over the lock from whoever locked it.
        SequencerService.Token token = sequencer.acquire(path);
        try {
            refreshSession(adminResolver, false);
            Lock lock;
            try {
                lock = lockManager.getLock(path);
            } catch (LockException le) { // node not locked anymode - race condition?
                refreshSession(adminResolver, false);
                if (resourceIsInitialized(adminResolver, path)) return null;
                LOG.error("Bug: could not lock " + path + " but is now unlocked but not ready: ", lastFail);
                throw new LockException("Could not lock " + path + " but is unlocked but not ready", lastFail);
            }
            try {
                ResourceHandle.use(adminResolver.getResource(path)).setProperty(PROP_LAST_MODIFIED, (Calendar) null);
                Validate.isTrue(path.equals(lock.getNode().getPath()), "Unexpected lock path %s instead of %s", path,
                        lock.getNode().getPath());
                adminResolver.commit();
                refreshSession(adminResolver, false);
                lockManager.addLockToken(lock.getLockToken());
                lockManager.unlock(path);
                lock = lockManager.lock(path, true, false, Long.MAX_VALUE, null);
                adminResolver.commit();

                ResourceHandle.use(adminResolver.getResource(path)).setProperty(PROP_LAST_MODIFIED, Calendar
                        .getInstance());
                adminResolver.commit();
                LOG.info("Took over obsolete lock on {}", path);
                return lock;
            } catch (LockException le) { // node not locked anymode - race condition?
                refreshSession(adminResolver, false);
                if (resourceIsInitialized(adminResolver, path)) return null;
                LOG.warn("Taking over lock on " + path + " failed; giving up since timeout");
                throw le;
            }
        } finally {
            sequencer.release(token);
        }
    }

    protected void initializeResource(ResourceResolver adminResolver, String path, InitializationStrategy
            initializer, LockManager lockManager) {
        SequencerService.Token token = sequencer.acquire(path);
        try {
            Resource resource;
            try {
                refreshSession(adminResolver, false);
                resource = adminResolver.getResource(path);
                if (!resourceIsInitialized(adminResolver, path)) {
                    initializer.initialize(adminResolver, resource);
                    ResourceHandle.use(resource).setProperty(PROP_LAST_MODIFIED, Calendar.getInstance());
                    adminResolver.commit();
                }

                // we deliberately do only unlock when the initialization is successful, since otherwise the
                // entity would be treated as "initialized." Another request will take over after timeout.
                LOG.info("Initialized {}", path);
                refreshSession(adminResolver, false);
                if (lockManager.holdsLock(path)) {
                    lockManager.addLockToken(lockManager.getLock(path).getLockToken());
                    lockManager.unlock(path);
                    LOG.debug("Unlocking {}", path);
                }
                adminResolver.commit();
            } catch (ItemExistsException | PersistenceException e) { // ignore
                LOG.info("Initialization of {} aborted - probably parallel initialization: {}", path, e.toString()
                        + "/" + String.valueOf(e.getCause()));
            } catch (RepositoryException e) { // others seem strange, though, but might be OK.
                LOG.warn("Initialization error for {}: {}", path, e);
            }
        } finally {
            sequencer.release(token);
        }
    }

    protected boolean resourceIsInitialized(ResourceResolver resolver, String path) throws RepositoryException {
        return isInitialized(resolver.getResource(path));
    }

    @Override
    public boolean isInitialized(Resource resource) throws RepositoryException {
        ResourceHandle handle = ResourceHandle.use(resource);
        return handle.isValid() && null != handle.getProperty(PROP_LAST_MODIFIED) &&
                !handle.getResourceResolver().adaptTo(Session.class).getWorkspace().getLockManager().holdsLock
                        (handle.getPath());
    }

    @Override
    public Resource waitForInitialization(ResourceResolver resolver, String path) throws RepositoryException {
        Resource resource = resolver.getResource(path);
        if (null == resource) {
            // there may be a creation in progress, so we wait for the lock.
            SequencerService.Token token = sequencer.acquire(path);
            sequencer.release(token);
            refreshSession(resolver, true);
            resource = resolver.getResource(path);
        }
        if (null == resource) return null;
        if (isInitialized(resource)) return resource;

        final long stopPollingTime = System.currentTimeMillis() + maximumLockWaitTimeSec * 1000;
        long waitStep = 0;
        long restWait;
        do {
            try {
                Thread.sleep(waitStep);
            } catch (InterruptedException e) {
            }
            refreshSession(resolver, true);
            resource = resolver.getResource(path);
            if (null == resource) {
                LOG.warn("Resource unexpectedly vanished during wait: {}", path);
                return null; // vanished again - how??
            }
            if (isInitialized(resource)) return resource;

            restWait = stopPollingTime - System.currentTimeMillis();
            waitStep = Math.min(waitStep * 2 + 100, restWait); // iterative doubling to not try too often
        } while (restWait > 0);
        return null;
    }

    /**
     * Tries to create the parent while catching exceptions that could be triggered by someone having created it in
     * parallel in the meantime. Includes commit and locks path on this node.
     */
    protected Resource safeCreateParent(ResourceResolver adminResolver, String path, int level,
                                        ParentCreationStrategy parentCreationStrategy) throws RepositoryException {
        if ("/".equals(path))
            return adminResolver.getResource("/");

        String[] separated = com.composum.sling.core.util.ResourceUtil.splitPathAndName(path);
        String parentPath = separated[0];
        SequencerService.Token token = sequencer.acquire(path);
        try {
            refreshSession(adminResolver, false);
            Resource resource = adminResolver.getResource(path);
            if (resource == null) {
                Resource parent = adminResolver.getResource(parentPath);
                if (parent == null) {
                    sequencer.release(token); // avoid any deadlock conditions by freeing lock temporarily
                    token = null;
                    parent = safeCreateParent(adminResolver, parentPath, level + 1, parentCreationStrategy);
                    token = sequencer.acquire(path);
                    refreshSession(adminResolver, false);
                    resource = adminResolver.getResource(path); // could have been created when re-locking
                }
                if (null == resource) {
                    try {
                        resource = parentCreationStrategy.createParent(adminResolver, parent, separated[1], level);
                        Validate.notNull(resource, "Parent creator didn't create " + path);
                        adminResolver.commit();
                        LOG.debug("Created parent {}", path);
                    } catch (PersistenceException e) { // ignore
                        LOG.info("Creation of parent {} aborted - probably parallel creation {}", path, e.toString()
                                + "/" + String.valueOf(e.getCause()));
                        refreshSession(adminResolver, false);
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
            LOG.warn(rex.toString(), rex);
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
