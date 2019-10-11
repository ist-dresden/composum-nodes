package com.composum.sling.nodes.servlet;

import com.composum.sling.core.BeanContext;
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

    public static final FileTime NO_TIME = FileTime.from(0, TimeUnit.MILLISECONDS);

    public class Property implements Comparable<Property> {

        protected final String name;
        protected final Object value;

        public Property(String name, Object value) {
            this.name = name;
            this.value = value;
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

        public String getString(String indent) {

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

        public String getString(Object value) {
            if (value instanceof Calendar) {
                DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                return formatter.format(((Calendar) value).getTime());
            }
            return value != null ? value.toString() : "";
        }

        public String getTypePrefix(Object value) {
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

    private transient FileTime lastModified;
    private transient List<Property> propertyList;
    private transient List<Resource> subnodeList;

    public SourceModel(NodesConfiguration config, BeanContext context, Resource resource) {
        if ("/".equals(ResourceUtil.normalize(resource.getPath())))
            throw new IllegalArgumentException("Cannot export the whole JCR - " + resource.getPath());
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

    public FileTime getLastModified() {
        if (lastModified == null) {
            Calendar timestamp = resource.getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
            lastModified = timestamp != null ?
                    FileTime.from(timestamp.getTimeInMillis(), TimeUnit.MILLISECONDS) : NO_TIME;
        }
        return lastModified == NO_TIME ? null : lastModified;
    }

    public List<Property> getPropertyList() {
        if (propertyList == null) {
            propertyList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {
                Property property = new Property(entry.getKey(), entry.getValue());
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

    public void writePackage(OutputStream output, String group, String name, String version)
            throws IOException, RepositoryException {

        String root = "jcr_root";
        ZipOutputStream zipStream = new ZipOutputStream(output);
        writeProperties(zipStream, group, name, version);
        writeFilter(zipStream);
        writeParents(zipStream, root, resource.getParent());
        writeZip(zipStream, root, true);
        zipStream.flush();
        zipStream.close();
    }

    public void writeProperties(ZipOutputStream zipStream, String group, String name, String version)
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

    public void writeFilter(ZipOutputStream zipStream) throws IOException {

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

    public void writeParents(ZipOutputStream zipStream, String root, Resource parent)
            throws IOException, RepositoryException {
        if (parent != null && !"/".equals(parent.getPath())) {
            writeParents(zipStream, root, parent.getParent());
            SourceModel parentModel = new SourceModel(config, context, parent);
            parentModel.writeZip(zipStream, root, false);
        }
    }

    // ZIP output

    public void writeArchive(OutputStream output)
            throws IOException, RepositoryException {

        ZipOutputStream zipStream = new ZipOutputStream(output);
        writeZip(zipStream, resource.getPath(), true);
        zipStream.flush();
        zipStream.close();
    }

    public void writeZip(ZipOutputStream zipStream, String root, boolean writeDeep)
            throws IOException, RepositoryException {

        ZipEntry entry;
        String path = resource.getPath();
        FileTime lastModified = getLastModified();

        entry = new ZipEntry(getZipName(root, path + "/.content.xml"));
        if (lastModified != null) {
            entry.setLastModifiedTime(lastModified);
        }
        zipStream.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(zipStream, "UTF-8");
        writeFile(writer, true);
        writer.flush();
        zipStream.closeEntry();

        if (writeDeep) {
            for (Resource subnode : getSubnodeList()) {
                if (!JcrConstants.JCR_CONTENT.equals(subnode.getName())) {
                    if (ResourceUtil.isFile(subnode)) {
                        writeFile(zipStream, root, subnode);
                    } else {
                        SourceModel subnodeModel = new SourceModel(config, context, subnode);
                        subnodeModel.writeZip(zipStream, root, true);
                    }
                }
            }
        }
    }

    public void writeFile(ZipOutputStream zipStream, String root, Resource file)
            throws IOException, RepositoryException {
        ZipEntry entry;
        String path = file.getPath();
        entry = new ZipEntry(getZipName(root, path));
        zipStream.putNextEntry(entry);
        try (InputStream fileContent = ResourceUtil.getBinaryData(file).getStream()) {
            IOUtils.copy(fileContent, zipStream);
        }
        zipStream.closeEntry();
    }

    public String getZipName(String root, String path) {
        String name = path;
        if (name.startsWith(root)) {
            name = name.substring(root.length() + 1);
        } else {
            name = root + name;
        }
        return name;
    }

    // XML output

    public void writeFile(Writer writer, boolean contentOnly) throws IOException, RepositoryException {
        List<String> namespaces = new ArrayList<>();
        namespaces.add("jcr");
        determineNamespaces(namespaces, contentOnly);
        Collections.sort(namespaces);
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<jcr:root");
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
        writeProperty(writer, "          ", "jcr:primaryType", getPrimaryType());
        writeProperties(writer, "          ");
        writer.append(">\n");
        Resource contentResource;
        if ((contentResource = resource.getChild(JcrConstants.JCR_CONTENT)) != null) {
            SourceModel subnodeModel = new SourceModel(config, context, contentResource);
            subnodeModel.writeXml(writer, "    ");
            for (Resource subnode : getSubnodeList()) {
                String name = subnode.getName();
                if (!JcrConstants.JCR_CONTENT.equals(name)) {
                    writer.append("    <").append(name).append("/>\n");
                }
            }
        } else {
            if (!contentOnly) {
                writeSubnodes(writer, "    ");
            }
        }
        writer.append("</jcr:root>\n");
    }

    public void writeXml(Writer writer, String indent) throws IOException {
        String name = escapeXmlName(getName());
        writer.append(indent).append("<").append(name).append('\n');
        writer.append(indent).append("        ").append("jcr:primaryType=\"").append(getPrimaryType()).append("\"");
        writeProperties(writer, indent + "        ");
        if (getHasSubnodes()) {
            writer.append(">\n");
            writeSubnodes(writer, indent + "    ");
            writer.append(indent).append("</").append(name).append(">\n");
        } else {
            writer.append("/>\n");
        }
    }

    public void writeSubnodes(Writer writer, String indent) throws IOException {
        for (Resource subnode : getSubnodeList()) {
            SourceModel subnodeModel = new SourceModel(config, context, subnode);
            subnodeModel.writeXml(writer, indent);
        }
    }

    public void writeProperties(Writer writer, String indent) throws IOException {
        for (Property property : getPropertyList()) {
            writeProperty(writer, indent, property.getName(), property.getString(indent));
        }
    }

    public void writeProperty(Writer writer, String indent, String name, String value)
            throws IOException {
        writer.append("\n");
        writer.append(indent);
        writer.append(escapeXmlName(name));
        writer.append("=\"");
        writer.append(escapeXmlAttribute(value));
        writer.append("\"");
    }

    public String escapeXmlName(String name) {
        return ISO9075.encode(name)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("'", "&apos;")
                .replaceAll("\"", "&quot;");
    }

    public String escapeXmlAttribute(String value) { // FIXME(hps,2019-07-11) use utilities? Should be consistent with packge manager, though.
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
