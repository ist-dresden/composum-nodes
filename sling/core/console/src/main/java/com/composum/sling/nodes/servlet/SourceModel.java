package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_RESOURCE;

public class SourceModel extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SourceModel.class);

    // TODO move to configuration
    public static final List<Pattern> EXCLUDED_PROPS;

    static {
        EXCLUDED_PROPS = new ArrayList<>();
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:primaryType"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:baseVersion"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:predecessors"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:versionHistory"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:isCheckedOut"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:created.*"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:lastModified.*"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:uuid"));
        EXCLUDED_PROPS.add(Pattern.compile("^jcr:data"));
        EXCLUDED_PROPS.add(Pattern.compile("^cq:lastModified.*"));
        EXCLUDED_PROPS.add(Pattern.compile("^cq:lastReplicat.*"));
        //EXCLUDED_PROPS.add(Pattern.compile("^cq:lastRolledout.*"));
    }

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static class Property implements Comparable<Property> {

        protected final String name;
        protected final Object value;
        /** JCR {@link PropertyType} since that cannot be guessed from the value in various cases. */
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
            return ddot > 0 ? name.substring(0, ddot) : "";
        }

        public boolean isMultiValue() {
            return value instanceof Object[];
        }

        public String getName() {
            return name;
        }

        public String getString(String indent) throws IOException {

            if (isMultiValue()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(getTypePrefix(value));
                Object[] array = (Object[]) value;
                String lineBreak = "";
                if (array.length > 0 && getString(array[0]).startsWith(" ")) {
                    // if string values of an array are beginning with spaces we assume that the values
                    // should be arranged as lines of string values with the current indent for each value
                    lineBreak = "\r"; // '\r' is replaced by '\n' after escaping of embedded '\n' characters
                }
                buffer.append("[").append(lineBreak);
                for (int i = 0; i < array.length; ) {
                    String string = getString(array[i]);
                    string = string.replaceAll(",", "\\\\,");
                    if (StringUtils.isNotEmpty(lineBreak)) {
                        string = string.trim();
                        buffer.append(indent).append("    ");
                    }
                    buffer.append(string);
                    if (++i < array.length) {
                        buffer.append(',').append(lineBreak);
                    }
                }
                if (StringUtils.isNotEmpty(lineBreak)) {
                    buffer.append(lineBreak).append(indent);
                }
                buffer.append("]");
                return buffer.toString();
            }

            return getTypePrefix(value) + getString(value);
        }

        protected String getString(Object value) throws IOException {
            if (value instanceof Calendar) {
                DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                return formatter.format(((Calendar) value).getTime());
            }
            if (value instanceof InputStream) { // actual value is exported in additional file.
                return "";
            }
            if (value instanceof String && ((String) value).startsWith("{")) {
                // a value starting with { would be misinterpreted as type prefix -> escape it:
                value = "\\" + value;
            }
            return value != null ? value.toString() : "";
        }

        protected String getTypePrefix(Object value) {
            if (jcrType != null && jcrType != PropertyType.STRING) {
                return "{" + PropertyType.nameFromValue(jcrType) + "}";
            }
            if (value instanceof String || value instanceof String[]) {
                return "";
            } else if (value instanceof Boolean || value instanceof Boolean[]) {
                return "{" + PropertyType.TYPENAME_BOOLEAN + "}";
            } else if (value instanceof BigDecimal || value instanceof BigDecimal[]) {
                return "{" + PropertyType.TYPENAME_DECIMAL + "}";
            } else if (value instanceof Long || value instanceof Long[]) {
                return "{" + PropertyType.TYPENAME_LONG + "}";
            } else if (value instanceof Double || value instanceof Double[]) {
                return "{" + PropertyType.TYPENAME_DOUBLE + "}";
            } else if (value instanceof Calendar || value instanceof Calendar[]) {
                return "{" + PropertyType.TYPENAME_DATE + "}";
            } else if (value instanceof InputStream || value instanceof InputStream[]) {
                return "{" + PropertyType.TYPENAME_BINARY + "}";
            }
            return "";
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(Property other) {
            if (other == null) {
                return 1;
            }
            String ns = getNs();
            String ons = other.getNs();
            if (ns.isEmpty() && !ons.isEmpty()) {
                return 1;
            }
            if (!ns.isEmpty() && ons.isEmpty()) {
                return -1;
            }
            if (!ns.equals(ons)) {
                return ns.compareTo(ons);
            }
            return getName().compareTo(other.getName());
        }
    }

    protected final NodesConfiguration config;

    private transient List<Property> propertyList;
    private transient List<Resource> subnodeList;

    public SourceModel(NodesConfiguration config, BeanContext context, Resource resource) {
        if ("/".equals(ResourceUtil.normalize(resource.getPath()))) {
            throw new IllegalArgumentException("Cannot export the whole JCR - " + resource.getPath());
        }
        this.config = config;
        initialize(context, resource);
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    public String getPrimaryType() {
        return StringUtils.defaultString(ResourceUtil.getPrimaryType(resource));
    }

    public FileTime getLastModified(ResourceHandle someResource) {
        Calendar timestamp;
        timestamp = someResource.getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        if (timestamp == null) {
            timestamp = someResource.getProperties().get(JcrConstants.JCR_CREATED, Calendar.class);
        }
        timestamp = someResource.getContentResource().getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        if (timestamp == null) {
            timestamp = someResource.getContentResource().getProperties().get(JcrConstants.JCR_CREATED, Calendar.class);
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
                    } catch (RepositoryException e) { // shouldn't happen
                        LOG.warn("Error reading property {}/{}", new Object[]{resource.getPath(), entry.getValue(), e});
                    }
                }
                Property property = new Property(entry.getKey(), entry.getValue(), type);
                if (!isExcluded(property)) {
                    propertyList.add(property);
                }
            }
            Collections.sort(propertyList);
        }
        return propertyList;
    }

    protected boolean isExcluded(Property property) {
        for (Pattern rule : EXCLUDED_PROPS) {
            if (rule.matcher(property.getName()).matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean getHasSubnodes() {
        return !getSubnodeList().isEmpty();
    }

    public List<Resource> getSubnodeList() {
        if (subnodeList == null) {
            subnodeList = new ArrayList<>();
            Iterator<Resource> iterator = resource.listChildren();
            while (iterator.hasNext()) {
                Resource subnode = iterator.next();
                if (config.getSourceNodesFilter().accept(subnode)) {
                    subnodeList.add(subnode);
                }
            }
        }
        return subnodeList;
    }

    public void determineNamespaces(List<String> keys, boolean contentOnly) {
        String primaryType = getPrimaryType();
        addNameNamespace(keys, primaryType);
        List<Property> properties = getPropertyList();
        for (Property property : properties) {
            String ns = property.getNs();
            addNamespace(keys, ns);
        }
        addNameNamespace(keys, resource.getName());
        if (contentOnly) {
            Resource contentResource;
            if ((contentResource = resource.getChild(JcrConstants.JCR_CONTENT)) != null) {
                SourceModel subnodeModel = new SourceModel(config, context, contentResource);
                subnodeModel.determineNamespaces(keys, false);
            }
        } else {
            for (Resource subnode : getSubnodeList()) {
                SourceModel subnodeModel = new SourceModel(config, context, subnode);
                subnodeModel.determineNamespaces(keys, false);
            }
        }
    }

    public void addNameNamespace(List<String> keys, String name) {
        String ns = getNamespace(name);
        addNamespace(keys, ns);
    }

    public void addNamespace(List<String> keys, String ns) {
        if (StringUtils.isNotBlank(ns) && !keys.contains(ns)) {
            keys.add(ns);
        }
    }

    public String getNamespace(String name) {
        int delim = name.indexOf(':');
        return delim < 0 ? "" : name.substring(0, delim);
    }

    // Package output

    /** Writes a complete package about the node - arguments specify the package metadata. */
    public void writePackage(OutputStream output, String group, String name, String version)
            throws IOException, RepositoryException {

        String root = "jcr_root";
        ZipOutputStream zipStream = new ZipOutputStream(output);
        writePackageProperties(zipStream, group, name, version);
        writeFilterXml(zipStream);
        writeParents(zipStream, root, resource.getParent());
        writeZip(zipStream, root, true);
        zipStream.flush();
        zipStream.close();
    }

    protected void writePackageProperties(ZipOutputStream zipStream, String group, String name, String version)
            throws IOException {

        ZipEntry entry;
        entry = new ZipEntry("META-INF/vault/properties.xml");
        zipStream.putNextEntry(entry);

        Writer writer = new OutputStreamWriter(zipStream, "UTF-8");
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n")
                .append("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n")
                .append("<properties>\n")
                .append("<comment>FileVault Package Properties</comment>\n")
                .append("<entry key=\"name\">")
                .append(name)
                .append("</entry>\n")
                .append("<entry key=\"buildCount\">1</entry>\n")
                .append("<entry key=\"version\">")
                .append(version)
                .append("</entry>\n")
                .append("<entry key=\"packageFormatVersion\">2</entry>\n")
                .append("<entry key=\"group\">")
                .append(group)
                .append("</entry>\n")
                .append("<entry key=\"description\">created from source download</entry>\n")
                .append("</properties>");

        writer.flush();
        zipStream.closeEntry();
    }

    protected void writeFilterXml(ZipOutputStream zipStream) throws IOException {

        String path = resource.getPath();

        ZipEntry entry;
        entry = new ZipEntry("META-INF/vault/filter.xml");
        zipStream.putNextEntry(entry);

        Writer writer = new OutputStreamWriter(zipStream, "UTF-8");
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<workspaceFilter version=\"1.0\">\n")
                .append("    <filter root=\"")
                .append(path)
                .append("\"/>\n")
                .append("</workspaceFilter>\n");

        writer.flush();
        zipStream.closeEntry();
    }

    /** Writes all the .content.xml of the parents of root into the zip. */
    protected void writeParents(@Nonnull ZipOutputStream zipStream, @Nonnull String root, @Nonnull Resource parent)
            throws IOException, RepositoryException {
        if (parent != null && !"/".equals(parent.getPath())) {
            writeParents(zipStream, root, parent.getParent());
            SourceModel parentModel = new SourceModel(config, context, parent);
            parentModel.writeZip(zipStream, root, false);
        }
    }

    // ZIP output

    /** Writes a "naked" Zip about the node: no package metadata, no parent nodes. */
    public void writeArchive(@Nonnull OutputStream output)
            throws IOException, RepositoryException {

        ZipOutputStream zipStream = new ZipOutputStream(output);
        writeZip(zipStream, resource.getPath(), true);
        zipStream.flush();
        zipStream.close();
    }

    /**
     * Writes a "naked" Zip about the node: no package metadata, no parent nodes.
     *
     * @param zipStream the stream to write to, not closed.
     * @param writeDeep if true, we also write subnodes recursively. If not, only a jcr:content node is written, if
     *                  present.
     */
    public void writeZip(@Nonnull ZipOutputStream zipStream, @Nonnull String root, boolean writeDeep)
            throws IOException, RepositoryException {
        if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
            return;
        }
        if (ResourceUtil.isFile(resource)) {
            if (writeDeep) { writeFile(zipStream, root, resource); }
            // not writeDeep: a .content.xml is not present for a file, so we can't do anything.
            return;
        }

        ZipEntry entry;
        String path = resource.getPath();
        FileTime lastModified = getLastModified(resource);

        entry = new ZipEntry(getZipName(root, path + "/.content.xml"));
        if (lastModified != null) {
            entry.setLastModifiedTime(lastModified);
        }
        zipStream.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(zipStream, "UTF-8");
        writeContentXmlFile(writer, true, false);
        writer.flush();
        zipStream.closeEntry();

        if (writeDeep) {
            for (Resource subnode : getSubnodeList()) {
                if (!JcrConstants.JCR_CONTENT.equals(subnode.getName())) {
                    if (ResourceUtil.isFile(subnode)) {
                        writeFile(zipStream, root, ResourceHandle.use(subnode));
                    } else {
                        SourceModel subnodeModel = new SourceModel(config, context, subnode);
                        subnodeModel.writeZip(zipStream, root, true);
                    }
                }
            }
        }
    }

    /**
     * Writes the current node as a file node (not the jcr:content but the parent) incl. it's binary data and possibly
     * additional data about nonstandard properties.
     */
    protected void writeFile(ZipOutputStream zipStream, String root, ResourceHandle file)
            throws IOException, RepositoryException {
        if (file.getName().equals(JcrConstants.JCR_CONTENT)) {
            // file format doesn't allow this - we need to write the file with the parent's name
            file = file.getParent();
        }

        FileTime lastModified = getLastModified(file);
        ZipEntry entry;
        String path = file.getPath();
        Binary binaryData = ResourceUtil.getBinaryData(file);
        if (binaryData != null) {
            entry = new ZipEntry(getZipName(root, path));
            if (lastModified != null) {
                entry.setLastModifiedTime(lastModified);
            }
            zipStream.putNextEntry(entry);
            try (InputStream fileContent = binaryData.getStream()) {
                IOUtils.copy(fileContent, zipStream);
            }
            zipStream.closeEntry();
        } else {
            LOG.info("Can't get binary data for {}", path);
        }

        // if it's more than a nt:file/nt:resource construct that contains additional attributes we have to write
        // an additional {file}.dir/.content.xml .
        boolean fileIsNonstandard = file.getProperty(JCR_MIXINTYPES, new String[0].length) > 0
                || !NT_FILE.equals(file.getProperty(JCR_PRIMARYTYPE, String.class));
        boolean contentNodeIsNonstandard = file.getContentResource().getProperty(JCR_MIXINTYPES, new String[0]).length > 0
                || !NT_RESOURCE.equals(file.getContentResource().getProperty(JCR_PRIMARYTYPE, String.class));
        if (fileIsNonstandard || contentNodeIsNonstandard) {
            entry = new ZipEntry(getZipName(root, file.getPath() + ".dir/.content.xml"));
            if (lastModified != null) {
                entry.setLastModifiedTime(lastModified);
            }
            zipStream.putNextEntry(entry);
            Writer writer = new OutputStreamWriter(zipStream, UTF_8);
            SourceModel fileModel = new SourceModel(config, context, file);
            fileModel.writeContentXmlFile(writer, false, false);
            writer.flush();
            zipStream.closeEntry();
        }
    }

    protected String getZipName(@Nonnull String root, @Nonnull String path) {
        String name = path;
        if (name.startsWith(root)) {
            name = name.substring(root.length() + 1);
        } else {
            name = root + name;
        }
        return name;
    }

    // XML output

    /**
     * Writes the data for the .content.xml file for the node, including an jcr:content node if present.
     *
     * @param contentOnly    if true, subnodes other than jcr:content are also included
     * @param noSubnodeNames if false and the node has a jcr:content, we also write nodes for the names of the
     *                       siblings of the jcr:content node to indicate the order of the subnodes.
     */
    protected void writeContentXmlFile(Writer writer, boolean contentOnly, boolean noSubnodeNames)
            throws IOException, RepositoryException {
        List<String> namespaces = new ArrayList<>();
        namespaces.add("jcr");
        determineNamespaces(namespaces, contentOnly);
        Collections.sort(namespaces);
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<jcr:root");
        writeNamespaceAttributes(writer, namespaces);
        writeProperty(writer, "          ", "jcr:primaryType", getPrimaryType());
        writeProperties(writer, "          ");
        writer.append(">\n");
        Resource contentResource;
        if ((contentResource = resource.getChild(JcrConstants.JCR_CONTENT)) != null) {
            SourceModel subnodeModel = new SourceModel(config, context, contentResource);
            subnodeModel.writeXml(writer, "    ");
            if (!noSubnodeNames) {
                for (Resource subnode : getSubnodeList()) {
                    String name = subnode.getName();
                    if (!JcrConstants.JCR_CONTENT.equals(name)) {
                        writer.append("    <").append(name).append("/>\n");
                    }
                }
            }
        } else {
            if (!contentOnly) {
                writeSubnodesAsXml(writer, "    ");
            }
        }
        writer.append("</jcr:root>\n");
    }

    /** Writes the node including subnodes as XML, using the base indentation. */
    protected void writeXml(Writer writer, String indent) throws IOException {
        String name = escapeXmlName(getName());
        writer.append(indent).append("<").append(name).append('\n');
        writer.append(indent).append("        ").append("jcr:primaryType=\"").append(getPrimaryType()).append("\"");
        writeProperties(writer, indent + "        ");
        if (getHasSubnodes()) {
            writer.append(">\n");
            writeSubnodesAsXml(writer, indent + "    ");
            writer.append(indent).append("</").append(name).append(">\n");
        } else {
            writer.append("/>\n");
        }
    }

    protected void writeSubnodesAsXml(Writer writer, String indent) throws IOException {
        for (Resource subnode : getSubnodeList()) {
            SourceModel subnodeModel = new SourceModel(config, context, subnode);
            subnodeModel.writeXml(writer, indent);
        }
    }

    protected void writeProperties(Writer writer, String indent) throws IOException {
        for (Property property : getPropertyList()) {
            writeProperty(writer, indent, property.getName(), property.getString(indent));
        }
    }

    protected void writeProperty(Writer writer, String indent, String name, String value)
            throws IOException {
        writer.append("\n");
        writer.append(indent);
        writer.append(escapeXmlName(name));
        writer.append("=\"");
        writer.append(escapeXmlAttribute(value));
        writer.append("\"");
    }

    protected void writeNamespaceAttributes(Writer writer, List<String> namespaces) throws RepositoryException, IOException {
        for (int i = 0; i < namespaces.size(); ) {
            String ns = namespaces.get(i);
            String url = getSession().getNamespaceURI(ns);
            if (StringUtils.isNotBlank(url)) {
                writer.append(" xmlns:").append(ns).append("=\"").append(url).append("\"");
                if (++i < namespaces.size()) {
                    writer.append("\n         ");
                }
            } else {
                i++;
            }
        }
    }

    public String escapeXmlName(String name) {
        return ISO9075.encode(name)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("'", "&apos;")
                .replaceAll("\"", "&quot;");
    }

    public String escapeXmlAttribute(String value) {
        // TODO(hps,2019-07-11) use utilities? Should be consistent with package manager, though.
        return value
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll("'", "&apos;")
                .replaceAll("\"", "&quot;")
                .replaceAll("\t", "&#x9;")
                .replaceAll("\n", "&#xa;")
                .replaceAll("\r", "\n");
    }
}
