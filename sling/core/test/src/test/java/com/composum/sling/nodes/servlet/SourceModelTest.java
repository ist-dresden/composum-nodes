package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.test.util.CharsetStress;
import com.composum.sling.test.util.JcrTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

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

    /** Compares results of encoding with results exported from Vault. */
    @Test
    public void testEscapeXmlAttribute() {
        String charsetstress = CharsetStress.getUTF8CharsetStress();
        ec.checkThat(model.escapeXmlAttribute(charsetstress), is("äöüÄ\\&quot;'ÖÜñóáéíóú¬áßàèìùòâêîôû &amp;&lt;&amp;>xml; &amp;euro; @%‰ ¼½¾ «™©®» „$”“€”‘£’‚¥’ &lt;b>!&lt;/b>"));
        ec.checkThat(model.escapeXmlAttribute("<p><strong>This</strong> <em>is</em> <u>some</u>&nbsp;</p><p><strike>rich</strike> te<sup>xt</sup> ev<sub>en</sub>&nbsp;with <a href=\"http://www.example.net/\" title=\"example\" target=\"_blank\">links</a> and</p><p><ul><li>with&nbsp;</li></ul></p><p><ol><li>some&nbsp;<code>code</code>.</li></ol></p>"), is("&lt;p>&lt;strong>This&lt;/strong> &lt;em>is&lt;/em> &lt;u>some&lt;/u>&amp;nbsp;&lt;/p>&lt;p>&lt;strike>rich&lt;/strike> te&lt;sup>xt&lt;/sup> ev&lt;sub>en&lt;/sub>&amp;nbsp;with &lt;a href=&quot;http://www.example.net/&quot; title=&quot;example&quot; target=&quot;_blank&quot;>links&lt;/a> and&lt;/p>&lt;p>&lt;ul>&lt;li>with&amp;nbsp;&lt;/li>&lt;/ul>&lt;/p>&lt;p>&lt;ol>&lt;li>some&amp;nbsp;&lt;code>code&lt;/code>.&lt;/li>&lt;/ol>&lt;/p>"));
    }

    /** Compares results of encoding with results exported from Vault. */
    @Test
    public void testEscapeXmlName() {
        // JCR: InvalidChar ::= '/' | ':' | '[' | ']' | '|' | '*'
        String charsetstress = CharsetStress.getUTF8CharsetStress().replaceAll("/", "");
        ec.checkThat(model.escapeXmlName("binary with-weird na_me"), is("binary_x0020_with-weird_x0020_na_me"));
        ec.checkThat(model.escapeXmlName("&<>'\\"), is("_x0026__x003c__x003e__x0027__x005c_"));
        ec.checkThat(model.escapeXmlName(charsetstress), is("äöüÄ_x005c__x0022__x0027_ÖÜñóáéíóú_x00ac_áßàèìùòâêîôû_x0020__x0026__x003c__x0026__x003e_xml_x003b__x0020__x0026_euro_x003b__x0020__x0040__x0025__x2030__x0020__x00bc__x00bd__x00be__x0020__x00ab__x2122__x00a9__x00ae__x00bb__x0020__x201e__x0024__x201d__x201c__x20ac__x201d__x2018__x00a3__x2019__x201a__x00a5__x2019__x0020__x003c_b_x003e__x0021__x003c_b_x003e_"));
    }

    @Test
    public void listPackage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.writePackage(out, "thegroup", "thename", "1.0");
        String zipContents = getZipContentOverview(out, false, false);
        System.out.println(zipContents);
        assertThat(zipContents, is("META-INF/vault/properties.xml\n" +
                "META-INF/vault/filter.xml\n" +
                "jcr_root/content/.content.xml\n" +
                "jcr_root/content/composum/.content.xml\n" +
                "jcr_root/content/composum/nodes/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/cancel/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/configuration/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/CreateConfig/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/i18n/de/config/ChangeConfig/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/plain.jpg.dir/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/withadditionaldata.jpg.dir/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/assetsfolder/_nt_resourcewithoutfile.dir/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/folderbinprop.binary\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/ntunstructuredwithjcrcontent/_jcr_content/binprop.binary\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary.binary\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml\n" +
                "jcr_root/content/composum/nodes/console/test/sourcemodel/subfolder/401.jsp\n"));
    }

    @Test
    public void listArchive() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.writeArchive(out);
        String zipContents = getZipContentOverview(out, true, true);
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
                "ntunstructuredwithjcrcontent/.content.xml : 456 | 1199963903\n" +
                "ntunstructuredwithjcrcontent/folderbinprop.binary : 20 | 2592797726\n" +
                "ntunstructuredwithjcrcontent/_jcr_content/binprop.binary : 20 | 2592797726\n" +
                "subfolder/.content.xml : 4642 | 3242412671\n" +
                "subfolder/_jcr_content/propertytest/binary.binary : 86 | 1434328335\n" +
                "subfolder/_jcr_content/propertytest/binary%20with-weird%20na_me.binary : 86 | 1434328335\n" +
                "subfolder/__folder_wi%5cth%20weird%20%22char's/.content.xml : 204 | 3438455284\n" +
                "subfolder/401.jsp : 650 | 442210237\n"));
    }

    @Nonnull
    protected String getZipContentOverview(ByteArrayOutputStream out, boolean details, boolean unpack) throws IOException {
        File basedir = new File("target").getAbsoluteFile();
        if (!basedir.exists() || !basedir.getAbsolutePath().endsWith("core/test/target")) { unpack = false; }
        basedir = basedir.toPath().resolve("sourcemodel-test").toFile();
        if (basedir.exists()) { basedir.delete(); }
        StringBuilder buf = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                byte[] bytes = IOUtils.toByteArray(zip);
                buf.append(entry.getName());
                if (details) { buf.append(" : ").append(bytes.length).append(" | ").append(entry.getCrc()); }
                buf.append("\n");
                if (unpack) {
                    File file = basedir.toPath().resolve(entry.getName()).toFile();
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fout = new FileOutputStream(file)) {
                        IOUtils.write(bytes, fout);
                    }
                }
                zip.closeEntry();
            }
        }
        return buf.toString();
    }

}
