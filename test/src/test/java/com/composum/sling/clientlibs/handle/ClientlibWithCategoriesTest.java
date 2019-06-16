package com.composum.sling.clientlibs.handle;

import com.composum.sling.clientlibs.processor.RenderingVisitor;
import com.composum.sling.clientlibs.service.ClientlibPermissionPlugin;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

import static com.composum.sling.clientlibs.handle.Clientlib.PROP_CATEGORY;
import static com.composum.sling.clientlibs.handle.Clientlib.PROP_ORDER;
import static com.composum.sling.clientlibs.handle.Clientlib.Type.js;
import static com.composum.sling.clientlibs.handle.ClientlibResourceFolder.PROP_DEPENDS;
import static com.composum.sling.clientlibs.handle.ClientlibResourceFolder.PROP_EMBED;
import static com.composum.sling.core.util.ResourceUtil.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests related to clientlibs with categories. This verifies only that categories are used correctly to
 * reference client libraries - other features are tested in {@link ClientlibTest}.
 */
public class ClientlibWithCategoriesTest extends AbstractClientlibTest {

    @Before
    public void setupClientlibs() throws Exception {
        createFile("/libs", "1.js");
        createFile("/libs", "2.1.js");
        createFile("/apps", "2.2.js");
        createFile("/apps", "3.1.js");
        createFile("/apps", "3.2.js");

        context.build().resource("/libs/1", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER,
                PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                PROP_CATEGORY, array("cat1", "multicat"), PROP_ORDER, "100")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER, PROP_EMBED, array("1.js"));

        context.build().resource("/apps/2.1", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER,
                PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                PROP_CATEGORY, array("unusedcat", "cat2", "whatever"), PROP_ORDER, "100")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("category:cat1"), PROP_EMBED, array("2.1.js"));

        context.build().resource("/apps/2.2", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER,
                PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                PROP_CATEGORY, array("unusedcat", "cat2", "whatever"), PROP_ORDER, "200")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("category:cat1"), PROP_EMBED, array("2.2.js"));

        context.build() // shadowed by /apps/3 and unused
                .resource("/libs/3", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER, PROP_RESOURCE_TYPE,
                        Clientlib.RESOURCE_TYPE, PROP_CATEGORY, array("cat3"), PROP_ORDER, "100")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER);

        context.build().resource("/apps/3", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER,
                PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                PROP_CATEGORY, array("cat3"), PROP_ORDER, "200")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER,
                        PROP_EMBED, array("3.1.js", "category:cat2", "3.2.js"));

        context.build().resource("/apps/embed2twice", PROP_PRIMARY_TYPE, TYPE_SLING_FOLDER,
                PROP_RESOURCE_TYPE, Clientlib.RESOURCE_TYPE,
                PROP_CATEGORY, array("embed2twice"), PROP_ORDER, "200")
                .resource("js", PROP_PRIMARY_TYPE, TYPE_SLING_ORDERED_FOLDER,
                        PROP_DEPENDS, array("category:cat2", "3.1.js"),
                        PROP_EMBED, array("category:cat3"));

        context.build().commit();
    }

    @Test
    public void testResolve() throws Exception {
        assertEquals("category:cat1[js:/libs/1]", getClientlibs2("category:cat1", js).toString());
        assertEquals("category:cat2[js:/apps/2.1, js:/apps/2.2]", getClientlibs2("category:cat2", js).toString());
        assertEquals("category:cat3[js:/apps/3]", getClientlibs2("category:cat3", js).toString());
    }

    @Test
    public void testRestrictedResolve() throws Exception {
        Mockito.when(permissionPlugin.categoryFilter("cat2")).thenReturn(
                new ResourceFilter.PathFilter(new StringFilter.WhiteList("^/apps/2.1")));
        assertEquals("category:cat2[js:/apps/2.1]", getClientlibs2("category:cat2", js).toString());
    }

    @Test
    public void testLinkGenerationCat3() throws Exception {
        ClientlibCategory ref = getClientlibs2("category:cat3", js);
        List<ClientlibLink> links = new RenderingVisitor(ref, rendererContext).execute().getLinksToRender();
        assertEquals("[js:category:cat1@{hash}, js:category:cat3@{hash}]", canonicHashes(links));
        assertEquals("[js:/apps/3.1.js, js:/libs/1.js, js:/libs/1, js:category:cat1@{hash}, js:/libs/2.1.js, " +
                "js:/apps/2.1, js:/apps/2.2.js, js:/apps/2.2, js:category:cat2, js:/apps/3.2.js, js:/apps/3, " +
                "js:category:cat3@{hash}]", canonicHashes(rendererContext.getRenderedClientlibs()));
    }

    @Test
    public void testLinkGenerationCat3Expanded() throws Exception {
        debuggingMode = true;
        ClientlibCategory ref = getClientlibs2("category:cat3", js);
        RenderingVisitor renderingVisitor = new RenderingVisitor(ref, rendererContext);
        List<ClientlibLink> links = renderingVisitor.execute().getLinksToRender();
        assertEquals("[js:/apps/3.1.js, js:/libs/1.js, js:/libs/2.1.js, js:/apps/2.2.js, js:/apps/3.2.js]", links
                .toString());
        assertEquals("[js:/apps/3.1.js, js:/libs/1.js, js:/libs/1, js:category:cat1, js:/libs/2.1.js, js:/apps/2.1, " +
                        "js:/apps/2.2.js, js:/apps/2.2, js:category:cat2, js:/apps/3.2.js, js:/apps/3, " +
                        "js:category:cat3]",
                getRenderedClientlibs().toString());
    }

    @Test
    public void testHashes() throws Exception {
        ClientlibCategory ref = getClientlibs2("category:cat3", js);
        RenderingVisitor renderingVisitor = new RenderingVisitor(ref, rendererContext);
        renderingVisitor.execute();
        assertEquals(renderingVisitor.getHash(), delivery());
    }


    @Test
    public void testDelivery() throws Exception {
        delivery();
    }

    protected String delivery() throws Exception {
        ClientlibCategory ref = getClientlibs2("category:cat3", js);
        return checkDeliveredContent(ref,
                "/apps/3.1.js\n\n/libs/2.1.js\n\n/apps/2.2.js\n\n/apps/3.2.js\n\n",
                "[js:/apps/3.1.js, js:/libs/1.js, js:/libs/1, js:category:cat1, js:/libs/2.1.js, js:/apps/2.1, " +
                        "js:/apps/2.2.js, js:/apps/2.2, js:category:cat2, js:/apps/3.2.js, js:/apps/3, " +
                        "js:category:cat3]");
    }

    @Test
    public void testExcludeDoubleInclusionOfC1() throws Exception {
        ClientlibCategory ref = getClientlibs2("category:embed2twice", js);
        checkDeliveredContent(ref, "/apps/3.2.js\n\n", "[js:/libs/1.js, js:/libs/1, js:category:cat1, " +
                "js:/libs/2.1.js, js:/apps/2.1, js:/apps/2.2.js, js:/apps/2.2, js:category:cat2, js:/apps/3.1.js, " +
                "js:/apps/3.2.js, js:/apps/3, js:category:cat3, js:/apps/embed2twice, js:category:embed2twice]");
    }

    @Test
    public void equalHashesOfAllVisitors() throws Exception {
        for (ClientlibElement clientlib : Arrays.<ClientlibElement>asList(getClientlibs2("category:cat1", js), getClientlibs2
                ("category:cat2", js), getClientlibs2("category:cat3", js), getClientlibs2("category:embed2twice", js)))
            verifyEqualHashesOfVisitors(clientlib);
    }

}
