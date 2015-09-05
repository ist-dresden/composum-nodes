package com.composum.sling.core.mapping;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.mapping.jcr.ObjectMapping;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 */
@Component(
        label = "Composum Package Import Service",
        description = "Provides the import of ZIPed content parts described by a package definition.",
        immediate = true,
        metatype = true
)
@Service
public class PackageImportService extends PackageMappingService implements PackageImporter {

    private static final Logger LOG = LoggerFactory.getLogger(PackageImportService.class);

    /** the instances of the various types of import strategies */
    protected NtFileImportStrategy ntFileImportStrategy = new NtFileImportStrategy();
    protected NtResourceImportStrategy ntResourceImportStrategy = new NtResourceImportStrategy();

    protected Map<String, ImportStrategy> importStrategyMap;

    //
    // export process attributes
    //

    /** the source stream for the package import */
    protected ZipInputStream zipStream;

    /** the source stream for the package import */
    protected Writer logWriter;

    protected List<File> tempFiles;

    public PackageImportService() {
        importStrategyMap = new HashMap<>();
        importStrategyMap.put(ResourceUtil.TYPE_FILE, ntFileImportStrategy);
    }

    //
    // service interface implementation
    //

    public void importPackage(InputStream input, OutputStream log, ResourceResolver resolver)
            throws RepositoryException, IOException {
        this.resolver = resolver;
        tempFiles = new ArrayList<>();
        logWriter = new OutputStreamWriter(log, MappingRules.CHARSET);
        zipStream = new ZipInputStream(input, MappingRules.CHARSET);
        RepoZipEntry entry = readMetaInf();
        while (entry != null) {
            if (entry.isDir) {
                entry = getNextEntry();
                if (MappingRules.CONTENT_NODE_FILE_NAME.equals(entry.name)) {
                    entry = importJsonContent(entry);
                } else {
                    continue;
                }
            } else {
                entry = importFile(entry);
            }
        }
        zipStream.close();
        logWriter.flush();
        // delete all created temporary files
        for (File file : tempFiles) {
            file.delete();
        }
    }

    //
    // implementation helpers...
    //

    /**
     * Reads the package definition ('META-INF' ZIP entries).
     * This must be present and read at first to ensure that
     * this definition is available on start of a package import.
     *
     * @return the next entry which has break this import for further processing
     * @throws IOException
     */
    protected RepoZipEntry readMetaInf() throws IOException {
        RepoZipEntry entry;
        entry = getNextEntry();
        if (entry.isDir && META_INF_DIR_NAME.equals(entry.path)) {
            while ((entry = getNextEntry()) != null && !entry.isDir) {
                if (META_INF_PACKAGE_NAME.equals(entry.name)) {
                    Reader reader = new InputStreamReader(zipStream);
                    JsonReader jsonReader = new JsonReader(reader);
                    servicePackage = GSON.fromJson(jsonReader, Package.class);
                    logWriter.append(GSON.toJson(servicePackage) + "\n\n");
                }
            }
        } else {
            throw new IOException("Package must contain the META-INF part at first - not found!");
        }
        return entry;
    }

    /**
     * @return the next entry which has break this import for further processing
     * @throws IOException
     */
    protected RepoZipEntry importJsonContent(RepoZipEntry entry) throws IOException, RepositoryException {
        Package.PackagePath pkgPath = servicePackage.getMatchingPath(entry.path, null);
        if (pkgPath != null) {
            Resource resource = resolver.getResource(entry.path);
            if (resource == null || pkgPath.filter.accept(resource)) {
                logEntry(entry, pkgPath.changeRule.name().toUpperCase().charAt(0));
                InputStreamReader reader = new InputStreamReader(zipStream, MappingRules.CHARSET);
                JsonReader jsonReader = new JsonReader(reader);
                if (JSON_OBJECT_EXT.equalsIgnoreCase(entry.ext)) {
                    importObject(jsonReader, resource, entry.name);
                } else {
                    MappingRules rules = servicePackage.getMappingRules(entry.path);
                    JsonUtil.importJson(
                            jsonReader,
                            resolver,
                            entry.path,
                            rules);
                }
            }
        }
        return getNextEntry();
    }

    protected void importObject(JsonReader jsonReader, Resource parent, String name)
            throws IOException, RepositoryException {
        try {
            jsonReader.beginObject();
            String className = null;
            String primaryType = null;
            String valueName = jsonReader.nextName();
            if (OBJECT_CLASS_PROPERTY.equalsIgnoreCase(valueName)) {
                className = jsonReader.nextString();
                jsonReader.nextName();
                primaryType = jsonReader.nextString();
            } else {
                primaryType = jsonReader.nextString();
                jsonReader.nextName();
                className = jsonReader.nextString();
            }
            jsonReader.nextName();
            Class<?> objectClass = Class.forName(className);
            Object object = GSON.fromJson(jsonReader, objectClass);
            jsonReader.endObject();
            Resource child = ResourceUtil.getOrCreateChild(parent, name, primaryType);
            ObjectMapping.toResource(child, object);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new IOException(ex);
        }
    }

    //
    // file import
    //

    /**
     * @return the next entry which has break this import for further processing
     * @throws IOException
     */
    protected RepoZipEntry importFile(RepoZipEntry entry) throws IOException, RepositoryException {
        Package.PackagePath pkgPath = servicePackage.getMatchingPath(entry.path, null);
        if (pkgPath != null) {
            Resource resource = resolver.getResource(entry.path);
            if (resource != null) {
                if (pkgPath.filter.accept(resource)) {
                    ResourceHandle handle = ResourceHandle.use(resource);
                    String resourcePath = handle.getPath();
                    if (resourcePath.endsWith("/" + entry.name)
                            || resourcePath.endsWith("/" + entry.name + "." + entry.ext)) {
                        // import binary data for existing resource
                        ImportStrategy strategy = null;
                        String primaryType = handle.getPrimaryType();
                        if (primaryType != null) {
                            strategy = importStrategyMap.get(primaryType);
                        }
                        if (strategy == null) {
                            String resourceType = handle.getResourceType();
                            if (resourceType != null) {
                                strategy = importStrategyMap.get(resourceType);
                            }
                        }
                        if (strategy == null) {
                            strategy = ntResourceImportStrategy;
                        }
                        entry = strategy.importEntry(handle, entry);
                    } else {
                        // import child object
                        if (JSON_FILE_EXT.equalsIgnoreCase(entry.ext)) {
                            entry = importJsonContent(entry);
                        } else if (JSON_OBJECT_EXT.equalsIgnoreCase(entry.ext)) {
                            entry = importJsonContent(entry);
                        } else {
                            // import binary data creating a new child of type 'nt:file'
                            logEntry(entry, 'F');
                            String fileChildName = entry.name;
                            if (StringUtils.isNotBlank(entry.ext)) {
                                fileChildName += "." + entry.ext;
                            }
                            ResourceHandle fileChild = ResourceHandle.use(
                                    ResourceUtil.getOrCreateChild(
                                            resource, fileChildName, ResourceUtil.TYPE_FILE));
                            entry = ntFileImportStrategy.importEntry(fileChild, entry);
                        }
                    }
                } else {
                    entry = getNextEntry();
                }
            } else {
                logEntry(entry, ' ');
                entry = getNextEntry();
            }
        } else {
            logEntry(entry, '?');
            entry = getNextEntry();
        }
        return entry;
    }

    /**
     * the strategy interface for the import of a zipped file entry
     */
    public interface ImportStrategy {

        RepoZipEntry importEntry(ResourceHandle resource, RepoZipEntry entry)
                throws IOException, RepositoryException;
    }

    /**
     * imports the 'jcr:data' property from the binary entry into the 'jcr:content' child of a resource
     */
    protected class NtFileImportStrategy implements ImportStrategy {

        @Override
        public RepoZipEntry importEntry(ResourceHandle resource, RepoZipEntry entry)
                throws IOException, RepositoryException {
            logEntry(entry, 'f');
            ResourceHandle contentRes = ResourceHandle.use(resource.getChild(ResourceUtil.CONTENT_NODE));
            if (!contentRes.isValid()) {
                contentRes = ResourceHandle.use(
                        ResourceUtil.getOrCreateChild(resource,
                                ResourceUtil.CONTENT_NODE, ResourceUtil.TYPE_RESOURCE));
            }
            contentRes.setProperty(ResourceUtil.PROP_DATA, bufferBinaryContent(entry));
            return getNextEntry();
        }
    }

    /**
     * imports the 'jcr:data' property from the binary entry into a resource
     */
    protected class NtResourceImportStrategy implements ImportStrategy {

        @Override
        public RepoZipEntry importEntry(ResourceHandle resource, RepoZipEntry entry)
                throws IOException, RepositoryException {
            logEntry(entry, 'r');
            resource.setProperty(ResourceUtil.PROP_DATA, bufferBinaryContent(entry));
            return getNextEntry();
        }
    }

    /**
     * copies all zipped content of the entry into e temporary file, registers the file for deletion
     * after import and opens the input stream of the temporary file to create the binary value
     *
     * @param entry
     * @return
     * @throws IOException
     */
    protected InputStream bufferBinaryContent(RepoZipEntry entry) throws IOException {
        File tempFile = File.createTempFile(entry.path + "/" + entry.name, entry.ext);
        FileOutputStream fileOutput = new FileOutputStream(tempFile);
        IOUtils.copy(zipStream, fileOutput);
        fileOutput.close();
        tempFiles.add(tempFile);
        return new FileInputStream(tempFile);
    }

    //
    // ZIP helpers...
    //

    public static final Pattern NAME_PATTERN = Pattern.compile("^((.*)?/)([^/]+)\\.([^.]+)$");

    public static class RepoZipEntry {

        public final ZipEntry entry;
        public final String path;
        public final String name;
        public final String ext;
        public final boolean isDir;

        public RepoZipEntry(ZipEntry entry) {
            this.entry = entry;
            String repoPath = getRepoPath(entry);
            Matcher matcher = NAME_PATTERN.matcher(repoPath);
            if (matcher.matches()) {
                String p = matcher.group(2);
                path = StringUtils.isBlank(p) ? "/" : p;
                name = matcher.group(3);
                ext = matcher.group(4);
            } else {
                String p = repoPath;
                if (p.endsWith("/")) {
                    p = p.substring(0, p.length() - 1);
                }
                path = StringUtils.isBlank(p) ? "/" : p;
                name = ext = null;
            }
            isDir = entry.isDirectory();
        }
    }

    protected RepoZipEntry getNextEntry() throws IOException {
        RepoZipEntry result = null;
        ZipEntry entry = zipStream.getNextEntry();
        if (entry != null) {
            result = new RepoZipEntry(entry);
        }
        return result;
    }

    protected static String getRepoPath(ZipEntry entry) {
        String path = entry.getName();
        if (path.startsWith(JCR_ROOT_ZIP_PREFIX)) {
            path = path.substring(JCR_ROOT_ZIP_PREFIX.length() - 1);
        }
        path = path.replaceAll(ZIP_NAMESPACE_PATTERN, "/$1:");
        return path;
    }

    // logging

    protected void logEntry(RepoZipEntry entry, char rule) throws IOException {
        logWriter.append(rule + " - " + entry.path + " " + entry.name + "." + entry.ext + "\n");
    }
}
