package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.servlet.ClientlibCategoryServlet;
import com.composum.sling.clientlibs.servlet.ClientlibServlet;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.composum.sling.clientlibs.handle.Clientlib.Type.link;
import static com.composum.sling.clientlibs.handle.ClientlibLink.Kind.CATEGORY;
import static com.composum.sling.clientlibs.handle.ClientlibLink.Kind.CLIENTLIB;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import static com.composum.sling.clientlibs.handle.Clientlib.Type.js;

/**
 * Tests for {@link ClientlibLink}.
 */
public class ClientlibLinkTest extends AbstractClientlibTest {

    @Before
    public void setup() {
        createFile("/libs", "somefile.js");
        createFile("/libs", "somefile.min.js");
        createFile("/libs", "nomin.js");
        createFile("/libs", "onlymin.min.js");

        context.build()
                .resource("/apps/clientlib", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER, ResourceUtil.PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                        Clientlib.PROP_CATEGORY, array("cat1", "cat2"))
                .resource("js", ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_ORDERED_FOLDER).commit();

        context.build().commit();
    }

    @Test
    public void getUrlForFile() throws Exception {
        ClientlibRef ref = new ClientlibRef(js, "/libs/somefile.js", false, null);

        ClientlibFile file = (ClientlibFile) clientlib2Service.resolve(ref, context.resourceResolver());
        assertEquals(CONTEXTPATH + "/libs/somefile.js", file.makeLink().getUrl(rendererContext));
        assertTrue(ref.isSatisfiedby(file.makeLink()));

        // useMinifiedFiles doesn't matter for getUrl on files:
        useMinifiedFiles = true;
        assertEquals(CONTEXTPATH + "/libs/somefile.js", file.makeLink().getUrl(rendererContext));

        file = (ClientlibFile) clientlib2Service.resolve(ref, context.resourceResolver());
        // but useMinifiedFiles matters when resolving files:
        assertEquals(CONTEXTPATH + "/libs/somefile.min.js", file.makeLink().getUrl(rendererContext));
        assertTrue(ref.isSatisfiedby(file.makeLink())); // despite of .min...

        // if it is (brokenly) referenced as min
        ref = new ClientlibRef(js, "/libs/somefile.min.js", false, null);
        file = (ClientlibFile) clientlib2Service.resolve(ref, context.resourceResolver());
        // it just stays that way
        assertEquals(CONTEXTPATH + "/libs/somefile.min.js", file.makeLink().getUrl(rendererContext));
        assertTrue(ref.isSatisfiedby(file.makeLink()));
    }

    @Test
    public void getUrlForFileWithoutMinifiedVersion() throws Exception {
        useMinifiedFiles = true;

        // if there is no minified version
        ClientlibRef ref = new ClientlibRef(js, "/libs/nomin.js", false, null);
        ClientlibFile file = (ClientlibFile) clientlib2Service.resolve(ref, context.resourceResolver());
        // it just stays that way
        ClientlibLink link = file.makeLink();
        assertEquals(CONTEXTPATH + "/libs/nomin.js", link.getUrl(rendererContext));
        assertTrue(ref.isSatisfiedby(link));
    }

    @Test
    public void getUrlForFileWithoutUnminifiedVersion() throws Exception {
        // if there is no unminified version
        ClientlibRef ref = new ClientlibRef(js, "/libs/onlymin.js", false, null);
        ClientlibFile file = (ClientlibFile) clientlib2Service.resolve(ref, context.resourceResolver());
        // the minified version is rendered
        ClientlibLink link = file.makeLink();
        assertEquals(CONTEXTPATH + "/libs/onlymin.min.js", link.getUrl(rendererContext));
        assertTrue(ref.isSatisfiedby(link));
    }

    @Test
    public void getUrlForClientlib() throws Exception {
        Clientlib lib = new Clientlib(js, context.resourceResolver().getResource("/apps/clientlib"));
        assertEquals("[cat1, cat2]", lib.getCategories().toString());
        useMinifiedFiles = false;
        ClientlibLink link = lib.makeLink();

        assertEquals(CONTEXTPATH + "/apps/clientlib.js", link.getUrl(rendererContext));
        useMinifiedFiles = true;
        assertEquals(CONTEXTPATH + "/apps/clientlib.min.js", link.getUrl(rendererContext));

        assertTrue(lib.getRef().isSatisfiedby(link));
        ClientlibLink hashLink = link.withHash("thehash");
        assertTrue(lib.getRef().isSatisfiedby(hashLink));

        useMinifiedFiles = false;
        assertEquals(CONTEXTPATH + "/apps/clientlib.js/thehash/clientlib.js", hashLink.getUrl(rendererContext));
        useMinifiedFiles = true;
        assertEquals(CONTEXTPATH + "/apps/clientlib.min.js/thehash/clientlib.min.js", hashLink.getUrl(rendererContext));

    }

    @Test
    public void getUrlForClientlibCategory() throws Exception {
        final String category = "thecat";
        ClientlibRef ref = ClientlibRef.forCategory(js, category, false, null);
        ClientlibCategory cat = new ClientlibCategory(ref, asList(context.resourceResolver().getResource("/apps/clientlib")));
        ClientlibLink link = cat.makeLink();

        useMinifiedFiles = false;
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.js/thecat.js", link.getUrl(rendererContext));
        useMinifiedFiles = true;
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.min.js/thecat.js", link.getUrl(rendererContext));

        // a clientlib ref to a category whould match the clientlib link to that category
        assertTrue(ref.isSatisfiedby(link));
        ClientlibLink hashLink = link.withHash("thehash");
        assertTrue(ref.isSatisfiedby(hashLink));

        useMinifiedFiles = false;
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.js/thehash/thecat.js", hashLink.getUrl(rendererContext));
        useMinifiedFiles = true;
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.min.js/thehash/thecat.js", hashLink.getUrl(rendererContext));
    }

    @Test
    public void urlRendering() {
        for (String uri : asList("//example.net/bla/blu", "http://example.net/bluf/blaeh", "https://example.net/testsestest?hi=ho")) {
            ClientlibLink lnk = new ClientlibLink(link, ClientlibLink.Kind.EXTERNALURI, uri, null);
            assertEquals(uri, lnk.getUrl(rendererContext));
            ClientlibExternalUri element = new ClientlibExternalUri(link, uri, null);
            assertTrue(element.getRef().isSatisfiedby(element.makeLink()));
        }
    }

    @Test
    public void hashSuffixRendering() {
        assertEquals(CONTEXTPATH + "/apps/clientlib.js", new ClientlibLink(js, CLIENTLIB, "/apps/clientlib", null).getUrl(rendererContext));
        assertEquals(CONTEXTPATH + "/apps/clientlib.js/Nnk0BQAAAAA/clientlib.js", new ClientlibLink(js, CLIENTLIB, "/apps/clientlib", null).withHash("Nnk0BQAAAAA").getUrl(rendererContext));
        assertEquals("Nnk0BQAAAAA", ClientlibServlet.parseHashFromSuffix("/Nnk0BQAAAAA/clientlib.js"));
        assertEquals(null, ClientlibServlet.parseHashFromSuffix(null));
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.js/cat1.js", new ClientlibLink(js, CATEGORY, "cat1", null).getUrl(rendererContext));
        assertEquals(CONTEXTPATH + "/bin/cpm/nodes/clientlibs.js/nqLDcqc8yMo/cat1.js", new ClientlibLink(js, CATEGORY, "cat1", null).withHash("nqLDcqc8yMo").getUrl(rendererContext));
        assertEquals(Pair.of("cat1", "nqLDcqc8yMo"), ClientlibCategoryServlet.parseCategoryAndHashFromSuffix("/nqLDcqc8yMo/cat1.js"));
        assertEquals(Pair.of("cat1", null), ClientlibCategoryServlet.parseCategoryAndHashFromSuffix("/cat1.js"));
    }

}
