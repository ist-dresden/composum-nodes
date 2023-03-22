package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.AbstractClientlibVisitor;
import com.composum.sling.clientlibs.processor.ProcessingVisitor;
import com.composum.sling.clientlibs.processor.ProcessorContext;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.processor.RenderingVisitor;
import com.composum.sling.clientlibs.processor.UpdateTimeVisitor;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.clientlibs.service.ClientlibPermissionPlugin;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.clientlibs.service.DefaultClientlibService;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.concurrent.LazyCreationService;
import com.composum.sling.core.concurrent.LazyCreationServiceImpl;
import com.composum.sling.core.concurrent.SemaphoreSequencer;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.ServiceHandle;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.xss.XSSFilter;
import org.apache.sling.xss.impl.XSSAPIImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Common code for clientlib tests.
 */
public class AbstractClientlibTest {

    public static final String DEFAULT_CACHE_ROOT = "/var/composum/clientlibs";

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    protected boolean debuggingMode;
    protected boolean useMinifiedFiles;

    protected RendererContext rendererContext;

    protected DefaultClientlibService clientlib2Service;
    protected ClientlibConfiguration configurationService;

    protected ExecutorService executorService;

    protected ClientlibPermissionPlugin permissionPlugin;

    protected final String CONTEXTPATH = "/context";

    /**
     * The time the clientlibs have been created in setupFramework.
     */
    private Calendar setupTime;

    @Before
    public void setupFramework() throws IllegalAccessException {
        setupTime = GregorianCalendar.getInstance();
        context.request().setContextPath(CONTEXTPATH);

        final SequencerService sequencerService = context.registerInjectActivateService(new SemaphoreSequencer());

        final LazyCreationService creationService = context.registerInjectActivateService(
                new LazyCreationServiceImpl());

        permissionPlugin = Mockito.mock(ClientlibPermissionPlugin.class);
        Mockito.when(permissionPlugin.categoryFilter(ArgumentMatchers.anyString())).thenReturn(ResourceFilter.ALL);

        final ClientlibConfiguration.Config serviceConfig = new ClientlibConfiguration.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean debug() {
                return AbstractClientlibTest.this.debuggingMode;
            }

            @Override
            public boolean author_mapping_enabled() {
                return false;
            }

            @Override
            public boolean css_minimize() {
                return true;
            }

            @Override
            public int css_line_break() {
                return 0;
            }

            @Override
            public boolean debug_tag() {
                return false;
            }

            @Override
            public boolean rerender_on_nocache() {
                return false;
            }

            @Override
            public String template_link_css() {
                return "  <link rel=\"stylesheet\" href=\"{0}\" />";
            }

            @Override
            public String template_link_javascript() {
                return "  <script type=\"text/javascript\" src=\"{0}\"></script>";
            }

            @Override
            public String template_link_general() {
                return "  <link rel=\"{1}\" href=\"{0}\" />";
            }

            @Override
            public boolean gzip_enabled() {
                return false;
            }

            @Override
            public String clientlibs_cache_root() {
                return DEFAULT_CACHE_ROOT;
            }

            @Override
            public boolean clientlibs_minified_use() {
                return AbstractClientlibTest.this.useMinifiedFiles;
            }

            @Override
            public boolean clientlibs_url_map() {
                return true;
            }

            @Override
            public int clientlibs_resolver_cache_time() {
                return 60;
            }

            @Override
            public int clientlibs_threadpool_min() {
                return 10;
            }

            @Override
            public int clientlibs_threadpool_max() {
                return 20;
            }
        };

        configurationService = context.registerService(ClientlibConfiguration.class, () -> serviceConfig);

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        BeanContext beanContext = new BeanContext.Servlet(servletContext, context.bundleContext(), context.request(),
                context.response());

        clientlib2Service = (DefaultClientlibService) context.registerService(ClientlibService.class, new
                DefaultClientlibService() {
                    {
                        clientlibConfig = configurationService;
                        resolverFactory = context.getService(ResourceResolverFactory.class);
                        sequencer = sequencerService;
                        lazyCreationService = creationService;
                        permissionPlugins.add(permissionPlugin);
                        activate(context.componentContext());
                    }
                });
        // TODO: MockOsgi.activate(clientlib2Service, context.bundleContext()); should work but doesn't

        rendererContext = RendererContext.instance(beanContext, context.request());

        executorService = Executors.newFixedThreadPool(2);

        // necessary since SlingUrl.buildUrl uses XSS now. :-(
        XSSFilter xssFilter = mock(XSSFilter.class);
        context.registerService(XSSFilter.class, xssFilter);
        when(xssFilter.isValidHref(anyString())).thenReturn(true);
        ServiceHandle xssapihandle = (ServiceHandle) FieldUtils.readStaticField(com.composum.sling.core.util.XSS.class, "XSSAPI_HANDLE", true);
        FieldUtils.writeField(xssapihandle, "service", context.registerInjectActivateService(new XSSAPIImpl()), true);
    }

    @After
    public void teardownFramework() {
        assertTrue(executorService.shutdownNow().isEmpty());
    }

    @SafeVarargs
    protected final <T> T[] array(T... elements) {
        return elements;
    }

    @SafeVarargs
    protected final <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    protected void createFile(String dir, String filename) {
        context.build().resource(dir).file(filename, new ByteArrayInputStream((dir + "/" + filename).getBytes()));
    }

    protected Set<ClientlibLink> getRenderedClientlibs() {
        return rendererContext.getRenderedClientlibs();
    }

    @SuppressWarnings("unchecked")
    protected <T extends ClientlibElement> T getClientlibs2(String path, Clientlib.Type type) {
        return (T) clientlib2Service.resolve(new ClientlibRef(type, path, false, null), context.resourceResolver());
    }

    protected String checkDeliveredContent(ClientlibElement lib, String expectedContent, String expectedProcessedLibs)
            throws Exception {
        ClientlibService.ClientlibInfo hints = clientlib2Service.prepareContent(context.request(), lib.getRef(),
                false, null,
                false, "thisisnotahash", -1);
        Calendar now = GregorianCalendar.getInstance();
        Calendar lastModified = hints.lastModified;
        assertFalse(hints.toString(), lastModified.before(setupTime));
        assertFalse(hints.toString(), lastModified.after(now));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientlib2Service.deliverContent(context.resourceResolver(), lib.getRef(), false, out, null);
        assertEquals(hints.toString(), Long.valueOf(out.toByteArray().length), hints.size);
        assertEquals(expectedContent, new String(out.toByteArray()));

        ProcessorContext ctx = new ProcessorContext(context.request(), context.resourceResolver(), executorService,
                false, false);
        ProcessingVisitor visitor = new ProcessingVisitor(lib, clientlib2Service, out, null, ctx);
        visitor.execute();
        assertEquals(expectedProcessedLibs, visitor.getProcessedElements().toString());
        return visitor.getHash();
    }

    protected void verifyEqualHashesOfVisitors(ClientlibElement clientlib) throws IOException, RepositoryException {
        debuggingMode = false;
        UpdateTimeVisitor updateTimeVisitor = new UpdateTimeVisitor(clientlib, clientlib2Service, context
                .resourceResolver());
        updateTimeVisitor.execute();
        String updateHash = updateTimeVisitor.getHash();

        ProcessorContext processorContext = new ProcessorContext(this.context.request(), this.context
                .resourceResolver(), executorService, false, true);
        ProcessingVisitor processingVisitor = new ProcessingVisitor(clientlib, clientlib2Service, new
                ByteArrayOutputStream(), null, processorContext);
        processingVisitor.execute();
        String processingHash = processingVisitor.getHash();

        RenderingVisitor renderingVisitor = new RenderingVisitor(clientlib, rendererContext);
        renderingVisitor.execute();
        String renderingHash = renderingVisitor.getHash();

        debuggingMode = true;
        renderingVisitor = new RenderingVisitor(clientlib, rendererContext);
        renderingVisitor.execute();
        String renderingHash2 = renderingVisitor.getHash();

        assertEquals(updateHash, processingHash);
        assertEquals(renderingHash, processingHash);
        assertEquals(renderingHash2, processingHash);
    }

    protected String canonicHashes(Object result) {
        return null == result ? null : result.toString().replaceAll("@" + AbstractClientlibVisitor.HASH_PATTERN
                .pattern(), "@{hash}");
    }

}
