package com.composum.sling.nodes.servlet;

import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.test.util.JcrTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.vault.fs.Mounter;
import org.apache.jackrabbit.vault.fs.api.AccessType;
import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.AggregateManager;
import org.apache.jackrabbit.vault.fs.api.Artifact;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.fs.impl.AggregateImpl;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSAXFormatter;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.fs.io.DocViewFormat;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.jackrabbit.vault.packaging.impl.PackagingImpl;
import org.apache.jackrabbit.vault.util.xml.serialize.XMLSerializer;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.xml.sax.ContentHandler;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertTrue;

/** Test how to use vault as an easier to maintain version of the SourceModel. */
public class VaultExportTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected static final String topnodePath = "/pathto/page";
    protected Resource topnode;
    protected ResourceResolver resolver;
    protected Session session;

    @Before
    public void setup() throws IOException, ParseException, RepositoryException {
        resolver = context.resourceResolver();
        session = resolver.adaptTo(Session.class);
        InputStreamReader cndReader =
                new InputStreamReader(getClass().getResourceAsStream("/nodes/testingNodetypes.cnd"));
        NodeType[] nodeTypes = CndImporter.registerNodeTypes(cndReader, session);
        ec.checkThat(nodeTypes.length, Matchers.greaterThan(0));

        topnode = context.load(false).json("/nodes/vaulttest.json", topnodePath);

        ModifiableValueMap vm = topnode.getChild("jcr:content/propertytest").adaptTo(ModifiableValueMap.class);
        vm.put("binary", getClass().getResourceAsStream("/nodes/something.bin"));
        vm.put("binary with-weird na_me", getClass().getResourceAsStream("/nodes/something.bin"));
        ModifiableValueMap vm2 = topnode.getChild("testbild.png/jcr:content").adaptTo(ModifiableValueMap.class);
        vm2.put(ResourceUtil.PROP_DATA, getClass().getResourceAsStream("/nodes/something.bin"));
        vm2.put(ResourceUtil.PROP_MIXINTYPES, new String[]{"tst:Resource"});

        topnode.getChild("jcr:content").adaptTo(ModifiableValueMap.class)
                .put(ResourceUtil.PROP_MIXINTYPES, new String[]{ResourceUtil.MIX_VERSIONABLE});

        context.load(true).binaryFile("/nodes/something.bin", "/pathto/ignored");
    }

    @Test
    public void printStructure() throws Exception {
        JcrTestUtils.printResourceRecursivelyAsJson(resolver.getResource("/pathto"));
        Resource types = resolver.getResource("/jcr:system/jcr:nodeTypes");
        System.out.println(StreamSupport.stream(types.getChildren().spliterator(), false)
                .map(Resource::getName)
                .sorted()
                .collect(Collectors.joining(",")));
    }

    @Test
    public void usePackageManager() throws Exception {
        PackageManager packageMgr = new PackageManagerImpl();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ExportOptions opts = new ExportOptions();
        // opts.setRootPath("/pathto/page");
        // opts.setMountPath("/pathto/page");
        DefaultMetaInf metainf = new DefaultMetaInf();
        Properties properties = new Properties();
        properties.setProperty(PackageProperties.NAME_GROUP, "somegroup");
        properties.setProperty(PackageProperties.NAME_NAME, "somename");
        properties.setProperty(PackageProperties.NAME_VERSION, "1");
        properties.setProperty(PackageProperties.NAME_PACKAGE_TYPE, PackageType.CONTENT.name());
        metainf.setProperties(properties);
        opts.setMetaInf(metainf);
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        filter.setImportMode(ImportMode.REPLACE);
        filter.add(new PathFilterSet(topnodePath));
        metainf.setFilter(filter);
        packageMgr.assemble(resolver.adaptTo(Session.class), opts, outStream);
        printZipContents(outStream.toByteArray());
    }

    protected void printZipContents(byte[] zipBytes) throws IOException {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        ZipEntry zipEntry;
        while ((zipEntry = zip.getNextEntry()) != null) {
            System.out.println(zipEntry.getName());
            if (zipEntry.getName().equals("jcr_root/pathto/.content.xml") ||
                    zipEntry.getName().equals("jcr_root/pathto/page/.content.xml") ||
                    zipEntry.getName().equals("jcr_root/pathto/page/testbild.png.dir/.content.xml")
            ) {
                System.out.println(IOUtils.toString(zip));
            }
            zip.closeEntry();
        }
    }

    @Test
    public void useJcrPackageManager() throws Exception {
        Packaging packaging = new PackagingImpl();
        JcrPackageManager packageMgr = packaging.getPackageManager(session);
        Resource resource = topnode.getChild("jcr:content");

        DefaultMetaInf metainf = new DefaultMetaInf();
        Properties properties = new Properties();
        properties.setProperty(PackageProperties.NAME_GROUP, "remotepublisher");
        properties.setProperty(PackageProperties.NAME_NAME, resource.getPath());
        properties.setProperty(PackageProperties.NAME_VERSION, "1");
        properties.setProperty(PackageProperties.NAME_PACKAGE_TYPE, PackageType.CONTENT.name());
        metainf.setProperties(properties);

        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        filter.setImportMode(ImportMode.REPLACE);
        PathFilterSet filterSet = new PathFilterSet(resource.getPath());
        filter.add(filterSet);
        metainf.setFilter(filter);

        assertTrue(filter.isAncestor("/pathto"));
        assertTrue(filter.contains(resource.getPath()));
        assertTrue(filter.contains(resource.getPath() + "/jcr:content"));

        ExportOptions opts = new ExportOptions();
        opts.setMetaInf(metainf);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        packageMgr.assemble(session, opts, outStream);

        printZipContents(outStream.toByteArray());
    }

    @Test
    public void useVaultFs() throws Exception {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        filter.add(new PathFilterSet(topnode.getPath()));
        RepositoryAddress mountpoint = new RepositoryAddress("/-" + topnodePath);
        VaultFileSystem jcrfs = Mounter.mount(null, filter, mountpoint, topnodePath,
                session);
        VaultFile root = jcrfs.getRoot();
        ec.checkThat(root, Matchers.notNullValue());
        listStructure(root, "    ");
        System.out.println("----------------------------------");
        listStructure(root, null);
        // root.getChild("pathto").dump(new DumpContext(new PrintWriter(System.out, true)), true);
    }

    protected void listStructure(VaultFile item, String indent) throws RepositoryException, IOException {
        Artifact artifact = item.getArtifact();
        if (indent != null) {
            System.out.println(indent + item.getPath() + " | " + item.getAggregatePath() + " : " + item.getContentType() +
                    (artifact != null ? " | " + artifact.getClass().getSimpleName() : ""));
        } else if (artifact != null && artifact.getPreferredAccess() != AccessType.NONE) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            artifact.spool(bos);
            System.out.println(item.getPath() + " | " + item.getAggregatePath() + " : ");
            System.out.println(bos.toString());
        }
        String increasedIndent = indent != null ? indent + "    " : null;
        for (VaultFile child : item.getChildren()) {
            if (!child.isDirectory()) {
                listStructure(child, increasedIndent);
            }
        }
        for (VaultFile child : item.getChildren()) {
            if (child.isDirectory()) {
                listStructure(child, increasedIndent);
            }
        }
    }

    @Test
    public void printXmls() throws Exception {
        printXml(topnodePath);
        printXml("/pathto/page/testbild.png");
        printXml("/pathto/page/testbild.png/jcr:content");
        printXml("/pathto/page/testbild.png/jcr:content/meta");
    }

    protected void printXml(String path) throws IOException, RepositoryException, URISyntaxException {
        RepositoryAddress mountpoint = new RepositoryAddress("/-" + path);
        VaultFileSystem jcrfs = Mounter.mount(null, null, mountpoint, path,
                session);
        VaultFile root = jcrfs.getRoot();
        System.out.println(root.getPath() + " | " + root.getAggregatePath());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        VaultFile contentXml = root.getChild(".content.xml");
        if (contentXml != null) {
            contentXml.getArtifact().spool(bos);
            System.out.println(bos.toString());
        }
    }

    @Test
    public void useAggregator() throws Exception {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        filter.add(new PathFilterSet(topnode.getPath()));
        VaultFileSystem jcrfs = Mounter.mount(null, filter, new RepositoryAddress("/-" + topnodePath), null,
                session); // ??? encoding of weird chars needed?
        AggregateManager agmgr = jcrfs.getAggregateManager();
        VaultFile root = jcrfs.getRoot();
        System.out.println(root.getPath());
        System.out.println(root.getAggregatePath());
        Aggregate rootAg = root.getAggregate();
        // rootAg.dump(new DumpContext(new PrintWriter(System.out, true)), true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new DocViewSerializerAvoidingProtectedProps(rootAg).writeContent(bos);
        System.out.println(bos.toString());
    }

    protected static class DocViewSerializerAvoidingProtectedProps extends DocViewSerializer {

        protected final AggregateImpl aggregate;

        public DocViewSerializerAvoidingProtectedProps(Aggregate aggregate) {
            super(aggregate);
            this.aggregate = (AggregateImpl) aggregate;
        }

        @Override
        public void writeContent(OutputStream out) throws IOException, RepositoryException {
            // build content handler and add filter in case of original xml files
            XMLSerializer ser = new XMLSerializer(out, new DocViewFormat().getXmlOutputFormat());
            DocViewSAXFormatter fmt = new DocViewSAXFormatterAvoidingProtectedProps(aggregate, ser);
            aggregate.walk(fmt);
        }

    }

    protected static class DocViewSAXFormatterAvoidingProtectedProps extends DocViewSAXFormatter {

        protected static List<String> mandatoryProperties = Arrays.asList(ResourceUtil.PROP_PRIMARY_TYPE,
                ResourceUtil.PROP_MIXINTYPES);

        protected DocViewSAXFormatterAvoidingProtectedProps(Aggregate aggregate, ContentHandler contentHandler) throws RepositoryException {
            super(aggregate, contentHandler);
        }

        @Override
        public void onProperty(Property prop, int level) throws RepositoryException {
            if (!mandatoryProperties.contains(prop.getName()) && prop.getDefinition().isProtected()) { return; }
            super.onProperty(prop, level);
        }
    }

}
