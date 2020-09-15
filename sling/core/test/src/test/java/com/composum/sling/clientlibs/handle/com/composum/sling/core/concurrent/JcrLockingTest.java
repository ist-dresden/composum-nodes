package com.composum.sling.clientlibs.handle.com.composum.sling.core.concurrent;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.test.util.JcrTestUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.*;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.composum.sling.core.util.ResourceUtil.*;
import static org.junit.Assert.*;

/**
 * Verifies that JCR locking works as expected - preparation for
 * {@link com.composum.sling.core.concurrent.LazyCreationServiceImpl}.
 */
public class JcrLockingTest {

    public String path;

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private Resource res;
    private ResourceResolverFactory resolverFactory;
    private ResourceResolver resolver;
    private LockManager lockManager;

    @Before
    public void setup() throws Exception {
        path = context.uniqueRoot().content() + "/lockingtest";
        ResourceBuilder builder = context.build();
        builder.resource(path, PROP_PRIMARY_TYPE, TYPE_UNSTRUCTURED, PROP_MIXINTYPES, new String[]{TYPE_LOCKABLE});
        builder.commit();
        res = context.resourceResolver().getResource(path);
        assertNotNull(res);
        resolverFactory = context.getService(ResourceResolverFactory.class);
        resolver = resolverFactory.getAdministrativeResourceResolver(null);
        lockManager = resolver.adaptTo(Session.class).getWorkspace().getLockManager();
    }

    @After
    public void teardown() {
        resolver.close();
    }


    @Test
    public void testSimpleLocking() throws Exception {
        assertFalse(lockManager.holdsLock(path));
        Lock lock = lockManager.lock(path, true, false, 2, null);
        assertNotNull(lock);
        resolver.commit();
        // unfortunately lock.getSecondsRemaining is not used in sling-mock-oak, while it works in the server :-(
        // assertTrue(lock.getSecondsRemaining() > 0 && lock.getSecondsRemaining() < 2);
        assertTrue(lockManager.holdsLock(path));
        assertEquals(1, lockManager.getLockTokens().length);
        lockManager.unlock(path);
        assertFalse(lockManager.holdsLock(path));
        assertEquals(0, lockManager.getLockTokens().length);
    }

    /**
     * Object needs to be comitted when locking.
     */
    @Test(expected = InvalidItemStateException.class)
    public void testLockDuringCreation() throws Exception {
        String freshPath = path + "/fresh";
        Resource fresh = resolver.create(res, "fresh", new HashMap<String, Object>() {
            {
                put(PROP_PRIMARY_TYPE, TYPE_UNSTRUCTURED);
                put(PROP_MIXINTYPES, new String[]{
                        TYPE_LOCKABLE
                });
            }
        });
        assertEquals(freshPath, fresh.getPath());
        Lock lock = lockManager.lock(freshPath, true, false, 2, null);
    }

    /**
     * Verifies that jcr:lastModified behaves like we need it for LazyCreationService.
     */
    @Test
    public void testLastModificationDate() throws Exception {
        ResourceHandle fresh = ResourceHandle.use(resolver.create(res, "fresh", new HashMap<String, Object>() {
            {
                put(PROP_PRIMARY_TYPE, TYPE_UNSTRUCTURED);
                put(PROP_MIXINTYPES, new String[]{
                        TYPE_CREATED, TYPE_CREATED, TYPE_LAST_MODIFIED
                });
            }
        }));
        fresh.adaptTo(Node.class).addMixin(TYPE_LAST_MODIFIED);
        fresh.adaptTo(Node.class).addMixin(TYPE_LOCKABLE);
        fresh.adaptTo(Node.class).addMixin(TYPE_LOCKABLE);
        fresh.adaptTo(Node.class).setProperty(PROP_LAST_MODIFIED, (Value) null);
        // fresh.adaptTo(ModifiableValueMap.class).remove(PROP_LAST_MODIFIED);
        resolver.commit();
        resolver.adaptTo(Session.class).refresh(false);

        Thread.sleep(1100);
        fresh = ResourceHandle.use(resolver.getResource(fresh.getPath()));
        JcrTestUtils.printResourceRecursivelyAsJson(fresh);
        assertNull(fresh.getProperty(PROP_LAST_MODIFIED));
        fresh.adaptTo(ModifiableValueMap.class).put("something", "else");
        fresh.adaptTo(ModifiableValueMap.class).put(PROP_LAST_MODIFIED, Calendar.getInstance());
        resolver.commit();

        resolver.adaptTo(Session.class).refresh(false);
        fresh = ResourceHandle.use(resolver.getResource(fresh.getPath()));
        JcrTestUtils.printResourceRecursivelyAsJson(fresh);
        assertNotNull(fresh.getProperty(PROP_LAST_MODIFIED));
    }


    /**
     * Try to create a locked object without having an intermediate unlocked state.
     */
    @Test
    public void testLockAndMove() throws Exception {
        assertFalse(lockManager.holdsLock(path));
        Lock lock = lockManager.lock(path, true, false, 2, null);
        resolver.commit();
        assertNotNull(lock);
        assertTrue(lockManager.holdsLock(path));

        ResourceResolver resolver2 = resolverFactory.getAdministrativeResourceResolver(null);
        LockManager lockManager2 = resolver2.adaptTo(Session.class).getWorkspace().getLockManager();

        assertNotNull(resolver.getResource(path));
        String freshPath = context.uniqueRoot().content() + "/fresh";
        resolver.adaptTo(Session.class).getWorkspace().move(path, freshPath);
        Resource freshResource = resolver.getResource(freshPath);
        assertNotNull(freshResource);
        assertEquals(freshPath, freshResource.getPath());
        assertNull(resolver.getResource(path));

        assertTrue(lockManager.holdsLock(freshPath));
        assertTrue(lockManager2.holdsLock(freshPath));

        try {
            lockManager.unlock(path);
            fail();
        } catch (PathNotFoundException ex) {
            // item is moved - can't do it this way.
        }
        try {
            lockManager.unlock(freshPath);
            fail();
        } catch (LockException ex) {
            // we do not own a lock on freshPath
        }
        assertEquals(1, lockManager.getLockTokens().length);
        lockManager.addLockToken(lockManager.getLock(freshPath).getLockToken()); // Duh!
        assertEquals(2, lockManager.getLockTokens().length);
        lockManager.unlock(freshPath);
        assertFalse(lockManager.holdsLock(freshPath));
        assertEquals(1, lockManager.getLockTokens().length); // Duh!

        resolver2.close();
    }

    @Test(expected = LockException.class)
    public void testLockingTwice() throws Exception {
        {
            assertFalse(lockManager.holdsLock(path));
            Lock lock = lockManager.lock(path, true, false, Long.MAX_VALUE, null);
            resolver.commit();
        }
        {
            ResourceResolver resolver2 = resolverFactory.getAdministrativeResourceResolver(null);
            LockManager lockManager2 = resolver2.adaptTo(Session.class).getWorkspace().getLockManager();
            Lock lock2 = lockManager2.lock(path, true, false, Long.MAX_VALUE, null);
            resolver2.close();
        }
    }

    @Test
    public void testUnlockFromAnotherResolver() throws Exception {
        {
            assertFalse(lockManager.holdsLock(path));
            Lock lock = lockManager.lock(path, true, false, 2, null);
            resolver.commit();
        }
        {
            ResourceResolver resolver2 = resolverFactory.getAdministrativeResourceResolver(null);
            LockManager lockManager2 = resolver2.adaptTo(Session.class).getWorkspace().getLockManager();
            assertTrue(lockManager2.holdsLock(path));
            lockManager2.addLockToken(lockManager2.getLock(path).getLockToken());
            lockManager2.unlock(path);
            resolver2.close();
        }
    }

    /**
     * Locking doesn't prevent others writing to the node.
     */
    @Test
    public void testWriteNodeLockedByOther() throws Exception {
        {
            assertFalse(lockManager.holdsLock(path));
            Lock lock = lockManager.lock(path, true, false, 2, null);
            resolver.commit();
        }
        {
            ResourceResolver resolver2 = resolverFactory.getAdministrativeResourceResolver(null);
            ResourceHandle handle = ResourceHandle.use(resolver2.getResource(path));
            handle.setProperty("test", "hallo");
            resolver2.close();
        }
    }

    /**
     * Several threads try to lock the same resource. If several succeed at the same time, something is wrong.
     */
    @Test
    @Ignore("It seems the locking implementation is buggy, here: several threads can lock the same thing. :-(")
    public void testParallelLockingProblem() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicBoolean someoneHasLock = new AtomicBoolean();
        try {
            final AtomicInteger lockCount = new AtomicInteger();
            List<Callable<Void>> callables = new ArrayList<>();
            final Random rnd = new Random();
            for (int i = 0; i < 10; ++i) {
                final ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
                Session session = resolver.adaptTo(Session.class);
                final LockManager lockManager = session.getWorkspace().getLockManager();
                callables.add(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            if (!someoneHasLock.get()) {
                                Lock lock = lockManager.lock(path, true, false, 10000, null);
                                resolver.commit();
                                Thread.sleep(500);
                                someoneHasLock.set(true); // make sure we only count threads who did get a lock so far, not after we close.
                                lockCount.incrementAndGet();
                                assertTrue(lock.isLockOwningSession());
                                assertTrue(lock.isLive());
                                assertTrue(lock.isDeep());
                                assertTrue(lockManager.holdsLock(path));
                                String[] tokens = lockManager.getLockTokens();
                                assertNotNull(tokens);
                                assertTrue(tokens.length > 0);
                            }
                        } catch (LockException e) {
                            // OK, some don't get it.
                        } finally {
                            resolver.close();
                        }
                        return null;
                    }
                });
            }
            List<Future<Void>> results = executor.invokeAll(callables);
            for (Future<Void> result : results) {
                result.get();
            }
            assertEquals(1, lockCount.get());
        } finally {
            executor.shutdownNow();
        }
    }


}
