package com.composum.sling.core.mapping;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ObjectMapping;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
@Component(
        label = "Composum Package Export Service",
        description = "Provides the export of ZIPed content parts described by a package definition.",
        immediate = true,
        metatype = true
)
@Service
public class PackageExportService extends PackageMappingService implements PackageExporter {

    private static final Logger LOG = LoggerFactory.getLogger(PackageExportService.class);

    //
    // service and filter properties for the different resource export policies
    //

    @Property(
            label = "Child Order",
            description = "if 'on' the child order hint property is included in the content files",
            boolValue = true
    )
    public static final String CHILD_ORDER_KEY = "composum.packages.export.childOrder";
    protected boolean exportChildOrder;

    @Property(
            label = "File Filter",
            description = "the filter to determine all 'file' resources for binary export to separated "
                    + "files; default value: PrimaryType(+'^nt:(file|resource)$') "
                    + "- each resource of primary type 'nt:file' or 'nt:resource; "
                    + "such a filter is described by the filter type and pattern list: "
                    + "'Name|Path|PrimaryType|MixinType|ResourceType|All|Folder(+|-'pattern,pattern,...') "
                    + "+'...' for a 'white list' (include), -'...' for a 'black list' (exclude); "
                    + "combinations are possible using 'and|or|first|last{filter,filter,...}'",
            value = "PrimaryType(+'^nt:(file|resource)$')"
    )
    public static final String FILE_RESOURCE_FILTER_KEY = "composum.packages.export.files";
    protected ResourceFilter fileResourceFilter;

    @Property(
            label = "Content Filter",
            description = "this filter is used to determine all resources which must be exported "
                    + "as one text file if possible; the generated text file contains all content "
                    + "without the content of these nodes which are breaking the text export "
                    + "(e.g. binary nodes); default value: "
                    + "PrimaryType(+'^rep:(ACL|.*ACE)$,^cq:(EditConfig|.*Panel|Dialog$|Widget.*)$') "
                    + "- each resource of primary type 'rep:ACL' or 'rep:...ACE' "
                    + "and each resource of a 'CQ' edit/dialog type",
            value = "PrimaryType(+'^rep:(ACL|.*ACE)$,^cq:(EditConfig|.*Panel|Dialog$|Widget.*)$')"
    )
    public static final String CONTENT_RESOURCE_FILTER_KEY = "composum.packages.export.content";
    protected ResourceFilter contentResourceFilter;

    @Property(
            label = "Content Blob Filter",
            description = "this filter is used to determine all resources which must be "
                    + "exported as one text file; the generated text file contains all "
                    + "content (no breaking nodes separated); default value: "
                    + "MixinType(+'^mix:(language)$') "
                    + "- each resource of mixin type 'mix:language'",
            value = "MixinType(+'^mix:(language)$')"
    )
    public static final String CONTENT_BLOB_FILTER_KEY = "composum.packages.export.blob";
    protected ResourceFilter contentBlobResourceFilter;

    @Property(
            label = "Content Break Filter",
            description = "this filter is used during content export to check for binary "
                    + "content which breaks the content stream, in this case these resources "
                    + "are exported separately (e.g. a binary node breaks the content export); "
                    + "default value: PrimaryType(+'^nt:(file|resource)$,^(nt|sling):.*[Ff]older$') "
                    + "- each resource of a 'file' or 'folder' type",
            value = "PrimaryType(+'^nt:(file|resource)$,^(nt|sling):.*[Ff]older$')"
    )
    public static final String CONTENT_BREAK_FILTER_KEY = "composum.packages.export.break.content";
    protected ResourceFilter contentBreakFilter;

    //
    // service configuration
    //

    /** the list of filters to determine the right resource handler for a resource */
    protected ResourceFilter[] resourceHandlerFilters;

    protected Dictionary properties;

    protected void activate(ComponentContext context) {
        properties = context.getProperties();
        exportChildOrder = (Boolean) properties.get(CHILD_ORDER_KEY);
        fileResourceFilter = ResourceFilterMapping.fromString(
                (String) properties.get(FILE_RESOURCE_FILTER_KEY));
        contentResourceFilter = ResourceFilterMapping.fromString(
                (String) properties.get(CONTENT_RESOURCE_FILTER_KEY));
        contentBlobResourceFilter = ResourceFilterMapping.fromString(
                (String) properties.get(CONTENT_BLOB_FILTER_KEY));
        contentBreakFilter = ResourceFilterMapping.fromString(
                (String) properties.get(CONTENT_BREAK_FILTER_KEY));
        resourceHandlerFilters = new ResourceFilter[]{
                fileResourceFilter,
                contentResourceFilter,
                contentBlobResourceFilter,
                ObjectMapping.OBJECT_FILTER
        };
    }

    protected void deactivate(ComponentContext context) {
        properties = null;
    }

    //
    // service implementation
    //

    /** the instances of the various types of resource handlers */
    protected PathResourceHandler pathResourceHandler = new PathResourceHandler();
    protected TreeResourceHandler treeResourceHandler = new TreeResourceHandler();
    protected ContentResourceHandler contentResourceHandler = new ContentResourceHandler();
    protected ContentBlobResourceHandler contentBlobResourceHandler = new ContentBlobResourceHandler();
    protected ObjectResourceHandler objectResourceHandler = new ObjectResourceHandler();
    protected FileResourceHandler fileResourceHandler = new FileResourceHandler();

    /** the resource handler list in the order corresponding to the filter list */
    protected ResourceHandler[] resourceHandlers = new ResourceHandler[]{
            fileResourceHandler,
            contentResourceHandler,
            contentBlobResourceHandler,
            objectResourceHandler
    };

    //
    // content type variants (currently JSON is supported only)
    //

    /** the text content type to use for export - set up by constructor parameters */
    protected MappingRules.ContentNodeType contentNodeType;
    /** the content handler determined by the content type duricng construction */
    protected ContentHandler contentHandler;


    /** the set of resource paths which are already exported */
    protected TreeSet<String> exportedResources;

    /** the target stream for the package export */
    protected ZipOutputStream zipStream;

    /**
     * Creates an exporter instance with the default text content type 'json'.
     */
    public PackageExportService() {
        this(MappingRules.ContentNodeType.json);
    }

    /**
     * Creates an exporter instance with the specified text content type.
     */
    protected PackageExportService(MappingRules.ContentNodeType contentNodeType) {
        this.contentNodeType = contentNodeType;
        switch (contentNodeType) {
            case xml:
            default:
                contentHandler = new JsonContentHandler();
        }
    }

    //
    // service interface implementation
    //

    /**
     * The function to export a package described by a resource or
     * a snapshot of one resource using default package settings.
     *
     * @param output   the target stream to write the ZIP content
     * @param resource the package resource or the non package source
     * @throws RepositoryException
     * @throws IOException
     */
    public void exportPackage(OutputStream output, Resource resource)
            throws RepositoryException, IOException {
        ResourceResolver resolver = resource.getResourceResolver();

        // use package definition stored in the resource if resource is of package type
        if (Package.RESOURCE_TYPE_PACKAGE.equals(resource.getResourceType())) {
            try {
                Package pkg = (Package) ObjectMapping.fromResource(resource);
                exportPackage(output, pkg, resolver);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                throw new IOException(ex);
            }

        } else {
            // otherwise, if the resource is not a package, a snapshot of the resource content is exported
            exportPackage(output,
                    new Package(
                            RESOURCE_PACKAGE_GROUP,
                            resource.getName(),
                            RESOURCE_PACKAGE_VERSION,
                            new Package.PackageOptions(MappingRules.ChangeRule.merge),
                            new Package.PackagePath(resource.getPath())
                    ),
                    resolver);
        }
    }

    /**
     * The function to export a complete package described by a package object into a ZIP stream.
     *
     * @param output   the target stream to write the ZIP content
     * @param pkg      the package definition object
     * @param resolver the resolver to use for resource retrieval
     * @throws RepositoryException
     * @throws IOException
     */
    public void exportPackage(OutputStream output, Package pkg, ResourceResolver resolver)
            throws RepositoryException, IOException {
        this.resolver = resolver;
        servicePackage = pkg;
        exportedResources = new TreeSet<>();
        zipStream = new ZipOutputStream(output, MappingRules.CHARSET);
        writeMetaInf(); // write META-INF first (!)
        for (Package.PackagePath path : servicePackage.pathList) {
            export(path); // export all package paths in the package definition
        }
        zipStream.close();
    }

    //
    // implementation helpers...
    //

    /**
     * Export one path from the package path list.
     *
     * @param pkgPath
     * @throws IOException
     * @throws RepositoryException
     */
    protected void export(Package.PackagePath pkgPath)
            throws IOException, RepositoryException {
        Resource pathRoot = resolver.getResource(pkgPath.path);
        if (pathRoot != null) {
            exportResource(pathRoot, null);
        }
    }

    /**
     * Checks a resource for including in the export stream; to include a resource they
     * must be real existing and accepted by the resource filter of the mapping rules.
     *
     * @param resource the resource to check
     * @return 'true', if the resource should be included in the export
     */
    protected boolean isIncluded(Resource resource) {
        boolean result = false;
        try {
            MappingRules rules = servicePackage.getMappingRules(resource.getPath());
            result = rules.resourceFilter.accept(resource)
                    && !ResourceUtil.isNonExistingResource(resource)
                    && !ResourceUtil.isSyntheticResource(resource)
                    && !ResourceUtil.isStarResource(resource);
        } catch (Exception ex) {
            // if an exceptions is thrown the resource doesn't match
        }
        return result;
    }

    /**
     * Writes the package definition ('META-INF' ZIP entries).
     * This must be done at first to ensure that this definition is available on start of a package import.
     *
     * @throws IOException
     */
    protected void writeMetaInf() throws IOException {
        createZipEntry(META_INF_DIR_NAME + "/");
        createZipEntry(META_INF_DIR_NAME + "/" + META_INF_PACKAGE_NAME + "." + contentNodeType.name());
        Writer writer = new OutputStreamWriter(zipStream, MappingRules.CHARSET);
        JsonWriter jsonWriter = createJsonWriter(writer);
        GSON.toJson(servicePackage, Package.class, jsonWriter);
        jsonWriter.flush();
        zipStream.closeEntry();
    }

    /**
     * The export function to embed the path to a resource in the export stream.
     *
     * @param resource
     * @throws RepositoryException
     * @throws IOException
     */
    protected void exportParent(Resource resource, TreeSet<String> exportedContentResources)
            throws RepositoryException, IOException {
        if (resource != null) {
            String path = resource.getPath();
            if (!exportedResources.contains(path)) {
                exportParent(resource.getParent(), exportedContentResources);
                pathResourceHandler.exportResource(resource, exportedContentResources);
            }
        }
    }

    /**
     * The export function to write one resource with all the appropriate content to the export stream.
     * This function determines the right ResourceHandler (default is the TreeHandler) ans delegates
     * the export call to this handler. The handler must check the relevance of this resource and
     * must set the 'already exported' marker for this resource if the resource is exported really.
     *
     * @param resource
     * @throws RepositoryException
     * @throws IOException
     */
    protected void exportResource(Resource resource, TreeSet<String> exportedContentResources)
            throws RepositoryException, IOException {
        if (resource != null) {
            String path = resource.getPath();
            if (!exportedResources.contains(path)) {
                ResourceHandler resourceHandler = treeResourceHandler;
                for (int i = 0; i < resourceHandlerFilters.length; i++) {
                    if (resourceHandlerFilters[i].accept(resource)) {
                        resourceHandler = resourceHandlers[i];
                    }
                }
                resourceHandler.exportResource(resource, exportedContentResources);
            } else {
                LOG.warn("resource already exported: " + path);
            }
        }
    }

    //
    // policies for different resource types
    //

    protected interface ResourceHandler {

        /**
         * @param resource
         * @throws RepositoryException
         * @throws IOException
         */
        void exportResource(Resource resource, TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException;
    }

    /**
     * Handles resources which are necessary to build the path to a resource which has to be exported.
     * Such a path resource contains their properties and the 'jcr:content' child in the content file
     * but no other child in the hierarchy.
     */
    protected class PathResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null) {
                String path = resource.getPath();
                exportedResources.add(path);
                if (exportedContentResources != null && exportedContentResources.contains(path)) {
                    // write path (directory) only for resources already exported into a content file
                    // if a content export is broken then some resources are exported to the content file
                    // but the paths to the resources which have broken the content stream must be
                    // present in the ZIP package as directory items (without any content file)
                    String zipPath = getZipPath(resource);
                    createZipEntry(zipPath + "/");
                } else {
                    // write path and path node content properties for all regular paths not exported before
                    createDirAndContent(resource);
                    contentHandler.writeResource(resource, ContentHandler.ChildPolicy.content, null);
                    zipStream.closeEntry();
                }
            }
        }
    }

    /**
     * The resource handler to export a repository hierarchy to the ZIP package.
     * This handler exports all resources which should be included in the package an which
     * are not handled by any other export handler; this reflects the hierarchy into the
     * directory structure of the ZIP package.
     */
    protected class TreeResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null) {
                if (isIncluded(resource)) {
                    // if the package filter accepts the resource export the content file
                    exportedResources.add(resource.getPath());
                    // build the path top the resource if not exported before
                    exportParent(resource.getParent(), exportedContentResources);
                    // write the content file for the resource
                    createDirAndContent(resource);
                    ContentExportHints hints = new ContentExportHints();
                    contentHandler.writeResource(resource,
                            ContentHandler.ChildPolicy.content, hints);
                    zipStream.closeEntry();
                    // if content was broken export all brteaking resources
                    flushContentBreakingResources(hints);
                }
                // traverse during the hierarchy and check for resources to export
                for (Resource child : resource.getChildren()) {
                    if (!ResourceUtil.CONTENT_NODE.equals(child.getName())) {
                        PackageExportService.this.exportResource(child, exportedContentResources);
                    }
                }
            }
        }
    }

    /**
     * Handles resources which are selected for an export in one content file which break option.
     * All properties and children are exported into on content file except on of the children
     * is a 'content breaking' resource. These breaking resource are collected during content
     * export and exported in separate files after the content file export has been done.
     */
    protected class ContentResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null && isIncluded(resource)) {
                exportedResources.add(resource.getPath());
                exportParent(resource.getParent(), exportedContentResources);
                String path = getZipPath(resource);
                createZipEntry(path + "." + contentNodeType.name());
                ContentExportHints hints = new ContentExportHints();
                contentHandler.writeResource(resource,
                        ContentHandler.ChildPolicy.deep, hints);
                zipStream.closeEntry();
                flushContentBreakingResources(hints);
            }
        }
    }

    /**
     * The strict content file handler exports all properties and the complete hierarchy
     * of a resources children into one content file of type text (JSON); no content
     * breaking rules are accepted. This is useful if a resource should be exported
     * in one text file and is of an content 'breaking type' (e.g. language folders).
     */
    protected class ContentBlobResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null && isIncluded(resource)) {
                exportedResources.add(resource.getPath());
                exportParent(resource.getParent(), exportedContentResources);
                String path = getZipPath(resource);
                createZipEntry(path + "." + contentNodeType.name());
                contentHandler.writeResource(resource,
                        ContentHandler.ChildPolicy.deep, null);
                zipStream.closeEntry();
            }
        }
    }

    /**
     *
     */
    protected class ObjectResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null && isIncluded(resource)) {
                Object object = null;
                try {
                    object = ObjectMapping.fromResource(resource);
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
                if (object != null) {
                    exportedResources.add(resource.getPath());
                    exportParent(resource.getParent(), exportedContentResources);
                    ResourceHandle handle = ResourceHandle.use(resource);
                    String path = getZipPath(resource);
                    createZipEntry(path + "." + JSON_OBJECT_EXT);
                    OutputStreamWriter streamWriter = new OutputStreamWriter(zipStream, MappingRules.CHARSET);
                    JsonWriter jsonWriter = createJsonWriter(streamWriter);
                    jsonWriter.beginObject();
                    jsonWriter.name(OBJECT_CLASS_PROPERTY).value(object.getClass().getName());
                    jsonWriter.name(OBJECT_TYPE_PROPERTY).value(handle.getPrimaryType());
                    jsonWriter.name(OBJECT_DATA_PROPERTY);
                    GSON.toJson(object, object.getClass(), jsonWriter);
                    jsonWriter.endObject();
                    jsonWriter.flush();
                    zipStream.closeEntry();
                }
            }
        }
    }

    /**
     *
     */
    protected class FileResourceHandler implements ResourceHandler {

        public void exportResource(Resource resource,
                                   TreeSet<String> exportedContentResources)
                throws RepositoryException, IOException {
            if (resource != null && isIncluded(resource)) {
                exportedResources.add(resource.getPath());
                exportParent(resource.getParent(), exportedContentResources);
                createDirAndContent(resource);
                ContentExportHints hints = new ContentExportHints();
                contentHandler.writeResource(resource,
                        ContentHandler.ChildPolicy.content, hints);
                zipStream.closeEntry();
                Binary binary = ResourceUtil.getBinaryData(resource);
                if (binary != null) {
                    InputStream binaryInput = binary.getStream();
                    if (binaryInput != null) {
                        try {
                            String path = getZipPath(resource) + "/" +
                                    MimeTypeUtil.getFilename(resource, DEFAULT_BINARY_EXT);
                            createZipEntry(path);
                            IOUtils.copy(binaryInput, zipStream);
                            zipStream.closeEntry();
                        } finally {
                            binaryInput.close();
                        }
                    }
                }
                flushContentBreakingResources(hints);
            }
        }
    }

    /**
     * @param hints
     * @throws RepositoryException
     * @throws IOException
     */
    protected void flushContentBreakingResources(ContentExportHints hints)
            throws RepositoryException, IOException {
        for (Map.Entry<String, Resource> item : hints.contentBreakingResources.entrySet()) {
            // the delayed export of content breaking resources must
            // NOT export all resources already written into the content file
            PackageExportService.this.exportResource(item.getValue(), hints.exportedContentResources);
        }
    }

    //
    // handling for content files with embedded content structures
    //

    /**
     * the structure to collect hints during content file export; contains:
     * - contentBreakingResources: all resource paths and resource objects which must be exported separately (later)
     * - exportedContentResources: all resource paths which has been exported in the content file
     */
    protected static class ContentExportHints {

        public TreeMap<String, Resource> contentBreakingResources = new TreeMap<>();
        public TreeSet<String> exportedContentResources = new TreeSet<>();
    }

    /**
     *
     */
    protected interface ContentHandler {

        enum ChildPolicy {skip, content, deep}

        /**
         * @param resource
         * @param childPolicy
         * @throws RepositoryException
         * @throws IOException
         */
        void writeResource(Resource resource, ChildPolicy childPolicy,
                           ContentExportHints hints)
                throws RepositoryException, IOException;
    }

    protected static JsonWriter createJsonWriter(Writer writer) {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(true);
        jsonWriter.setIndent("    ");
        return jsonWriter;
    }

    protected class JsonContentHandler implements ContentHandler {

        /**
         * @see PackageExportService.ContentHandler
         */
        public void writeResource(Resource resource, ChildPolicy childPolicy,
                                  ContentExportHints hints)
                throws RepositoryException, IOException {

            if (resource != null) {
                OutputStreamWriter streamWriter = new OutputStreamWriter(zipStream, MappingRules.CHARSET);
                JsonWriter jsonWriter = createJsonWriter(streamWriter);
                writeResource(jsonWriter, resource, childPolicy, hints);
                jsonWriter.flush();
            }
        }

        public void writeResource(JsonWriter jsonWriter, Resource resource, ChildPolicy childPolicy,
                                  ContentExportHints hints)
                throws RepositoryException, IOException {

            if (resource != null) {
                MappingRules mappingRules = servicePackage.getMappingRules(resource.getPath());
                jsonWriter.beginObject();
                boolean exportChildOrderHint = (childPolicy != ChildPolicy.deep);
                // write the resources properties...
                JsonUtil.exportProperties(jsonWriter, resource, mappingRules);
                // write the 'jcr:content' child resource as the first child if present
                if (childPolicy == ChildPolicy.content || childPolicy == ChildPolicy.deep) {
                    Resource contentResource = resource.getChild(ResourceUtil.CONTENT_NODE);
                    if (contentResource != null) {
                        hints.exportedContentResources.add(contentResource.getPath());
                        jsonWriter.name(contentResource.getName());
                        writeResource(jsonWriter, contentResource, ChildPolicy.deep, hints);
                    }
                }
                // write all the other children recursive if 'deep' policy is set
                if (childPolicy == ChildPolicy.deep) {
                    for (Resource child : resource.getChildren()) {
                        String childName = child.getName();
                        if (!ResourceUtil.CONTENT_NODE.equals(childName)) {
                            String childPath = child.getPath();
                            if (hints != null && contentBreakFilter.accept(child)) {
                                hints.contentBreakingResources.put(childPath, child);
                                exportChildOrderHint = true;
                            } else {
                                if (hints != null) {
                                    hints.exportedContentResources.add(childPath);
                                }
                                jsonWriter.name(childName);
                                writeResource(jsonWriter, child, childPolicy, hints);
                            }
                        }
                    }
                }
                // write the names of the children into an additional array as a hint for
                // the right order of the children (that's important in the case that the ZIP package
                // is unzipped and zipped back from the file system - the order is lost in this case);
                // this list is not filtered(!) because this is also important for all path resources
                if (exportChildOrder && exportChildOrderHint) {
                    JsonUtil.exportChildOrderProperty(jsonWriter, resource);
                }
                jsonWriter.endObject();
            }
        }
    }

    //
    // ZIP helpers
    //

    protected String getZipPath(Resource resource) {
        String path = resource.getPath();
        if ("/".equals(path)) {
            path = "";
        }
        path = JCR_ROOT_ZIP_NAME + path;
        path = path.replaceAll(JCR_NAMESPACE_PATTERN, "/_$1_");
        return path;
    }

    protected ZipEntry createZipEntry(String name)
            throws IOException {
        LOG.debug("toZIP: " + name);
        ZipEntry entry = new ZipEntry(name);
        zipStream.putNextEntry(entry);
        return entry;
    }

    protected void createDirAndContent(Resource resource)
            throws IOException {
        String path = getZipPath(resource);
        createZipEntry(path + "/");
        createZipEntry(path + "/" +
                MappingRules.CONTENT_NODE_FILE_NAME + "." + contentNodeType.name());
    }
}
