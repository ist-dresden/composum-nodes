package com.composum.sling.clientlibs.handle.com.composum.sling.core.concurrent;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.concurrent.LazyCreationService;
import com.composum.sling.core.concurrent.LazyCreationServiceImpl;
import com.composum.sling.core.concurrent.SemaphoreSequencer;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.test.util.JcrTestUtils;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.composum.sling.core.util.ResourceUtil.PROP_PRIMARY_TYPE;
import static com.composum.sling.core.util.ResourceUtil.TYPE_UNSTRUCTURED;
import static org.apache.sling.hamcrest.ResourceMatchers.props;
import static org.apache.sling.hamcrest.ResourceMatchers.resourceType;
import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests for {@link com.composum.sling.core.concurrent.LazyCreationServiceImpl}.
 */
public class TestLazyCreationServiceImpl {

    private static final Logger LOG = getLogger(TestLazyCreationServiceImpl.class);

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    protected LazyCreationService lazyCreationService;
    protected ResourceResolverFactory resourceResolverFactory;
    protected ExecutorService executor;
    protected Random rnd = new Random();

    protected Map<String, Object> folderProps;

    {
        folderProps = new HashMap<>();
        folderProps.put(PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER);
    }

    protected Map<String, Object> itemProps;

    {
        itemProps = new HashMap<>();
        itemProps.put(PROP_PRIMARY_TYPE, TYPE_UNSTRUCTURED);
    }


    protected void setup(final SequencerService sequencerService) {
        resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        lazyCreationService = new LazyCreationServiceImpl() {{
            sequencer = sequencerService;
            resolverFactory = resourceResolverFactory;
        }};
        executor = Executors.newFixedThreadPool(20);
    }

    @After
    public void teardown() {
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testCreation() throws Exception {
        final SequencerService sequencerService = new SemaphoreSequencer() {{
            activate(context.componentContext());
        }};
        setup(sequencerService);
        runCreation();
    }

    /** Simulate collisions in cluster as far as possible - SequencerService doesn't work there. */
    @Test
    public void testCreationWithoutSequencer() throws Exception {
        final SequencerService sequencerService = new SequencerService<SequencerService.Token>() {
            @Override
            public Token acquire(String key) {
                return null;
            }

            @Override
            public void release(Token token) {
            }
        };
        setup(sequencerService);
        runCreation();
    }

    @Test
    public void testRandomDelays() throws Exception {
        folderProps = delayWrap(folderProps);
        itemProps = delayWrap(itemProps);
        testCreationWithoutSequencer();
    }

    private Map<String, Object> delayWrap(final Map<String, Object> wrappedMap) {
        return new AbstractMap<String, Object>() {
            @Override
            public Set<Entry<String, Object>> entrySet() {
                try {
                    Thread.sleep(rnd.nextInt(100) + 100);
                } catch (InterruptedException e) {
                    LOG.error("" + e, e);
                }
                return wrappedMap.entrySet();
            }
        };
    }


    protected void runCreation() throws Exception {
        final List<String> paths = Arrays.asList("/var/t1/c1/a", "/var/t1/c1/b", "/var/t1/c1/c", "/var/t1/c2/e",
                "/var/t1/c2/f", "/var/t2/c3/g", "/var/t2/c3/h", "/var/t2/c3/i", "/var/t2/c4/j", "/var/t2/c4/k",
                "/var/t2/c4/l", "/var/t2/c4/m", "/var/t2/c4/n", "/var/t2/c5/o", "/var/t2/c6/p", "/var/t2/c7/q",
                "/var/t2/c8/r");
        Collections.shuffle(paths);
        final int numGetOrCreates = 40;
        List<Future<ResourceHandle>> futures = new ArrayList<Future<ResourceHandle>>();
        for (int i = 0; i < numGetOrCreates; ++i) {
            final String path = paths.get(rnd.nextInt(paths.size()));
            final ResourceResolver resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            futures.add(executor.submit(new Callable<ResourceHandle>() {
                public ResourceHandle call() throws Exception {
                    ResourceHandle handle = lazyCreationService.getOrCreate(
                            resolver, path, new LazyCreationService.RetrievalStrategy<ResourceHandle>() {
                                @Override
                                public ResourceHandle get(ResourceResolver resolver, String path) throws
                                        RepositoryException {
                                    ResourceHandle handle1 = ResourceHandle.use(resolver.getResource(path));
                                    return handle1.isValid() ? handle1 : null;
                                }
                            },
                            new LazyCreationService.CreationStrategy() {
                                @Override
                                public void create(ResourceResolver resolver, String path) throws
                                        RepositoryException, PersistenceException {
                                    String[] separated = ResourceUtil.splitPathAndName
                                            (path);
                                    resolver.create(resolver.getResource(separated[0]), separated[1], itemProps);
                                }
                            },
                            folderProps);
                    return handle;
                }
            }));
        }
        for (Future<ResourceHandle> future : futures) {
            ResourceHandle handle = future.get();
            assertNotNull(handle);
            assertTrue(handle.isValid());
            assertThat(handle, props(PROP_PRIMARY_TYPE, TYPE_UNSTRUCTURED));
        }
        // JcrTestUtils.printResourceRecursivelyAsJson(context.resourceResolver().getResource("/"));
    }

}
