package com.composum.sling.test.util;

import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Some utility methods for JCR.
 */
public class JcrTestUtils {

    /** Prints the paths of the descendants of a resource. */
    public static void listResourcesRecursively(@Nullable Resource resource) {
        if (resource != null) {
            System.out.println(resource.getPath());
            resource.getChildren().forEach(JcrTestUtils::listResourcesRecursively);
        }
    }

    /**
     * Prints a resource and its subresources as JSON, depth effectively unlimited.
     */
    public static void printResourceRecursivelyAsJson(@Nullable final Resource resource) {
        if (resource != null) {
            try {
                StringWriter writer = new StringWriter();
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setHtmlSafe(true);
                jsonWriter.setLenient(true);
                jsonWriter.setSerializeNulls(false);
                jsonWriter.setIndent("    ");
                JsonUtil.exportJson(jsonWriter, resource, MappingRules.getDefaultMappingRules(), 99);
                // ensure uninterrupted printing : wait for logmessages being printed, flush
                Thread.sleep(200);
                System.err.flush();
                System.out.flush();
                System.out.println("JCR TREE FOR " + resource.getPath());
                System.out.println(writer);
                System.out.flush();
            } catch (RepositoryException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("NO RESOURCE");
        }
    }

    /**
     * Prints a resource and its subresources as JSON, depth effectively unlimited.
     */
    public static void printResourceRecursivelyAsJson(@Nullable ResourceResolver resolver, @Nullable String path) {
        if (resolver == null) {
            System.out.println("NO RESOLVER for printing resource");
        } else if (path == null) {
            System.out.println("INVALID NULL PATH");
        } else {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                printResourceRecursivelyAsJson(resource);
            } else {
                System.out.println("NO RESOURCE at " + path);
            }
        }
    }

    /**
     * Uses the varargs mechanism to easily construct an array - shorter than e.g. new String[]{objects...}.
     */
    @SafeVarargs
    @Nonnull
    public static <T> T[] array(@Nonnull T... objects) {
        return objects;
    }

    /** Imports a CND file into the repository from a classpath location, e.g. /nodes/testingNodetypes.cnd . */
    public static void importCnd(@Nonnull String location, @Nonnull ResourceResolver resolver) throws ParseException, RepositoryException, IOException {
        InputStreamReader cndReader =
                new InputStreamReader(JcrTestUtils.class.getResourceAsStream("/nodes/testingNodetypes.cnd"));
        NodeType[] nodeTypes = CndImporter.registerNodeTypes(cndReader, resolver.adaptTo(Session.class));
        if (nodeTypes == null || nodeTypes.length == 0) { throw new IllegalArgumentException("No nodetypes found."); }
    }

    /**
     * Given a location in the classpath resources, e.g. /jcr_root/content/something, this imports
     * the contents from there into the repository.
     */
    public static void importTestPackage(@Nonnull String location, @Nonnull ResourceResolver resolver) throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        ArrayList<Throwable> exceptions = new ArrayList<>();

        Thread writer = new Thread() {
            @Override
            public void run() {
                try {
                    ZipOutputStream zip = new ZipOutputStream(out);
                    File jcrRootDir = new File(getClass().getResource(location).getFile());
                    for (File file : FileUtils.listFilesAndDirs(jcrRootDir, FileFilterUtils.trueFileFilter(),
                            FileFilterUtils.trueFileFilter())) {
                        try {
                            if (file.isFile()) {
                                int jcrRootLoc = file.getPath().indexOf("/jcr_root/");
                                String path = file.getPath().substring(jcrRootLoc + 1);
                                ZipEntry entry = new ZipEntry(path);
                                zip.putNextEntry(entry);
                                try (FileInputStream fileContent = new FileInputStream(file)) {
                                    IOUtils.copy(fileContent, zip);
                                }
                                zip.closeEntry();
                                zip.flush();
                            }
                        } catch (Exception e) {
                            exceptions.add(new AssertionError("Could not import " + file, e));
                        }
                    }
                    try {
                        zip.close();
                    } catch (IOException e) {
                        // ignore: importer does not read the directory that's written now.
                    }
                } catch (RuntimeException e) {
                    exceptions.add(new AssertionError("Could not read " + location, e));
                    e.printStackTrace();
                    IOUtils.closeQuietly(out);
                    IOUtils.closeQuietly(in);
                }
            }
        };
        try {
            writer.setDaemon(true);
            writer.start();

            Importer importer = new Importer();
            ZipStreamArchive archive = new ZipStreamArchive(in);
            archive.open(true);
            importer.run(archive, resolver.getResource("/").adaptTo(Node.class));
            archive.close();
            if (importer.hasErrors()) { throw new IllegalArgumentException("Import failed!"); }
            if (!exceptions.isEmpty()) {
                for (Throwable exception : exceptions) {
                    exception.printStackTrace();
                }
                throw new AssertionError("Import failed!", exceptions.get(1));
            }
            resolver.commit();
        } finally {
            writer.interrupt();
            if (writer.isAlive()) { Thread.sleep(500); }
            writer.stop();
        }

    }

}
