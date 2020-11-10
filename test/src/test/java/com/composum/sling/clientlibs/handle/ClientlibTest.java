package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.DefaultLinkRenderer;
import com.composum.sling.clientlibs.processor.LinkRenderer;
import com.composum.sling.clientlibs.processor.RenderingVisitor;
import com.composum.sling.clientlibs.processor.UpdateTimeVisitor;
import com.composum.sling.clientlibs.service.ClientlibConfigurationService;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static com.composum.sling.clientlibs.handle.Clientlib.PROP_CATEGORY;
import static com.composum.sling.clientlibs.handle.Clientlib.PROP_ORDER;
import static com.composum.sling.clientlibs.handle.Clientlib.Type.js;
import static com.composum.sling.clientlibs.handle.Clientlib.Type.link;
import static com.composum.sling.clientlibs.handle.ClientlibLink.PROP_REL;
import static com.composum.sling.clientlibs.handle.ClientlibResourceFolder.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


/**
 * Tests for Clientlib rendering and processing.
 */
public class ClientlibTest extends AbstractClientlibTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibTest.class);

    private Clientlib c1;
    private Clientlib c2;
    private Clientlib c3;
    private Clientlib c4;

    private Clientlib linkit;
    private Clientlib expanded;
    /**
     * The time the clientlibs have been created in setupFramework.
     */
    private Calendar clientlibSetupTime;

    @Before
    public void setupClientlibs() throws Exception {
        clientlibSetupTime = GregorianCalendar.getInstance();

        createFile("/libs", "jslib/2.1.2/outerembed.js");
        createFile("/libs", "jslib/2.3.1/outerembed.js"); // overridden by apps:
        createFile("/apps", "jslib/2.3.1/outerembed.js");
        createFile("/libs", "jquery/2.2.4/jquery.js");
        createFile("/libs", "jslib/c2dep.js");
        createFile("/libs", "jslib/onlyc3embed.js");
        createFile("/libs", "jslib/onlyc3depend.min.js");

        context.build()
                .resource("/apps/c1", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil
                                .PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                        PROP_CATEGORY, array("c1cat", "multicat"), PROP_ORDER, "200")
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("jquery/([1-3]*:2.2.4)/jquery.js"),
                        PROP_EMBED, array("jslib/([1-3]*:2.3.1)/outerembed.js")
                )
                .siblingsMode()
                .file("c1child.js", new ByteArrayInputStream("c1child.js".getBytes()));

        context.build()
                .resource("/apps/c2", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil
                                .PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                        PROP_CATEGORY, array("c2cat", "multicat"))
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("jslib/c2dep.min.js"),
                        PROP_EMBED, array("jslib/([1-3]*:2.1.2)/outerembed.js", "c1") // overrides c1 preferred
                        // outerembed version here
                )
                .siblingsMode()
                .file("c2child.js", new ByteArrayInputStream("c2child.js".getBytes()));

        context.build()
                .resource("/libs/c3", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil
                                .PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                        PROP_CATEGORY, array("c3cat", "multicat"), PROP_ORDER, "100")
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("c1"),
                        PROP_EMBED, array("jslib/([1-3]*:2.3.1)/outerembed.js", "c2") // overrides c2 preferred
                        // outerembed version
                )
                .siblingsMode()
                .file("c3child.js", new ByteArrayInputStream("c3child.js".getBytes()))
                .resource("c3childlib", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EMBED, array("jslib/onlyc3embed.js"),
                        PROP_DEPENDS, array("jslib/onlyc3depend.js", "c1")
                );

        context.build()
                .resource("/apps/c4", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil
                                .PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                        PROP_CATEGORY, array("c4cat", "multicat"), PROP_ORDER, "100")
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("c1")
                );

        context.build()
                .resource("/apps/ex", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil
                        .PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE)
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EXPANDED, "true",
                        PROP_EMBED, array("jslib/c2dep.js"))
                .siblingsMode()
                .resource("no", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EXPANDED, "false",
                        PROP_EMBED, array("jslib/2.1.2/outerembed.js")
                )
                .resource("e", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EMBED, array("jquery/2.2.4/jquery.js")
                );
        createFile("/apps/ex/js", "rt.js");
        createFile("/apps/ex/js/no", "noexp.js");
        createFile("/apps/ex/js/e", "exp.js");

        context.build()
                .resource("/libs/linkit", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER,
                        ResourceUtil.PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE)
                .resource("link", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EMBED, array("links/root.png"))
                .siblingsMode()
                .resource("first", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_EXPANDED, "false",
                        PROP_EMBED, array("links/first.png", "//externalurl/hallooo"), PROP_REL, "firstrel"
                )
                .resource("second", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("http://example.net/something.orelse.png", "links/second.png"), PROP_REL,
                        "secondrel"
                );
        createFile("/libs/links", "root.png");
        createFile("/libs/links", "first.png");
        createFile("/libs/links", "second.png");
        createFile("/libs/linkit/link/second", "secondchild.png");

        context.build().commit();

        c1 = new Clientlib(js, context.resourceResolver().getResource("/apps/c1"));
        c2 = new Clientlib(js, context.resourceResolver().getResource("/apps/c2"));
        c3 = new Clientlib(js, context.resourceResolver().getResource("/libs/c3"));
        c4 = new Clientlib(js, context.resourceResolver().getResource("/apps/c4"));
        expanded = new Clientlib(js, context.resourceResolver().getResource("/apps/ex"));
        linkit = new Clientlib(link, context.resourceResolver().getResource("/libs/linkit"));
        assertTrue(c1.isValid() && c2.isValid() && c3.isValid() && c4.isValid() && expanded.isValid() && linkit
                .isValid());
    }

    @Test
    public void testLinkGenerationC1() throws Exception {
        RenderingVisitor renderingVisitor = new RenderingVisitor(c1, rendererContext);
        List<ClientlibLink> links = renderingVisitor.execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/c1@{hash}]", canonicHashes(links));
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1@{hash}]", canonicHashes(getRenderedClientlibs()));

        // same as testDeliveryC1 ; repeated here only for hash check. The hash isn't fixed because of varying
        // creation times, which can't easily be changed since jcr:created isn't modifiable.
        assertEquals(renderingVisitor.getHash(), deliveryC1());
    }

    @Test
    public void testLinkGenerationC1Debug() throws Exception {
        debuggingMode = true;
        List<ClientlibLink> links = new RenderingVisitor(c1, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js]", links.toString());
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1]", getRenderedClientlibs().toString());
    }

    @Test
    public void testDeliveryC1() throws Exception {
        deliveryC1();
    }

    protected String deliveryC1() throws Exception {
        return checkDeliveredContent(c1, "/apps/jslib/2.3.1/outerembed.js\n\nc1child.js\n\n",
                "[js:/libs/jquery/2.2.4/jquery" + ".js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                        ".js, js:/apps/c1]");
    }

    @Test
    public void testLinkGenerationC2() throws Exception {
        List<ClientlibLink> links = new RenderingVisitor(c2, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jquery/2.2.4/jquery.js, js:/apps/c2@{hash}]", canonicHashes
                (links));
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/libs/jquery/2.2.4/jquery.js, " +
                        "" + "js:/apps/c1/js/c1child.js, js:/apps/c1, js:/apps/c2/js/c2child.js, js:/apps/c2@{hash}]",
                canonicHashes(getRenderedClientlibs()));
    }

    @Test
    public void testLinkGenerationC2Debug() throws Exception {
        debuggingMode = true;
        List<ClientlibLink> links = new RenderingVisitor(c2, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/libs/jquery/2.2.4/jquery.js, " +
                "" + "js:/apps/c1/js/c1child.js, js:/apps/c2/js/c2child.js]", links.toString());
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/libs/jquery/2.2.4/jquery.js, " +
                        "" + "js:/apps/c1/js/c1child.js, js:/apps/c1, js:/apps/c2/js/c2child.js, js:/apps/c2]",
                getRenderedClientlibs().toString());
    }

    @Test
    public void testDeliveryC2() throws Exception {
        checkDeliveredContent(c2, "/libs/jslib/2.1.2/outerembed.js\n\nc1child.js\n\nc2child.js\n\n",
                "[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/libs/jquery/2.2.4/jquery.js, " +
                        "js:/apps/c1/js/c1child.js, js:/apps/c1, js:/apps/c2/js/c2child.js, js:/apps/c2]");
    }

    @Test
    public void testLinkGenerationC3() throws Exception {
        RenderingVisitor renderingVisitor = new RenderingVisitor(c3, rendererContext);
        List<ClientlibLink> links = renderingVisitor.execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/c1@{hash}, js:/libs/jslib/c2dep.js, " +
                "js:/libs/jslib/onlyc3depend.min.js, js:/libs/c3@{hash}]", canonicHashes(links));
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1@{hash}, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/apps/c2, " +
                "js:/libs/c3/js/c3child.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed.js, " +
                "js:/libs/c3@{hash}]", canonicHashes(getRenderedClientlibs()));
    }

    @Test
    public void testSameHashWhenRenderedTwice() throws Exception {
        LOG.info("Rendering 1");
        RenderingVisitor renderingVisitor = new RenderingVisitor(c3, rendererContext);
        renderingVisitor.execute();

        LOG.info("Rendering 2");
        RenderingVisitor renderingVisitor2 = new RenderingVisitor(c3, rendererContext);
        renderingVisitor2.execute();

        assertEquals(renderingVisitor.getHash(), renderingVisitor2.getHash());

        // same as testDeliveryC3 ; repeated here only for hash check
        assertEquals(renderingVisitor.getHash(), deliveryC3());
    }

    @Test
    public void testLinkGenerationC3Debug() throws Exception {
        debuggingMode = true;
        List<ClientlibLink> links = new RenderingVisitor(c3, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/libs/c3/js/c3child.js, " +
                "js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed.js]", links.toString());
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/apps/c2, " +
                "js:/libs/c3/js/c3child.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed.js, " +
                "js:/libs/c3]", getRenderedClientlibs().toString());
    }

    @Test
    public void testLinkGenerationC3WithPreviousC1() throws Exception {
        testLinkGenerationC1();

        RenderingVisitor renderingVisitor = new RenderingVisitor(c3, rendererContext);
        List<ClientlibLink> links = renderingVisitor.execute().getLinksToRender();
        // in comparison to testLinkGenerationC3 all stuff already mentioned in c1 is not here:
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/c3@{hash}]",
                canonicHashes(links));
        // same as only c3
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1@{hash}, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/apps/c2, " +
                "js:/libs/c3/js/c3child.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed.js, " +
                "js:/libs/c3@{hash}]", canonicHashes(getRenderedClientlibs()));
    }

    /** This is not really a test yet - just a sanity check since everything in the repo has creation date "now". */
    @Test
    public void testUpdateSimpleCheckC3() throws Exception {
        UpdateTimeVisitor visitor = new UpdateTimeVisitor(c3, clientlib2Service, context.resourceResolver());
        Calendar updateTime = visitor.execute().getLastUpdateTime();
        assertFalse(updateTime.before(clientlibSetupTime));
        assertFalse(Calendar.getInstance().before(updateTime));
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/apps/c2, " +
                "js:/libs/c3/js/c3child.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed.js, " +
                "js:/libs/c3]", visitor.getProcessedElements().toString());
    }

    @Test
    public void testDeliveryC3() throws Exception {
        deliveryC3();
    }

    protected String deliveryC3() throws Exception {
        return checkDeliveredContent(c3, "c2child.js\n\nc3child.js\n\n/libs/jslib/onlyc3embed.js\n\n",
                "[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child.js, " +
                        "js:/apps/c1, js:/libs/jslib/c2dep.js, js:/apps/c2/js/c2child.js, js:/apps/c2, " +
                        "js:/libs/c3/js/c3child.js, js:/libs/jslib/onlyc3depend.min.js, js:/libs/jslib/onlyc3embed" +
                        ".js, js:/libs/c3]");
    }

    @Test
    public void testLinkGenerationC4() throws Exception {
        List<ClientlibLink> links = new RenderingVisitor(c4, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/c1@{hash}]", canonicHashes(links));
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1@{hash}, js:/apps/c4]", canonicHashes(getRenderedClientlibs()));
    }

    @Test
    public void testLinkGenerationC4Debug() throws Exception {
        debuggingMode = true;
        List<ClientlibLink> links = new RenderingVisitor(c4, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js]", links.toString());
        assertEquals("[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, js:/apps/c1/js/c1child" +
                ".js, js:/apps/c1, js:/apps/c4]", getRenderedClientlibs().toString());
    }

    /**
     * C4 has no actual content since it just renders c1 as dependency.
     */
    @Test
    public void testDeliveryC4() throws Exception {
        checkDeliveredContent(c4, "", "[js:/libs/jquery/2.2.4/jquery.js, js:/apps/jslib/2.3.1/outerembed.js, " +
                "js:/apps/c1/js/c1child.js, js:/apps/c1, js:/apps/c4]");
    }

    @Test
    public void testLinkGenerationExpanded() throws Exception {
        List<ClientlibLink> links = new RenderingVisitor(expanded, rendererContext).execute().getLinksToRender();
        // js:/apps/ex is contained since outerembed.js is embedded since its folder has expanded=false
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jquery/2.2.4/jquery.js, js:/apps/ex/js/e/exp.js, " +
                "js:/apps/ex/js/rt.js, js:/apps/ex@{hash}]", canonicHashes(links));
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/apps/ex/js/no/noexp.js, " +
                "js:/libs/jquery/2.2.4/jquery.js, js:/apps/ex/js/e/exp.js, js:/apps/ex/js/rt.js, " +
                "js:/apps/ex@{hash}]", canonicHashes(getRenderedClientlibs()));
    }

    @Test
    public void testLinkGenerationExpandedDebug() throws Exception {
        debuggingMode = true;
        List<ClientlibLink> links = new RenderingVisitor(expanded, rendererContext).execute().getLinksToRender();
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/apps/ex/js/no/noexp.js, " +
                "js:/libs/jquery/2.2.4/jquery.js, js:/apps/ex/js/e/exp.js, js:/apps/ex/js/rt.js]", links.toString());
        assertEquals("[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/apps/ex/js/no/noexp.js, " +
                        "js:/libs/jquery/2.2.4/jquery.js, js:/apps/ex/js/e/exp.js, js:/apps/ex/js/rt.js, js:/apps/ex]",
                getRenderedClientlibs().toString());
    }

    /**
     * C4 has no actual content since it just renders c1 as dependency.
     */
    @Test
    public void testDeliveryExpanded() throws Exception {
        checkDeliveredContent(expanded, "/libs/jslib/2.1.2/outerembed.js\n\n/apps/ex/js/no/noexp.js\n\n",
                "[js:/libs/jslib/c2dep.js, js:/libs/jslib/2.1.2/outerembed.js, js:/apps/ex/js/no/noexp.js, " +
                        "js:/libs/jquery/2.2.4/jquery.js, js:/apps/ex/js/e/exp.js, js:/apps/ex/js/rt.js, js:/apps/ex]");
    }

    @Test
    public void findByCategory() {
        assertEquals("category:c1cat[js:/apps/c1]", getClientlibs2("category:c1cat", js).toString());
        assertEquals("js:/apps/c1", getClientlibs2(c1.resource.getPath(), js).toString());
        assertEquals("category:multicat[js:/apps/c2, js:/apps/c4, js:/libs/c3, js:/apps/c1]", getClientlibs2
                ("category:multicat", Clientlib.Type.js).toString());
    }

    @Test
    public void testTypeLink() throws Exception {
        List<ClientlibLink> links = new RenderingVisitor(linkit, rendererContext).execute().getLinksToRender();
        assertEquals("[link:/libs/links/root.png, link:/libs/links/first.png;rel=firstrel, " +
                "link://externalurl/hallooo;rel=firstrel, link:http://example.net/something.orelse.png;rel=secondrel," +
                "" + " link:/libs/links/second.png;rel=secondrel, link:/libs/linkit/link/second/secondchild.png;" +
                "rel=secondrel]", links.toString());
        assertEquals("[link:/libs/links/root.png, link:/libs/links/first.png;rel=firstrel, " +
                "link://externalurl/hallooo;rel=firstrel, link:http://example.net/something.orelse.png;rel=secondrel," +
                "" + " link:/libs/links/second.png;rel=secondrel, link:/libs/linkit/link/second/secondchild.png;" +
                "rel=secondrel, link:/libs/linkit]", getRenderedClientlibs().toString());
    }

    @Test
    public void testTypeLinkRendering() throws Exception {
        StringWriter writer = new StringWriter();
        LinkRenderer linkRenderer = new DefaultLinkRenderer() {{
            this.clientlibConfig = configurationService;
        }};
        linkRenderer.renderClientlibLinks(linkit, writer, context.request(), rendererContext);
        assertEquals("  <link rel=\"\" href=\"/context/libs/links/root.png\" />\n" + "  <link rel=\"firstrel\" " +
                "href=\"/context/libs/links/first.png\" />\n" + "  <link rel=\"firstrel\" " +
                "href=\"//externalurl/hallooo\" />\n" + "  <link rel=\"secondrel\" href=\"http://example" + "" +
                ".net/something.orelse.png\" />\n" + "  <link rel=\"secondrel\" href=\"/context/libs/links/second" +
                ".png\" />\n" + "  <link rel=\"secondrel\" href=\"/context/libs/linkit/link/second/secondchild.png\" " +
                "" + "/>", writer.toString());
    }

    @Test
    public void equalHashesOfAllVisitors() throws Exception {
        for (Clientlib clientlib : Arrays.asList(c1, c2, c3, c4, linkit, expanded))
            verifyEqualHashesOfVisitors(clientlib);
    }

    /** Tests weird case that the cached file is not found from the users resolver. */
    @Test
    public void testCacheInaccessible() throws Exception {
        ClientlibService.ClientlibInfo hints = clientlib2Service.prepareContent(context.request(), c1.getRef(),
                false, null,
                false, "thisisnotahash", -1);
        Calendar lastModified = hints.lastModified;
        Thread.sleep(1000);

        // resolver that can't access the cache
        ResourceResolver noAccessToCacheResolver = Mockito.spy(context.resourceResolver());
        when(noAccessToCacheResolver.getResource(Mockito.startsWith(DEFAULT_CACHE_ROOT))).thenReturn(null);
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(noAccessToCacheResolver, context
                .bundleContext());
        hints = clientlib2Service.prepareContent(request, c1.getRef(),
                false, null,
                false, "thisisnotahash", -1);
        assertNull(hints);

        // the cached file should not have been regenerated
        hints = clientlib2Service.prepareContent(context.request(), c1.getRef(),
                false, null,
                false, "thisisnotahash", -1);
        assertEquals(lastModified, hints.lastModified);
    }


}
