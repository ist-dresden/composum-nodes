package com.composum.sling.nodes.servlet;

import com.composum.sling.test.util.JcrTestUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/** Some tests for {@link SourceModel}. */
public class SourceModelTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Before
    public void setup() throws Exception {
        JcrTestUtils.importCnd("/nodes/testingNodetypes.cnd", context.resourceResolver());
        JcrTestUtils.importTestPackage("/jcr_root/content/composum/nodes/console/test/sourcemodel",
                context.resourceResolver());
    }

    @Test
    public void printContent() {
        Resource contentResource = context.resourceResolver().getResource("/content");
        JcrTestUtils.listResourcesRecursively(contentResource);
        // JcrTestUtils.printResourceRecursivelyAsJson(contentResource);
    }

}
