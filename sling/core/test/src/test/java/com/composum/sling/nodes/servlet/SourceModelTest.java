package com.composum.sling.nodes.servlet;

import com.composum.sling.test.util.JcrTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.MemoryArchive;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import javax.jcr.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Some tests for {@link SourceModel}. */
public class SourceModelTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Before
    public void setup() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        Thread writer = new Thread() {
            @Override
            public void run() {
                ZipOutputStream zip = new ZipOutputStream(out);
                File jcrRootDir = new File(getClass().getResource("/jcr_root").getFile());
                System.out.println(Arrays.asList(jcrRootDir.list()));

                listFilesRecursively(jcrRootDir).filter(File::isFile).forEach(file -> {
                    try {
                        String path = "jcr_root" + StringUtils.removeStart(file.getPath(), jcrRootDir.getPath());
                        System.out.println(path);
                        ZipEntry entry = new ZipEntry(path);
                        zip.putNextEntry(entry);
                        try (FileInputStream fileContent = new FileInputStream(file)) {
                            IOUtils.copy(fileContent, zip);
                        }
                        zip.closeEntry();
                        zip.flush();
                    } catch (Exception e) {
                        ec.addError(e);
                    }
                });
                try {
                    zip.close();
                } catch (IOException e) {
                    // ignore: importer does not read the directory that's written now.
                }
            }
        };
        try {
            writer.setDaemon(true);
            writer.start();

            Importer importer = new Importer();
            MemoryArchive archive = new MemoryArchive(false);
            archive.run(in);
            importer.run(archive, context.resourceResolver().getResource("/").adaptTo(Node.class));
        } finally {
            writer.interrupt();
            Thread.sleep(500);
            if (writer.isAlive()) { writer.stop(); }
        }
    }

    public Stream<File> listFilesRecursively(File parent) {
        return Stream.concat(Stream.of(parent), Stream.concat(
                Arrays.stream(parent.listFiles()).filter(File::isFile),
                Arrays.stream(parent.listFiles()).filter(File::isDirectory).flatMap(this::listFilesRecursively)
        ));
    }

    @Test
    public void printContent() {
        Resource contentResource = context.resourceResolver().getResource("/content");
        JcrTestUtils.listResourcesRecursively(contentResource);
        // JcrTestUtils.printResourceRecursivelyAsJson(contentResource);
    }

}
