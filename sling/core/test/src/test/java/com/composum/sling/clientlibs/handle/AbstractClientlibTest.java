package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.*;
import com.composum.sling.clientlibs.service.*;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.clientlibs.service.DefaultClientlibService;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.concurrent.LazyCreationService;
import com.composum.sling.core.concurrent.LazyCreationServiceImpl;
import com.composum.sling.core.concurrent.SemaphoreSequencer;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Common code for clientlib tests.
 */
public class AbstractClientlibTest {

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
    public final void setupFramework() throws Exception {
        setupTime = GregorianCalendar.getInstance();
        context.request().setContextPath(CONTEXTPATH);

        final SequencerService sequencerService = context.registerInjectActivateService(new SemaphoreSequencer());

        final LazyCreationService creationService = context.registerInjectActivateService(
                new LazyCreationServiceImpl());

        permissionPlugin = Mockito.mock(ClientlibPermissionPlugin.class);
        Mockito.when(permissionPlugin.categoryFilter(Matchers.anyString())).thenReturn(ResourceFilter.ALL);

        configurationService = context.registerService(ClientlibConfiguration.class, new
                ClientlibConfigurationService() {
                    {
                        cacheRoot = ClientlibConfigurationService.DEFAULT_CACHE_ROOT;
                        threadPoolMin = 0;
                        threadPoolMax = 1;
                        linkTemplate = ClientlibConfigurationService.LINK_DEFAULT_TEMPLATE;
                    }

                    @Override
                    public boolean getDebug() {
                        return AbstractClientlibTest.this.debuggingMode;
                    }

                    @Override
                    public boolean getUseMinifiedFiles() {
                        return AbstractClientlibTest.this.useMinifiedFiles;
                    }
                });

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
    }

    @After
    public void teardownFramework() {
        assertTrue(executorService.shutdownNow().isEmpty());
    }

    protected <T> T[] array(T... elements) {
        return elements;
    }

    protected <T> List<T> list(T... elements) {
        return new ArrayList<T>(Arrays.asList(elements));
    }

    protected void createFile(String dir, String filename) {
        context.build().resource(dir).file(filename, new ByteArrayInputStream((dir + "/" + filename).getBytes()));
    }

    protected Set<ClientlibLink> getRenderedClientlibs() throws IllegalAccessException {
        return rendererContext.getRenderedClientlibs();
    }

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
