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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SourceModel extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SourceModel.class);

    // TODO move to configuration
    public static final Map<String, String> NAMESPACES;

    static {
        NAMESPACES = new HashMap<>();
        NAMESPACES.put("jcr", "http://www.jcp.org/jcr/1.0");
        NAMESPACES.put("nt", "http://www.jcp.org/jcr/nt/1.0");
        NAMESPACES.put("mix", "http://www.jcp.org/jcr/mix/1.0");
        NAMESPACES.put("sling", "http://sling.apache.org/jcr/sling/1.0");
        NAMESPACES.put("cpp", "http://sling.composum.com/pages/1.0");
        NAMESPACES.put("cpa", "http://sling.composum.com/assets/1.0");
        NAMESPACES.put("cq", "http://www.day.com/jcr/cq/1.0");
    }

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

        public String getName() {
            return name;
        }

        public String getString() {

            if (value instanceof Object[]) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(getTypePrefix(value));
                Object[] array = (Object[]) value;
                buffer.append("[");
                for (int i = 0; i < array.length; ) {
                    buffer.append(getString(array[i]));
                    if (++i < array.length) {
                        buffer.append(',');
                    }
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

        @Override
        public int compareTo(Property other) {
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
        this.config = config;
        initialize(context, resource);
    }

    public String getName() {
        return resource.getName();
    }

    public String getPrimaryType() {
        try {
            return resource.adaptTo(Node.class).getPrimaryNodeType().getName();
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return "";
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

        entry = new ZipEntry(getZipName(root, path + "/.content.xml"));
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

    public void writeFile(Writer writer, boolean contentOnly) throws IOException {
        List<String> namespaces = new ArrayList<>();
        namespaces.add("jcr");
        determineNamespaces(namespaces, contentOnly);
        Collections.sort(namespaces);
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<jcr:root");
        for (int i = 0; i < namespaces.size(); ) {
            String ns = namespaces.get(i);
            String url = NAMESPACES.get(ns);
            if (StringUtils.isNotBlank(url)) {
                writer.append(" xmlns:").append(ns).append("=\"").append(url).append("\"");
                if (++i < namespaces.size()) {
                    writer.append("\n         ");
                }
            } else {
                i++;
            }
        }
        writeProperty(writer, "        ", "jcr:primaryType", getPrimaryType());
        writeProperties(writer, "        ");
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
            writeProperty(writer, indent, property.getName(), property.getString());
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

    public String escapeXmlAttribute(String value) {
        return value
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll("'", "&apos;")
                .replaceAll("\"", "&quot;")
                .replaceAll("\t", "&#x9;")
                .replaceAll("\n", "&#xa;");
    }
}
