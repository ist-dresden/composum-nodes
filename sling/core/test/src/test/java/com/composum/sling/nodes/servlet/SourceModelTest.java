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
        assertThat(zipContents, is("META-INF/vault/properties.xml : 442\n" +
                "META-INF/vault/filter.xml : 160\n" +
                "jcr_root/content/.content.xml : 190\n" +
                "jcr_root/content/composum/.content.xml : 190\n" +
                "jcr_root/content/composum/nodes/.content.xml : 190\n" +
                "jcr_root/content/composum/nodes/console/.content.xml : 190\n" +
                "jcr_root/content/composum/nodes/console/test/.content.xml : 190\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/.content.xml : 211\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/.content.xml : 204\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/.content.xml : 317\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/cancel/.content.xml : 275\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/configuration/.content.xml : 286\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/.content.xml : 196\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/CreateConfig/.content.xml : 301\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/ChangeConfig/.content.xml : 301\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/.content.xml : 204\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg : 76910\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg.dir/.content.xml : 344\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg : 76910\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg.dir/.content.xml : 1104\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile : 83358\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile.dir/.content.xml : 227\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/.content.xml : 461\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/folderbinprop.binary : 20\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/_jcr_content/binprop.binary : 20\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/.content.xml : 3430\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary.binary : 86\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary : 86\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml : 204\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/401.jsp : 650\n"));
    }

    @Test
    public void listArchive() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.writeArchive(out);
        String zipContents = getZipContentOverview(out);
        System.out.println(zipContents);
        assertThat(zipContents, is(".content.xml : 211\n" +
                "i18n/.content.xml : 204\n" +
                "i18n/de/.content.xml : 317\n" +
                "i18n/de/cancel/.content.xml : 275\n" +
                "i18n/de/configuration/.content.xml : 286\n" +
                "i18n/de/config/.content.xml : 196\n" +
                "i18n/de/config/CreateConfig/.content.xml : 301\n" +
                "i18n/de/config/ChangeConfig/.content.xml : 301\n" +
                "assetsfolder/.content.xml : 204\n" +
                "assetsfolder/plain.jpg : 76910\n" +
                "assetsfolder/plain.jpg.dir/.content.xml : 344\n" +
                "assetsfolder/withadditionaldata.jpg : 76910\n" +
                "assetsfolder/withadditionaldata.jpg.dir/.content.xml : 1104\n" +
                "assetsfolder/_nt_resourcewithoutfile : 83358\n" +
                "assetsfolder/_nt_resourcewithoutfile.dir/.content.xml : 227\n" +
                "ntunstructuredwithjcrcontent/.content.xml : 461\n" +
                "ntunstructuredwithjcrcontent/folderbinprop.binary : 20\n" +
                "ntunstructuredwithjcrcontent/_jcr_content/binprop.binary : 20\n" +
                "subfolder/.content.xml : 3430\n" +
                "subfolder/_jcr_content/propertytest/binary.binary : 86\n" +
                "subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary : 86\n" +
                "subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml : 204\n" +
                "subfolder/401.jsp : 650\n"));
    }

    @Nonnull
    protected String getZipContentOverview(ByteArrayOutputStream out) throws IOException {
        StringBuilder buf = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                int size = IOUtils.toByteArray(zip).length;
                buf.append(entry.getName()).append(" : ").append(size).append("\n");
                zip.closeEntry();
            }
        }
        return buf.toString();
    }

}
