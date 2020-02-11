package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.test.util.JcrTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Some tests for {@link SourceModel}. */
public class SourceModelTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected final Answer exceptionThrowingAnswer = (invocation) -> {
        throw new AssertionError("Invocation not mocked: " + invocation);
    };
    protected NodesConfiguration config = mock(NodesConfiguration.class, exceptionThrowingAnswer);
    protected BeanContext.Service beanContext;
    protected ResourceResolver resolver;
    protected Resource resource;
    protected SourceModel model;

    @Before
    public void setup() throws Exception {
        JcrTestUtils.importCnd("/nodes/testingNodetypes.cnd", context.resourceResolver());
        JcrTestUtils.importTestPackage("/jcr_root/content/composum/nodes/console/test/sourcemodel",
                context.resourceResolver());
        resolver = context.resourceResolver();
        resource = resolver.getResource("/content/composum/nodes/console/test/sourcemodel");
        beanContext = new BeanContext.Service(context.request(), context.response(), resource,
                context.resourceResolver());
        model = new SourceModel(config, beanContext, resource);
        Mockito.doReturn(ResourceFilter.ALL).when(config).getSourceNodesFilter();
    }

    @Ignore
    @Test
    public void printContent() {
        Resource contentResource = context.resourceResolver().getResource("/content");
        JcrTestUtils.listResourcesRecursively(contentResource);
        JcrTestUtils.printResourceRecursivelyAsJson(contentResource);
    }

    @Test
    public void listPackage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.writePackage(out, "thegroup", "thename", "1.0");
        String zipContents = getZipContentOverview(out);
        System.out.println(zipContents);
        assertThat(zipContents, is("META-INF/vault/properties.xml : 442 | 2916747502\n" +
                "META-INF/vault/filter.xml : 160 | 1687942330\n" +
                "jcr_root/content/.content.xml : 190 | 2531735275\n" +
                "jcr_root/content/composum/.content.xml : 190 | 2531735275\n" +
                "jcr_root/content/composum/nodes/.content.xml : 190 | 2531735275\n" +
                "jcr_root/content/composum/nodes/console/.content.xml : 190 | 2531735275\n" +
                "jcr_root/content/composum/nodes/console/test/.content.xml : 190 | 2531735275\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/.content.xml : 211 | 2724194649\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/.content.xml : 204 | 3438455284\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/.content.xml : 317 | 3793538747\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/cancel/.content.xml : 275 | 902577937\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/configuration/.content.xml : 286 | 3561449388\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/.content.xml : 196 | 1992426303\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/CreateConfig/.content.xml : 301 | 2322230755\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/ChangeConfig/.content.xml : 301 | 2706073434\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/.content.xml : 204 | 3438455284\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg : 76910 | 2714537933\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg.dir/.content.xml : 344 | 1511749529\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg : 76910 | 2714537933\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg.dir/.content.xml : 1104 | 1828430221\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile : 83358 | 2844564088\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile.dir/.content.xml : 227 | 1283844817\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/.content.xml : 461 | 1613583327\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/folderbinprop.binary : 20 | 2592797726\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/_jcr_content/binprop.binary : 20 | 2592797726\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/.content.xml : 3430 | 783664152\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary.binary : 86 | 1434328335\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary : 86 | 1434328335\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml : 204 | 3438455284\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/401.jsp : 650 | 442210237\n"));
    }

    @Test
    public void listArchive() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.writeArchive(out);
        String zipContents = getZipContentOverview(out);
        System.out.println(zipContents);
        assertThat(zipContents, is(".content.xml : 211 | 2724194649\n" +
                "i18n/.content.xml : 204 | 3438455284\n" +
                "i18n/de/.content.xml : 317 | 3793538747\n" +
                "i18n/de/cancel/.content.xml : 275 | 902577937\n" +
                "i18n/de/configuration/.content.xml : 286 | 3561449388\n" +
                "i18n/de/config/.content.xml : 196 | 1992426303\n" +
                "i18n/de/config/CreateConfig/.content.xml : 301 | 2322230755\n" +
                "i18n/de/config/ChangeConfig/.content.xml : 301 | 2706073434\n" +
                "assetsfolder/.content.xml : 204 | 3438455284\n" +
                "assetsfolder/plain.jpg : 76910 | 2714537933\n" +
                "assetsfolder/plain.jpg.dir/.content.xml : 344 | 1511749529\n" +
                "assetsfolder/withadditionaldata.jpg : 76910 | 2714537933\n" +
                "assetsfolder/withadditionaldata.jpg.dir/.content.xml : 1104 | 1828430221\n" +
                "assetsfolder/_nt_resourcewithoutfile : 83358 | 2844564088\n" +
                "assetsfolder/_nt_resourcewithoutfile.dir/.content.xml : 227 | 1283844817\n" +
                "ntunstructuredwithjcrcontent/.content.xml : 461 | 1613583327\n" +
                "ntunstructuredwithjcrcontent/folderbinprop.binary : 20 | 2592797726\n" +
                "ntunstructuredwithjcrcontent/_jcr_content/binprop.binary : 20 | 2592797726\n" +
                "subfolder/.content.xml : 3430 | 783664152\n" +
                "subfolder/_jcr_content/propertytest/binary.binary : 86 | 1434328335\n" +
                "subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary : 86 | 1434328335\n" +
                "subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml : 204 | 3438455284\n" +
                "subfolder/401.jsp : 650 | 442210237\n"));
    }

    @Nonnull
    protected String getZipContentOverview(ByteArrayOutputStream out) throws IOException {
        StringBuilder buf = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                int size = IOUtils.toByteArray(zip).length;
                buf.append(entry.getName()).append(" : ").append(size).append(" | ").append(entry.getCrc()).append("\n");
                zip.closeEntry();
            }
        }
        return buf.toString();
    }

}
