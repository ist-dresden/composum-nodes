package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import com.composum.sling.nodes.mount.ExtendedResolver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_RESOURCE;

/**
 * <p>
 * Model that can produce XML source, ZIP files or Vault-packages for it's resource.
 * This is quite similar to what
 * <a href="https://jackrabbit.apache.org/filevault/vaultfs.html">Jackrabbit Vault</a>
 * produces, but we make a few differences, since our main intention here is to produce good source code for our site.
 * These are:</p>
 * <ul>
 *     <li>
 *         This is based on Sling {@link Resource}s and does not use JCR itself like Vault does. Thus, it could also
 *         be used for non-JCR based resources. (One exception is that we need JCR to determine whether a nodes
 *         children are orderable and to find out primaryType relationships, but we try to degrade gracefully if that
 *         fails.)
 *     </li>
 *     <li>
 *         We do not export properties that change on each im- and export, since importing and exporting a site
 *     should not change it's source - compare {@link #EXCLUDED_PROPS}. For instance, jcr:created* and
 *     jcr:lastModified* are omitted, as are the properties mix:versionable creates. Also, jcr:uuid is not exported,
 *     as it will change on each import and since we regard references to jcr:uuid as
 *      dangerous, to be used only sparingly if at all. (Composum uses absolute paths as references.)
 *     </li>
 *     <li>
 *         In the .content.xml for a node with a jcr:content node, all subnodes of this node will be included, even
 *         if they happen to be a nt:folder. The only exception here are binary properties / files, which are written
 *         into separate files, since we don't want binary content in XML files.
 *     </li>
 *     <li>If nt:file have additional properties, we do always create an "extended file aggregate" with an
 *     additional directory {filename}.dir to avoid having parts of the file hidden in
 *     the .content.xml, invisible in a file browser</li>
 * </ul>
 * <p> To summarize, we follow the following rules for our output. </p>
 * <ul>
 *   <li>Resources with a primary type of nt:folder (or subtypes) or with a subnode named jcr:content are
 *   exported as a folder with a .content.xml. Other nodes (barring files and binary properties) are exported in the
 *   next higher .content.xml.</li>
 *   <li>Resources with type nt:file are exported as a file with a name according to their name; if they contain
 *   additional attributes an additional file {filename}_dir/.content.xml is created for them, which also contains
 *   other subnodes of the file node (barring files and binary attributes).</li>
 *   <li>Resources with type nt:resource that are not below a nt:file are exported into a file named like the
 *   resource with an extension added according to the mime type.</li>
 *   <li>Binary resources (except jcr:data below nt:file + nt:resource) are exported as a file {resource}/{
 *   propertyname}.binary, with an entry {@code {propertyname}="{BINARY}"} in the .content.xml to declare the
 *   attribute with it's type.</li>
 *   <li>If a node has orderable subnodes, it includes entries for all it's children - possibly empty nodes if they
 *   have their own folder / file according to the above rules.</li>
 *   <li>Only "source compatible" properties are exported - we ignore protected properties and properties that
 *   change on import-export cycles. (See {@link #EXCLUDED_PROPS}). </li>
 * </ul>
 * <p> Limitations: </p>
 * <ul>
 *     <li>Since we do not export jcr:uuid, references don't work.</li>
 *     <li>This will, obviously, not be quite identical to an export with Jackrabbit Vault, though
 *     it is compatible to be imported into JCR via Vault.</li>
 * </ul>
 *
 * @see "https://jackrabbit.apache.org/filevault/vaultfs.html"
 * @see org/apache/jackrabbit/vault/fs/config/defaultConfig-1.1.xml
 */
@Restricted(key = SourceServlet.SERVICE_KEY)
public class SourceModel extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SourceModel.class);

    /**
     * Matches a number of properties that do not belong into a source.
     * <p>TODO move to configuration
     */
    public static final StringFilter EXCLUDED_PROPS = new StringFilter.WhiteList(
            "^jcr:baseVersion$", "^jcr:predecessors$", "^jcr:versionHistory$", "^jcr:isCheckedOut$"
            , "^jcr:created", "^jcr:lastModified", "^jcr:uuid$", "^jcr:data$", "^cq:lastModified"
            , "^cq:lastReplicat" // used for staging
            // , "^cq:lastRolledout"
    );

    /**
     * Matches mixins that do not belong into a source. E.g. rep:AccessControllable doesn't make sense
     * since we do not export ACLs, anyway, and adding ACLs automatically adds this mixin.
     */
    public static final StringFilter EXCLUDED_MIXINS = new StringFilter.WhiteList("^rep:AccessControllable$");

    /**
     * Pattern for {@link SimpleDateFormat} that creates a date suitable with XML sources.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Indentation level for one level in the XML hierarchy in an XML document.
     */
    public static final String BASIC_INDENT = "    ";

    protected static final Pattern PATH_WITHIN_JCR_CONTENT = Pattern.compile(".*/jcr:content/.*$");

    protected final NodesConfiguration config;

    protected transient List<Property> propertyList;
    protected transient List<Resource> subnodeList;
    /**
     * Whether the siblings are known to be orderable - wrapped into array to distinguish "not known" from "not
     * yet determined".
     */
    protected transient Boolean[] hasOrderableSiblings;
    /**
     * Whether the child nodes are known to be orderable - wrapped into array to distinguish "not known" from "not
     * yet determined".
     */
    protected transient Boolean[] hasOrderableChildren;
    protected transient Comparator<Property> propertyComparator;

    /** Some things look like namespaces but aren't actual namespaces. This saves them. */
    protected transient List<String> nonExistingNamespaces = new ArrayList<>();

    public SourceModel(NodesConfiguration config, BeanContext context, Resource resource) {
        this.config = config;
        initialize(context, resource);
    }

    // ExtendedResolver export adjustment

    /**
     * @return the root path to use for exporting artifacts, honors a root path (mount poiunt) of an extended resolver
     */
    @NotNull
    public String getExportRootPath() {
        String exportRootPath = "/";
        ResourceResolver resolver = getResolver();
        if (resolver instanceof ExtendedResolver) {
            String resolverRootPath = ((ExtendedResolver) resolver).getResolverRootPath();
            if (resolverRootPath != null) {
                exportRootPath = resolverRootPath;
            }
        }
        return exportRootPath;
    }

    /**
     * @param path the path to map for exporting
     * @return the path relative to the export root path
     */
    @NotNull
    public String getExportPath(@NotNull final String path) {
        String exportPath = path;
        String exportRootPath = getExportRootPath();
        if (!"/".equals(exportRootPath)) {
            if (path.equals(exportRootPath)) {
                exportPath = "/";
            } else if (path.startsWith(exportRootPath + "/")) {
                exportPath = path.substring(exportRootPath.length());
            }
        }
        return exportPath;
    }

    /**
     * @param aPath a resource path in the resource repository tree; maybe a mounted resource path
     * @return 'true' if the path is equal to the export root path
     */
    public boolean isRootPath(@NotNull final String aPath) {
        return getExportRootPath().equals(aPath);
    }

    //

    @Override
    public String getName() {
        return resource.getName();
    }

    public String getPrimaryType() {
        return StringUtils.defaultString(ResourceUtil.getPrimaryType(resource));
    }

    public FileTime getLastModified(Resource rawResource) {
        ResourceHandle someResource = ResourceHandle.use(rawResource);
        Calendar timestamp = someResource.getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        if (timestamp == null) {
            timestamp = someResource.getProperties().get(JcrConstants.JCR_CREATED, Calendar.class);
        }
        if (timestamp == null) {
            timestamp = someResource.getContentResource().getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
        if (timestamp == null) {
            timestamp = someResource.getContentResource().getProperties().get(JcrConstants.JCR_CREATED, Calendar.class);
        }
        if (timestamp == null) {
            timestamp = someResource.getInherited(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
        if (timestamp == null) {
            timestamp = someResource.getInherited(JcrConstants.JCR_CREATED, Calendar.class);
        }
        return timestamp != null ?
                FileTime.from(timestamp.getTimeInMillis(), TimeUnit.MILLISECONDS) : null;
    }

    public List<Property> getPropertyList() {
        if (propertyList == null) {
            propertyList = new ArrayList<>();
            Node jcrNode = resource.adaptTo(Node.class);
            for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {
                Integer type = null;
                if (jcrNode != null) {
                    try {
                        javax.jcr.Property jcrProp = jcrNode.getProperty(entry.getKey());
                        type = jcrProp.getType();
                        if (JCR_PRIMARYTYPE.equals(entry.getKey()) || JCR_MIXINTYPES.equals(entry.getKey()) ||
                                jcrProp.getDefinition().getRequiredType() != PropertyType.UNDEFINED) {
                            // the property has a required type - no point in forcing it to be displayed. for instance
                            // you don't need to give {NAME} for jcr:mixinTypes which is forced to be name.
                            type = null;
                        }
                    } catch (RepositoryException e) {
                        // shouldn't happen, but happens for staging resources in the platform since we
                        // don't implement getDefinition there -- would be hard to change.
                        LOG.debug("Error reading property {}/{} : {}",
                                resource.getPath(), entry.getValue(), e.toString());
                    }
                } else {
                    Object value = entry.getValue();
                    if (value != null) {
                        if (value instanceof String || value instanceof String[]) {
                            type = PropertyType.STRING;
                        } else if (value instanceof Long || value instanceof Long[]) {
                            type = PropertyType.LONG;
                        } else if (value instanceof Boolean || value instanceof Boolean[]) {
                            type = PropertyType.BOOLEAN;
                        } else if (value instanceof Double || value instanceof Double[]) {
                            type = PropertyType.DOUBLE;
                        } else if (value instanceof Calendar || value instanceof Calendar[]) {
                            type = PropertyType.DATE;
                        } else if (value instanceof InputStream) {
                            type = PropertyType.BINARY;
                        }
                    }
                }
                Property property = new Property(entry.getKey(), entry.getValue(), type);
                if (!isExcluded(property)) {
                    propertyList.add(property);
                }
            }
            Collections.sort(propertyList, getPropertyComparator());
        }
        return propertyList;
    }

    protected boolean isExcluded(Property property) {
        return EXCLUDED_PROPS.accept(property.getName());
    }

    public boolean hasSubnodes() {
        return !getSubnodeList().isEmpty();
    }

    public List<Resource> getSubnodeList() {
        if (subnodeList == null) {
            subnodeList = new ArrayList<>();
            Resource jcrcontent = null;
            Iterator<Resource> iterator = resource.listChildren();
            while (iterator.hasNext()) {
                Resource subnode = iterator.next();
                if (config.getSourceNodesFilter().accept(subnode) && !ResourceUtil.isSyntheticResource(subnode)) {
                    if (subnode.getName().equals(JCR_CONTENT)) {
                        jcrcontent = subnode;
                    } else {
                        subnodeList.add(subnode);
                    }
                }
            }
            if (jcrcontent != null) {
                subnodeList.add(0, jcrcontent);
            }
        }
        return subnodeList;
    }

    protected void determineNamespaces(List<String> keys, boolean inFullCoverage) {
        addNameNamespace(keys, getPrimaryType());
        List<Property> properties = getPropertyList();
        for (Property property : properties) {
            String ns = property.getNs();
            addNamespace(keys, ns);
        }
        addNameNamespace(keys, resource.getName());
        boolean subnodeInFullCoverage = inFullCoverage || isFullCoverageNode();
        for (Resource subnode : getSubnodeList()) {
            addNameNamespace(keys, subnode.getName());
            SourceModel subnodeModel = new SourceModel(config, context, subnode);
            if (subnodeModel.getRenderingType(subnodeModel.getResource(), subnodeInFullCoverage) == RenderingType.EMBEDDED) {
                subnodeModel.determineNamespaces(keys, subnodeInFullCoverage);
            }
        }
    }

    protected void addNameNamespace(List<String> keys, String aName) {
        String ns = getNamespace(aName);
        addNamespace(keys, ns);
    }

    protected void addNamespace(List<String> keys, String ns) {
        if (StringUtils.isNotBlank(ns) && !keys.contains(ns)) {
            keys.add(ns);
        }
    }

    protected String getNamespace(String aName) {
        int delim = aName.indexOf(':');
        return delim < 0 ? "" : aName.substring(0, delim);
    }

    // Package output

    /**
     * Writes a complete package about the node - arguments specify the package metadata.
     *
     * @throws IOException             on IO errors
     * @throws IOErrorOnCloseException is thrown when an IO error appears during {@link ZipOutputStream#close()},
     *                                 which writes the central directory of the zip which is not read by some consumers. We give them
     *                                 the possibility to distinguish these.
     */
    public void writePackage(OutputStream output, String group, String packageName, String version)
            throws IOException, IOErrorOnCloseException, RepositoryException {

        String root = "jcr_root";
        ZipOutputStream zipStream = new ZipOutputStream(output);
        writePackageProperties(zipStream, group, packageName, version);
        writeFilterXml(zipStream);
        if (ResourceUtil.CONTENT_NODE.equals(getName())) {
            Resource parent = resource.getParent();
            if (parent != null) {
                SourceModel parentModel = new SourceModel(config, context, parent);
                writeParents(zipStream, root, parentModel.getResource().getParent());
                parentModel.writeIntoZip(zipStream, root, DepthMode.DEEP);
            }
        } else {
            writeParents(zipStream, root, resource.getParent());
            writeIntoZip(zipStream, root, DepthMode.DEEP);
        }
        zipStream.flush();
        try {
            zipStream.close();
        } catch (IOException e) {
            throw new IOErrorOnCloseException(e);
        }
    }

    /**
     * Returns true if the nodes siblings are ordered.
     * Works only for JCR resources - if we cannot determine this, we return 'false'.
     */
    public boolean hasOrderableSiblings() {
        Boolean result = null;
        try {
            ResourceHandle parent = getResource().getParent();
            if (hasOrderableSiblings == null) {
                result = hasOrderableChildren(parent);
                hasOrderableSiblings = new Boolean[]{result};
            } else {
                result = hasOrderableSiblings[0];
            }
        } catch (RuntimeException ex) {
            LOG.warn(ex.toString()); // probably no JCR resource
        }
        return result != null ? result : false;
    }

    /**
     * Returns true if the nodes children are ordered. Works only for JCR resources - if we cannot determine this,
     * we return null.
     */
    public boolean hasOrderableChildren() {
        if (hasOrderableChildren == null) {
            Boolean determined = hasOrderableChildren(resource);
            hasOrderableChildren = new Boolean[]{determined != null ? determined : Boolean.FALSE};
        }
        return hasOrderableChildren[0];
    }

    /**
     * Returns true if the nodes children are ordered. Works only for JCR resources - if we cannot determine this,
     * we return null.
     */
    protected Boolean hasOrderableChildren(ResourceHandle aResource) {
        try {
            Node node = requireNonNull(aResource).adaptTo(Node.class);
            if (node != null) {
                return node.getPrimaryNodeType().hasOrderableChildNodes();
            } else {
                String primaryType = resource.getPrimaryType();
                if (primaryType != null) {
                    if (primaryType.equals(ResourceUtil.TYPE_SLING_ORDERED_FOLDER)) {
                        return true;
                    }
                }
            }
        } catch (RepositoryException | RuntimeException e) {
            LOG.warn("Can't determine orderability of {}", getPath(), e);
        }
        return null;
    }

    protected void writePackageProperties(ZipOutputStream zipStream, String group, String aName, String version)
            throws IOException {

        ZipEntry entry;
        entry = new ZipEntry("META-INF/vault/properties.xml");
        zipStream.putNextEntry(entry);

        @SuppressWarnings("resource")
        Writer writer = new OutputStreamWriter(zipStream, UTF_8);
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n")
                .append("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n")
                .append("<properties>\n")
                .append("<comment>FileVault Package Properties</comment>\n")
                .append("<entry key=\"packageType\">content</entry> ")
                .append("<entry key=\"name\">")
                .append(Property.escapeXmlAttribute(aName))
                .append("</entry>\n")
                .append("<entry key=\"buildCount\">1</entry>\n")
                .append("<entry key=\"version\">")
                .append(Property.escapeXmlAttribute(version))
                .append("</entry>\n")
                .append("<entry key=\"packageFormatVersion\">2</entry>\n")
                .append("<entry key=\"group\">")
                .append(Property.escapeXmlAttribute(group))
                .append("</entry>\n")
                .append("<entry key=\"description\">created from source download</entry>\n")
                .append("<entry key=\"createdBy\">");
        String userId = getResolver().getUserID();
        if (userId != null) {
            writer.append(Property.escapeXmlAttribute(userId));
        }
        writer.append("</entry>\n")
                .append("<entry key=\"created\">")
                .append(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .append("</entry>\n")
                .append("</properties>");
        writer.flush(); // don't close since that closes the zipStream
        zipStream.closeEntry();
    }

    protected void writeFilterXml(ZipOutputStream zipStream) throws IOException {

        String path = getExportPath(resource.getPath());

        ZipEntry entry;
        entry = new ZipEntry("META-INF/vault/filter.xml");
        zipStream.putNextEntry(entry);

        @SuppressWarnings("resource")
        Writer writer = new OutputStreamWriter(zipStream, UTF_8);
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<workspaceFilter version=\"1.0\">\n")
                .append("    <filter root=\"")
                .append(Property.escapeXmlAttribute(path))
                .append("\"/>\n")
                .append("</workspaceFilter>\n");

        writer.flush(); // don't close since that closes the zipStream
        zipStream.closeEntry();
    }

    /**
     * Writes all the .content.xml of the parents of root into the zip.
     */
    protected void writeParents(@NotNull ZipOutputStream zipStream, @NotNull String root, @Nullable Resource parent)
            throws IOException, RepositoryException {
        if (parent != null && !isRootPath(parent.getPath())) {
            writeParents(zipStream, root, parent.getParent());
            SourceModel parentModel = new SourceModel(config, context, parent);
            parentModel.writeIntoZip(zipStream, root, DepthMode.PROPERTIESONLY);
        }
    }

    // ZIP output

    /**
     * Writes a "naked" Zip about the node: no package metadata, no parent nodes.
     */
    public void writeArchive(@NotNull OutputStream output)
            throws IOException, RepositoryException {

        ZipOutputStream zipStream = new ZipOutputStream(output);
        writeIntoZip(zipStream, resource.getPath(), DepthMode.DEEP);
        zipStream.flush();
        zipStream.close();
    }

    /**
     * Writes a "naked" Zip about the node: no package metadata, no parent nodes. This might include entries about
     * subnodes if {writeDeep}=true, and might include entries about binary properties.
     *
     * @param zipStream the stream to write to, not closed.
     * @param depthMode determines to what extent we write subnodes
     */
    protected void writeIntoZip(@NotNull ZipOutputStream zipStream, @NotNull String root, @NotNull DepthMode depthMode)
            throws IOException, RepositoryException {
        if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
            return;
        }
        if (ResourceUtil.isResourceType(resource, ResourceUtil.TYPE_RESOURCE) && ResourceUtil.isResourceType(resource.getParent(), NT_FILE)) {
            // there is no proper way to write a nt:resource of a nt:file into a package.
            // We write the parent - nt:file - instead.
            new SourceModel(config, context, resource.getParent()).writeIntoZip(zipStream, root, depthMode);
            return;
        }
        RenderingType renderingType = getRenderingType(resource, false);
        if (renderingType == RenderingType.BINARYFILE) {
            if (DepthMode.DEEP == depthMode) {
                writeFile(zipStream, root, resource);
            }
            // not writeDeep: a .content.xml is not present for a file, so we can't do anything.
            return;
        }

        String zipName = getZipName(root);
        ZipEntry entry = new ZipEntry(zipName);
        FileTime lastModified = getLastModified(resource);
        LOG.debug("Writing entry {} ({})", entry.getName(), root);
        if (lastModified != null) {
            entry.setLastModifiedTime(lastModified);
        }
        zipStream.putNextEntry(entry);

        Queue<String> binaryProperties = new ArrayDeque<>(); // these need to have an additional file
        Queue<SourceModel> additionalFiles = new ArrayDeque<>();
        Writer writer = new OutputStreamWriter(zipStream, UTF_8);
        writeXmlFile(writer, depthMode, binaryProperties, additionalFiles);
        writer.flush(); // deliberately not close since that'd close the zip 8-/
        zipStream.closeEntry();

        writeBinaryProperties(zipStream, root, binaryProperties);
        for (SourceModel binaryFile : additionalFiles) {
            binaryFile.writeIntoZip(zipStream, root, depthMode);
        }
    }

    /**
     * Writes the current node as a file node (not the jcr:content but the parent) incl. it's binary data and possibly
     * additional data about nonstandard properties.
     */
    protected void writeFile(@NotNull ZipOutputStream zipStream, @NotNull String root, @NotNull ResourceHandle file)
            throws IOException, RepositoryException {
        @NotNull ResourceHandle origFile = file;
        if (file.getName().equals(JCR_CONTENT)) {
            // file format doesn't allow this - we need to write the file with the parent's name
            file = file.getParent();
        }

        FileTime lastModified = getLastModified(file);
        ZipEntry entry;
        String path = requireNonNull(file).getPath();
        InputStream fileContent;
        Binary binaryData = ResourceUtil.getBinaryData(file);
        if (binaryData != null) {
            fileContent = binaryData.getStream();
        } else {
            if ((fileContent = file.getProperty(ResourceUtil.PROP_DATA, InputStream.class)) == null) {
                Resource content = file.getChild(JCR_CONTENT);
                if (content != null) {
                    fileContent = content.getValueMap().get(ResourceUtil.PROP_DATA, InputStream.class);
                }
            }
        }
        if (fileContent != null) {
            entry = new ZipEntry(getZipName(root, path));
            LOG.debug("Writing entry {}", entry.getName());
            if (lastModified != null) {
                entry.setLastModifiedTime(lastModified);
            }
            InputStream writeContent = fileContent;
            putEntry(zipStream, entry, () -> IOUtils.copy(writeContent, zipStream), () -> writeContent.close());
        } else {
            LOG.warn("Can't get binary data for {}", path);
        }

        // if it's more than a nt:file/nt:resource construct that contains additional attributes we have to write
        // an additional {file}.dir/.content.xml .
        boolean fileIsNonstandard = file.getProperty(JCR_MIXINTYPES, new String[0]).length > 0
                || !NT_FILE.equals(file.getProperty(JCR_PRIMARYTYPE, String.class));
        boolean contentNodeIsNonstandard = file.getContentResource().getProperty(JCR_MIXINTYPES, new String[0]).length > 0
                || !NT_RESOURCE.equals(file.getContentResource().getProperty(JCR_PRIMARYTYPE, String.class));
        if (fileIsNonstandard || contentNodeIsNonstandard) {
            Queue<String> binaryProperties = new ArrayDeque<>(); // these need to have an additional file
            Queue<SourceModel> binaryFiles = new ArrayDeque<>();
            entry = new ZipEntry(getZipName(root, file.getPath() + ".dir/.content.xml"));
            LOG.debug("Writing entry {}", entry.getName());
            if (lastModified != null) {
                entry.setLastModifiedTime(lastModified);
            }
            SourceModel fileModel = new SourceModel(config, context, file);
            putEntry(zipStream, entry, () -> {
                Writer writer = new OutputStreamWriter(zipStream, UTF_8);
                fileModel.writeXmlFile(writer, DepthMode.DEEP, binaryProperties, binaryFiles);
                writer.flush();
            }, null);
            writeBinaryProperties(zipStream, root, binaryProperties);
            for (SourceModel binaryFile : binaryFiles) {
                binaryFile.writeIntoZip(zipStream, root, DepthMode.DEEP);
            }
        }
    }

    @FunctionalInterface
    private interface IOExceptionRunnable {
        void run() throws IOException, RepositoryException;
    }

    private void putEntry(@NotNull ZipOutputStream zipStream, @NotNull ZipEntry entry, @NotNull IOExceptionRunnable writeAction, @Nullable IOExceptionRunnable finalAction) throws IOException, RepositoryException {
        try {
            zipStream.putNextEntry(entry);
            writeAction.run();
        } catch (ZipException e) {
            if (!e.getMessage().contains("duplicate entry")) {
                throw e;
            }
            LOG.warn("Duplicated entry for {} - skipping duplicate", entry.getName(), e);
        } finally {
            zipStream.closeEntry();
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Writes the binary properties collected in {binaryProperties} into entries in the zip file.
     */
    protected void writeBinaryProperties(@NotNull ZipOutputStream zipStream, @NotNull String root, @Nullable Queue<String> binaryProperties) throws IOException {
        if (binaryProperties == null || binaryProperties.isEmpty()) {
            return;
        }
        for (String binPropPath : binaryProperties) {
            Resource propertyResource = resolver.getResource(binPropPath);
            try (InputStream inputStream = propertyResource != null ? propertyResource.adaptTo(InputStream.class) : null) {
                if (inputStream != null) {
                    FileTime lastModified = getLastModified(ResourceHandle.use(propertyResource));
                    ZipEntry entry;
                    entry = new ZipEntry(getZipName(root, binPropPath) + ".binary");
                    LOG.debug("Writing entry {}", entry.getName());
                    if (lastModified != null) {
                        entry.setLastModifiedTime(lastModified);
                    }
                    zipStream.putNextEntry(entry);
                    IOUtils.copy(inputStream, zipStream);
                    zipStream.closeEntry();
                } else {
                    LOG.warn("Can't get binary data for binary property {}", binPropPath);
                }
            }
        }
    }

    /**
     * Turns a resource path into a proper name for a zip file with the appropriate encoding of troublesome chars.
     */
    protected String getZipName(@NotNull String root, @NotNull String resourcePath) {
        String name = getExportPath(resourcePath);
        String exportRoot = getExportPath(root);
        if (name.startsWith(exportRoot)) {
            name = name.substring(exportRoot.length() + 1);
        } else {
            name = exportRoot + name;
        }
        return filesystemName(name);
    }

    /**
     * Returns the name for the zip entry for this resource.
     */
    protected String getZipName(@NotNull String root) {
        RenderingType renderingType = getRenderingType(resource, false);
        String zipName;
        switch (renderingType) {
            case FOLDER:
                zipName = getZipName(root, getPath() + "/.content.xml");
                break;
            case BINARYFILE:
                zipName = getZipName(root, getPath());
                break;
            case EMBEDDED: // this shouldn't happen - no sensible way to handle it, but can happen on unfortunate calls.
            case XMLFILE:
                if (ResourceUtil.CONTENT_NODE.equals(getName())) {
                    zipName = getZipName(root, resource.getParentPath() + "/.content.xml");
                } else {
                    zipName = getZipName(root, getPath() + ".xml");
                }
                break;
            default:
                throw new IllegalArgumentException("Impossible rendering type " + renderingType);
        }
        return zipName;
    }

    /**
     * Transforms the name into something usable for the filesystem.
     */
    protected String filesystemName(String aName) {
        return PlatformNameFormat.getPlatformPath(aName);
    }

    // XML output

    /**
     * Writes an XML file for the node, normally .content.xml, including an jcr:content node if present.
     *
     * @param writeDeep also write subnodes; if false only properties of the node itself are written but no
     *                  children (and no jcr:content).
     */
    public void writeXmlFile(@NotNull Writer writer, boolean writeDeep) throws IOException, RepositoryException {
        DepthMode depthMode = writeDeep ? DepthMode.DEEP : DepthMode.PROPERTIESONLY;
        writeXmlFile(writer, depthMode, null, null);
    }

    /**
     * Writes an XML file for the node, normally .content.xml, including an jcr:content node if present.
     *
     * @param depthMode        determines to what extent we write subnodes
     * @param binaryProperties if given, collects the full paths to binary properties (except jcr:data which is
     *                         written as a binary file
     * @param additionalFiles  if given, collects the {@link SourceModel}s that have to be written into another file
     */
    protected void writeXmlFile(@NotNull Writer writer, @NotNull DepthMode depthMode,
                                @Nullable Queue<String> binaryProperties, @Nullable Queue<SourceModel> additionalFiles)
            throws IOException, RepositoryException {
        List<String> namespaces = new ArrayList<>();
        namespaces.add("jcr");
        determineNamespaces(namespaces, isFullCoverageNode());
        Collections.sort(namespaces);
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<jcr:root");
        writeNamespaceAttributes(writer, namespaces);
        writeProperties(writer, "        ", binaryProperties);
        writer.append(">\n");
        switch (depthMode) {
            case DEEP:
                writeSubnodesAsXml(writer, BASIC_INDENT, isFullCoverageNode(), binaryProperties, additionalFiles);
                break;
            case CONTENTNODE:
                Resource contentNode = resource.getChild(ResourceUtil.CONTENT_NODE);
                if (contentNode != null) {
                    SourceModel contentNodeModel = new SourceModel(config, context, contentNode);
                    contentNodeModel.writeXmlSubnode(writer, BASIC_INDENT, isFullCoverageNode(),
                            binaryProperties, additionalFiles);
                }
                writeSubnodeOrder(writer, BASIC_INDENT, true);
                break;
            default:
                writeSubnodeOrder(writer, BASIC_INDENT, false);
        }
        writer.append("</jcr:root>\n");
    }

    protected void writeSubnodesAsXml(@NotNull Writer writer, @NotNull String indent, boolean inFullCoverageNode,
                                      @Nullable Queue<String> binaryProperties, @Nullable Queue<SourceModel> additionalFiles)
            throws IOException {
        for (Resource subnode : getSubnodeList()) {
            SourceModel subnodeModel = new SourceModel(config, context, subnode);
            subnodeModel.writeXmlSubnode(writer, indent, inFullCoverageNode,
                    binaryProperties, additionalFiles);
        }
    }

    /**
     * Writes the node including subnodes as XML, using the base indentation.
     */
    protected void writeXmlSubnode(@NotNull Writer writer, @NotNull String indent, boolean inFullCoverageNode,
                                   @Nullable Queue<String> binaryProperties, @Nullable Queue<SourceModel> additionalFiles)
            throws IOException {
        String name = escapeXmlName(getName());
        RenderingType renderingType = getRenderingType(resource, inFullCoverageNode);
        boolean fullCoverage = inFullCoverageNode || isFullCoverageNode();
        switch (renderingType) {
            case EMBEDDED:
                writer.append(indent).append("<").append(name);
                writeProperties(writer, indent + "        ", binaryProperties);
                if (hasSubnodes()) {
                    writer.append(">\n");
                    writeSubnodesAsXml(writer, indent + BASIC_INDENT, fullCoverage, binaryProperties, additionalFiles);
                    writer.append(indent).append("</").append(name).append(">\n");
                } else {
                    writer.append("/>\n");
                }
                break;
            case FOLDER:
            case XMLFILE:
            case BINARYFILE:
                if (additionalFiles != null) {
                    additionalFiles.add(this);
                }
                // If the node has orderable siblings, we write a stub node to specify the node order.
                // The real content is in a separate file.
                if (hasOrderableSiblings()) {
                    writer.append(indent).append("<").append(name).append("/>\n");
                }
                break;
            default:
                throw new IllegalArgumentException("Impossible rendering type " + renderingType);
        }
    }

    /**
     * If the node has orderable children, we write a stub node to specify the node order.
     */
    protected void writeSubnodeOrder(Writer writer, String indent, boolean skipContentNode) throws IOException {
        if (hasOrderableChildren() && getSubnodeList().size() > 1) {
            for (Resource subnode : getSubnodeList()) {
                if (!skipContentNode || !ResourceUtil.CONTENT_NODE.equals(subnode.getName())) {
                    writer.append(indent).append("<").append(escapeXmlName(subnode.getName())).append("/>\n");
                }
            }
        }
    }

    protected void writeProperties(@NotNull Writer writer, @NotNull String indent,
                                   @Nullable Queue<String> binaryProperties) throws IOException {
        for (Property property : getPropertyList()) {
            if (binaryProperties != null && property.isBinary()) {
                binaryProperties.add(getPath() + "/" + property.getName());
            }
            writeProperty(writer, indent, property.getName(), property.getEscapedString(indent));
        }
    }

    protected void writeProperty(Writer writer, String indent, String propertyName, String escapedValue) throws IOException {
        if (StringUtils.isNotEmpty(escapedValue)) {
            writer.append("\n");
            writer.append(indent);
            writer.append(escapeXmlName(propertyName));
            writer.append("=\"");
            writer.append(escapedValue);
            writer.append("\"");
        }
    }

    protected void writeNamespaceAttributes(Writer writer, List<String> namespaces) throws RepositoryException, IOException {
        Session session = getSession();
        if (session != null) {
            for (int i = 0; i < namespaces.size(); ++i) {
                String ns = namespaces.get(i);
                try {
                    String url = session.getNamespaceURI(ns);
                    if (StringUtils.isNotBlank(url)) {
                        writer.append(" xmlns:").append(ns).append("=\"").append(url).append("\"");
                        if (i + 1 < namespaces.size()) {
                            writer.append("\n       ");
                        }
                    }
                } catch (NamespaceException nsex) {
                    LOG.debug(nsex.toString());
                    nonExistingNamespaces.add(ns);
                }
            }
        }
    }

    public String escapeXmlName(String propertyName) {
        String encoded = ISO9075.encode(propertyName);
        // If the attribute has a colon which does not start a namespace, we still need to encode that.
        if (encoded.contains(":")) {
            int pos = encoded.indexOf(':');
            String prefix = encoded.substring(0, pos);
            if (nonExistingNamespaces.contains(prefix)) {
                encoded = encoded.replaceAll(":", "_x003A_");
            }
        }
        return encoded;
    }

    /**
     * This encodes what nodes are presented in which way nodes are represented in a Zip / Package.
     *
     * @param inFullCoverageNode if true we suppress folders - e.g. if we are within a jcr:content node or
     *                           an RenderingType.XMLFILE node.
     */
    protected RenderingType getRenderingType(Resource aResource, boolean inFullCoverageNode) {
        // The ordering of the rules is important, as it handles various special cases.
        if (ResourceUtil.isNodeType(aResource, NT_FILE) ||
                (ResourceUtil.isNodeType(aResource, NT_RESOURCE) && !ResourceUtil.isNodeType(aResource.getParent(), NT_FILE))
        ) { // a nt:resource node without a nt:file as parent is a special case that's handled as file, too.
            return RenderingType.BINARYFILE;
        }
        if (PATH_WITHIN_JCR_CONTENT.matcher(aResource.getPath()).matches()) {
            // we want everything below a jcr:content to stay in it's .content.xml except if it's binary
            return RenderingType.EMBEDDED;
        }
        if (aResource.getChild(JCR_CONTENT) != null) {
            // jcr:content shall always be the top node of a .content.xml
            return RenderingType.FOLDER;
        }
        if (config.getSourceXmlNodesFilter().accept(aResource)) {
            // in theory it would be nice to have a rule here that everything below this stays in this file,
            // even if it's a folder, but that'd be inefficient or a hack - let's see later whether it'd be worth it.
            return RenderingType.XMLFILE;
        }
        if (!inFullCoverageNode && config.getSourceFolderNodesFilter().accept(aResource)) {
            return RenderingType.FOLDER;
        }
        return RenderingType.EMBEDDED;
    }

    protected boolean isFullCoverageNode() {
        return resource.getName().equalsIgnoreCase(JCR_CONTENT) ||
                RenderingType.XMLFILE == getRenderingType(resource, false);
    }

    protected Comparator<Property> getPropertyComparator() {
        if (propertyComparator == null) {
            propertyComparator =
                    Comparator.comparing(Property::getNs, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Property::getName);
            if (this.config.isSourceAdvancedSortAttributes()) {
                propertyComparator = Comparator.comparing(Property::getOrderingLevel).thenComparing(propertyComparator);
            }
        }
        return propertyComparator;
    }

    /**
     * How the node is rendered in a zip.
     */
    protected enum RenderingType {
        /**
         * A single XML file named like the node - e.g. en.xml for mix:language.
         */
        XMLFILE,
        /**
         * A folder with a .content.xml (the .content.xml might be missing if the node is of type nt:folder without
         * any additional attributes).
         */
        FOLDER,
        /**
         * A file named like the node which just contains the binary content of the resource - typically for a
         * nt:file/nt:resource combination, or a standalone nt:resource. If there are additional attributes, they are
         * written into a {nodename}.dir/.content.xml.
         */
        BINARYFILE,
        /**
         * Just contained in whatever surrounds it - that is, an {@link #XMLFILE} or a {@link #FOLDER}'s .content
         * .xml.
         */
        EMBEDDED
    }

    public enum DepthMode {
        /**
         * Only the properties of the node, no subnodes
         */
        PROPERTIESONLY,
        /**
         * The properties of the node itself and its jcr:content node, if it exists.
         */
        CONTENTNODE,
        /**
         * All subnodes
         */
        DEEP
    }

    public static class Property {

        protected final String name;
        protected final Object value;
        /**
         * JCR {@link PropertyType} since that cannot be guessed from the value in various cases.
         */
        protected final Integer jcrType;

        public Property(String name, Object value, Integer jcrType) {
            this.name = name;
            this.value = value;
            this.jcrType = jcrType;
        }

        public Property(String key, Object value) {
            this(key, value, null);
        }

        public String getNs() {
            int ddot = name.indexOf(':');
            return ddot > 0 ? name.substring(0, ddot) : null;
        }

        public boolean isMultiValue() {
            return value instanceof Object[];
        }

        public boolean isBinary() {
            return value instanceof InputStream;
        }

        public String getName() {
            return name;
        }

        /**
         * Properly escaped String that can be written as attribute value into an XML file.
         */
        public String getEscapedString(String indent) {

            if (isMultiValue()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(getTypePrefix(value));
                Object[] array = cleanupArray((Object[]) value);
                if (array == null || array.length == 0) {
                    return null;
                }
                String lineBreak = "";
                if (getEscapedString(array[0]).startsWith(" ")) {
                    // if string values of an array are beginning with spaces we assume that the values
                    // should be arranged as lines of string values with the current indent for each value
                    lineBreak = "\r"; // '\r' is replaced by '\n' after escaping of embedded '\n' characters
                }
                buffer.append("[").append(lineBreak);
                for (int i = 0; i < array.length; ++i) {
                    String string = getEscapedString(array[i]);
                    string = string.replace(",", "\\,");
                    if (StringUtils.isNotEmpty(lineBreak)) {
                        string = string.trim();
                        buffer.append(indent).append(BASIC_INDENT);
                    }
                    buffer.append(string);
                    if (i + 1 < array.length) {
                        buffer.append(',').append(lineBreak);
                    }
                }
                if (StringUtils.isNotEmpty(lineBreak)) {
                    buffer.append(lineBreak).append(indent);
                }
                buffer.append("]");
                return buffer.toString();
            }

            String typePrefix = getTypePrefix(value);
            String valueString = getEscapedString(value);
            if (StringUtils.startsWith(valueString, "[")) {
                valueString = "\\" + valueString;
            }
            if (StringUtils.isNotBlank(typePrefix) || !StringUtils.startsWith(valueString, "{")) {
                return typePrefix + valueString;
            } else { // a value starting with { would be misinterpreted as type prefix -> escape it:
                return "\\" + valueString;
            }
        }

        /**
         * Cleanup of array values - if this is {@value JcrConstants#JCR_MIXINTYPES} we filter out {@link #EXCLUDED_MIXINS}.
         */
        protected Object[] cleanupArray(Object[] value) {
            Object[] result = value;
            if (JCR_MIXINTYPES.equals(name) && value != null) {
                result = Arrays.stream(value)
                        .filter(o -> !EXCLUDED_MIXINS.accept(String.valueOf(o)))
                        .toArray();
            }
            return result;
        }

        protected String getEscapedString(Object aValue) {
            if (aValue instanceof Calendar) {
                DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                return escapeXmlAttribute(formatter.format(((Calendar) aValue).getTime()));
            }
            if (aValue instanceof InputStream) { // actual value is exported in additional file.
                return "";
            }
            return aValue != null ? escapeXmlAttribute(aValue.toString()) : "";
        }

        public static String escapeXmlAttribute(String attrValue) {
            // TODO(hps,2019-07-11) use utilities? Should be consistent with package manager, though.
            // This is probably not quite complete - what about other control characters?
            // There is org.apache.jackrabbit.util.Text.encodeIllegalXMLCharacters , but that doesn't seem right.
            return attrValue.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace("\"", "&quot;")
                    .replace("\t", "&#x9;")
                    .replace("\n", "&#xa;")
                    .replace("\r", "&#xd;")
                    .replace("\\", "\\\\");
        }

        protected int getOrderingLevel() {
            if (name.equals(JCR_PRIMARYTYPE)) {
                return 1;
            }
            if (name.equals(JCR_MIXINTYPES)) {
                return 2;
            }
            if (name.startsWith("sling:")) {
                return 3;
            }
            return 4;
        }

        protected String getTypePrefix(Object aValue) {
            if (jcrType != null && jcrType != PropertyType.STRING) {
                return "{" + PropertyType.nameFromValue(jcrType) + "}";
            }
            if (aValue instanceof String || aValue instanceof String[]) {
                return "";
            } else if (aValue instanceof Boolean || aValue instanceof Boolean[]) {
                return "{" + PropertyType.TYPENAME_BOOLEAN + "}";
            } else if (aValue instanceof BigDecimal || aValue instanceof BigDecimal[]) {
                return "{" + PropertyType.TYPENAME_DECIMAL + "}";
            } else if (aValue instanceof Long || aValue instanceof Long[]) {
                return "{" + PropertyType.TYPENAME_LONG + "}";
            } else if (aValue instanceof Double || aValue instanceof Double[]) {
                return "{" + PropertyType.TYPENAME_DOUBLE + "}";
            } else if (aValue instanceof Calendar || aValue instanceof Calendar[]) {
                return "{" + PropertyType.TYPENAME_DATE + "}";
            } else if (aValue instanceof InputStream) {
                return "{" + PropertyType.TYPENAME_BINARY + "}";
            }
            return "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Property property = (Property) o;
            return Objects.equals(name, property.name) && Objects.equals(value, property.value) && Objects.equals(jcrType, property.jcrType);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }


        @Override
        public String toString() {
            return name + "=" + getEscapedString("");
        }
    }

    /**
     * Is thrown when an exception appears during {@link ZipOutputStream#close()}. This might happen
     * when a consumer of the stream does read all contents, but does not read the central directory that is written
     * during close.
     */
    public static class IOErrorOnCloseException extends IOException {
        public IOErrorOnCloseException(IOException e) {
            super(e);
        }
    }

}
