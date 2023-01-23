package com.composum.sling.core.pckgmgr.jcrpckg.util;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeNode;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.XSS;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.PropertyNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.composum.sling.core.pckgmgr.Packages.REGISTRY_PATH;

/**
 * Helper methods for Package handling (VLT Package Manager)
 */
public class PackageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PackageUtil.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String THUMBNAIL_PNG = "thumbnail.png";

    public static final String DEF_AC_HANDLING = JcrPackageDefinition.PN_AC_HANDLING;
    public static final String DEF_DEPENDENCIES = JcrPackageDefinition.PN_DEPENDENCIES;
    public static final String DEF_DESCRIPTION = JcrPackageDefinition.PN_DESCRIPTION;
    public static final String DEF_DISABLE_INTERMEDIATE_SAVE = JcrPackageDefinition.PN_DISABLE_INTERMEDIATE_SAVE;
    public static final String DEF_PROVIDER_LINK = "providerLink";
    public static final String DEF_PROVIDER_NAME = "providerName";
    public static final String DEF_PROVIDER_URL = "providerUrl";
    public static final String DEF_REPLACES = "replaces";
    public static final String DEF_REQUIRES_RESTART = JcrPackageDefinition.PN_REQUIRES_RESTART;
    public static final String DEF_REQUIRES_ROOT = JcrPackageDefinition.PN_REQUIRES_ROOT;
    public static final String DEF_TESTED_WITH = "testedWith";
    public static final String DEF_INCLUDE_VERSIONS = "includeVersions";

    public static final Pattern IMPORT_DONE = Pattern.compile("^Package imported\\.$");

    public enum ViewType {
        packages, registry, group, jcrpckg, regpckg, version
    }

    /**
     * Retrieves a package manager for the JCR session.
     */
    @Nonnull
    public static JcrPackageManager getPackageManager(@Nonnull Packaging packaging, @Nonnull SlingHttpServletRequest request) throws RepositoryException {
        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        if (session != null) {
            return packaging.getPackageManager(session);
        } else {
            throw new RepositoryException("can't adapt resolver to session"); // should be impossible
        }
    }

    public static String getPath(SlingHttpServletRequest request) {
        RequestPathInfo reqPathInfo = request.getRequestPathInfo();
        String path = XSS.filter(reqPathInfo.getSuffix());
        if (StringUtils.isBlank(path)) {
            final String pathParam = XSS.filter(request.getParameter("path"));
            if (StringUtils.isBlank(pathParam)) {
                path = "/";
            } else {
                path = pathParam;
            }
        } else {
            while (path.endsWith("/") && !"/".equals(path)) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    public static Resource getResource(JcrPackageManager manager, SlingHttpServletRequest request, String path) throws RepositoryException {
        Resource resource = null;
        Node node;
        node = manager.getPackageRoot(true);
        if (node != null) {
            ResourceResolver resolver = request.getResourceResolver();
            String resourcePath = node.getPath() + path;
            resource = resolver.getResource(resourcePath);
        }
        return resource;
    }

    public static JcrPackage getJcrPackage(JcrPackageManager manager, String group, String name) throws RepositoryException {
        final List<JcrPackage> jcrPackages = manager.listPackages(group, false);
        for (JcrPackage jcrPackage : jcrPackages) {
            final String packageName = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_NAME);
            if (packageName.equals(name)) {
                return jcrPackage;
            }
        }
        return null;
    }

    public static JcrPackage getJcrPackage(JcrPackageManager manager, Resource resource) throws RepositoryException {
        JcrPackage jcrPackage = null;
        Node node;
        if (ResourceHandle.isValid(resource) && (node = resource.adaptTo(Node.class)) != null) {
            jcrPackage = manager.open(node, true);
        }
        return jcrPackage;
    }

    public static String getPackagePath(JcrPackageManager pckgMgr, JcrPackage pckg) {
        String path = "";
        if (pckg != null) {
            try {
                Node node = pckg.getNode();
                path = node.getPath();
                Node rootNode = pckgMgr.getPackageRoot(true);
                String root = rootNode.getPath();
                if (path.startsWith(root)) {
                    path = path.substring(root.length());
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return path;
    }

    public static ViewType getViewType(BeanContext context, SlingHttpServletRequest request, String path) {
        if ("/".equals(path)){
            return ViewType.packages;
        }
        if (StringUtils.isNotBlank(path) && REGISTRY_PATH.matcher(path).matches()){
            return ViewType.registry;
        }
        ViewType type = ViewType.group;
        try {
            JcrPackageManager manager = PackageUtil.getPackageManager(context.getService(Packaging.class), request);
            Resource resource = getResource(manager, request, path);
            JcrPackage jcrPackage = getJcrPackage(manager, resource);
            Packages.Mode mode = Packages.getMode(request);
            if (jcrPackage != null) {
                type = ViewType.valueOf(mode.name());
            } else {
                PackageRegistries.Registries registries = context.getService(PackageRegistries.class).getRegistries(request.getResourceResolver());
                Pair<String, PackageId> found = registries.resolve(path);
                if (found != null) {
                    if (path.equals(RegistryUtil.toPath("", found.getRight()))
                            || path.equals(RegistryUtil.toPath(found.getLeft(), found.getRight()))) {
                        type = ViewType.version;
                    } else if (path.equals(RegistryUtil.toPackagePath("", found.getRight()))
                            || path.equals(RegistryUtil.toPackagePath(found.getLeft(), found.getRight()))) {
                        type = mode == Packages.Mode.jcrpckg ? ViewType.group : ViewType.regpckg;
                    }
                } else {
                    // probably an invalid package - should shown as a package
                    String lowercase = path.toLowerCase();
                    if (lowercase.endsWith(".zip") || lowercase.endsWith(".jar")) {
                        type = ViewType.valueOf(mode.name());
                    }
                }
            }
        } catch (RepositoryException | IOException | RuntimeException ex) {
            LOG.error("Error accessing {}", path, ex);
        }
        return type;
    }

    @Nullable
    public static String getGroupPath(@Nullable JcrPackage pckg) throws RepositoryException {
        return getGroupPath(pckg.getDefinition());
    }

    @Nullable
    public static String getGroupPath(@Nullable JcrPackageDefinition pckgDef) {
        String group = null;
        if (pckgDef != null) {
            group = pckgDef.get(JcrPackageDefinition.PN_GROUP);
            group = StringUtils.isNotBlank(group) ? ("/" + group + "/") : "/";
        }
        return group;
    }

    public static boolean isGroup(JcrPackageDefinition pckgDef, String group) {
        return equals(pckgDef, JcrPackageDefinition.PN_GROUP, group);
    }

    public static boolean isName(JcrPackageDefinition pckgDef, String name) {
        return equals(pckgDef, JcrPackageDefinition.PN_NAME, name);
    }

    public static boolean isVersion(JcrPackageDefinition pckgDef, String version) {
        return equals(pckgDef, JcrPackageDefinition.PN_VERSION, version);
    }

    public static boolean equals(JcrPackageDefinition pckgDef, String key, String value) {
        String current = pckgDef.get(key);
        return StringUtils.isNotBlank(current) ? current.equals(value) : StringUtils.isBlank(value);
    }

    public static String getFilename(JcrPackage pckg) {
        StringBuilder filename = new StringBuilder();
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    filename.append(pckgDef.get(JcrPackageDefinition.PN_NAME));
                    String version = pckgDef.get(JcrPackageDefinition.PN_VERSION);
                    if (StringUtils.isNotBlank(version)) {
                        filename.append('-').append(version);
                    }
                    filename.append(".zip");
                } else {
                    Node node = pckg.getNode();
                    filename.append(node.getName());
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return filename.toString();
    }

    public static String getDownloadUrl(JcrPackage pckg) {
        StringBuilder downloadUrl = new StringBuilder();
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    downloadUrl.append("/bin/cpm/package.download.zip/")
                            .append(pckgDef.get(JcrPackageDefinition.PN_GROUP))
                            .append("/")
                            .append(getFilename(pckg));
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return downloadUrl.toString();
    }

    public static <T> T getDefAttr(JcrPackageDefinition pckgDef, String key, T defaultValue) {
        T value = null;
        if (pckgDef != null) {
            Class<?> type = defaultValue != null ? defaultValue.getClass() : String.class;
            if (Calendar.class.isAssignableFrom(type)) {
                value = (T) pckgDef.getCalendar(key);
            } else {
                value = (T) pckgDef.get(key);
            }
        }
        return value != null ? value : defaultValue;
    }

    public static Calendar getLastModified(JcrPackage pckg) {
        Calendar result = null;
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    result = pckgDef.getLastModified();
                }
                if (result == null) {
                    Node node = pckg.getNode();
                    if (node != null && (node = node.getNode(JcrConstants.JCR_CONTENT)) != null
                            && node.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
                        Property property = node.getProperty(JcrConstants.JCR_LASTMODIFIED);
                        result = property.getDate();
                    }
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return result;
    }

    public static void setLastModified(JcrPackageDefinition pckgDef, Calendar time) {
        if (pckgDef != null) {
            pckgDef.set(JcrConstants.JCR_LASTMODIFIED, time, true);
        }
    }

    public static void setLastModified(JcrPackageDefinition pckgDef) {
        Calendar now = Calendar.getInstance();
        setLastModified(pckgDef, now);
    }

    public static String getLastModifiedBy(JcrPackage pckg) {
        String result = null;
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    result = pckgDef.getLastModifiedBy();
                }
                if (result == null) {
                    Node node = pckg.getNode();
                    if (node != null && (node = node.getNode(JcrConstants.JCR_CONTENT)) != null
                            && node.hasProperty(JcrConstants.JCR_LAST_MODIFIED_BY)) {
                        Property property = node.getProperty(JcrConstants.JCR_LAST_MODIFIED_BY);
                        result = property.getString();
                    }
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return result;
    }

    public static Calendar getCreated(JcrPackage pckg) {
        Calendar result = null;
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    result = pckgDef.getCreated();
                }
                if (result == null) {
                    Node node = pckg.getNode();
                    if (node != null && node.hasProperty(JcrConstants.JCR_CREATED)) {
                        Property property = node.getProperty(JcrConstants.JCR_CREATED);
                        result = property.getDate();
                    }
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return result;
    }

    public static String getCreatedBy(JcrPackage pckg) {
        String result = null;
        if (pckg != null) {
            try {
                JcrPackageDefinition pckgDef = pckg.getDefinition();
                if (pckgDef != null) {
                    result = pckgDef.getCreatedBy();
                }
                if (result == null) {
                    Node node = pckg.getNode();
                    if (node != null && node.hasProperty(JcrConstants.JCR_CREATED_BY)) {
                        Property property = node.getProperty(JcrConstants.JCR_CREATED_BY);
                        result = property.getString();
                    }
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return result;
    }

    public static void getCoverage(JcrPackageDefinition pckgDef, Session session,
                                   ProgressTrackerListener listener) {
        if (pckgDef != null) {
            try {
                pckgDef.dumpCoverage(listener);
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
                listener.onError(ProgressTrackerListener.Mode.TEXT, "exception thrown", rex);
            }
        }
    }

    // Filters

    public static WorkspaceFilter getFilter(JcrPackageDefinition pckgDef) throws RepositoryException {
        WorkspaceFilter filter = null;
        if (pckgDef != null) {
            MetaInf metaInf = pckgDef.getMetaInf();
            filter = metaInf.getFilter();
        }
        return filter;
    }

    public static List<PathFilterSet> getFilterList(JcrPackageDefinition pckgDef) throws RepositoryException {
        WorkspaceFilter filter = getFilter(pckgDef);
        return filter != null ? filter.getFilterSets() : new ArrayList<PathFilterSet>();
    }

    // Thumbnail

    public static String getThumbnailPath(JcrPackageDefinition pckgDef) throws RepositoryException {
        if (pckgDef != null) {
            Node pckgDefNode = pckgDef.getNode();
            if (pckgDefNode.hasNode(THUMBNAIL_PNG)) {
                return pckgDefNode.getPath() + "/" + THUMBNAIL_PNG;
            }
        }
        return "";
    }

    public static void setThumbnail(JcrPackageDefinition pckgDef,
                                    ResourceResolver resolver, InputStream pngStream)
            throws PersistenceException, RepositoryException {
        Resource pckgDefRes = resolver.getResource(pckgDef.getNode().getPath());
        Resource thumbnail = pckgDefRes.getChild(THUMBNAIL_PNG);
        if (thumbnail != null) {
            resolver.delete(thumbnail);
            resolver.commit();
        }
        if (pngStream != null) {
            HashMap<String, Object> fileProps = new HashMap<>();
            fileProps.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_FILE);
            Resource fileRes = resolver.create(pckgDefRes, THUMBNAIL_PNG, fileProps);
            HashMap<String, Object> contentProps = new HashMap<>();
            contentProps.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_RESOURCE);
            contentProps.put(ResourceUtil.PROP_MIME_TYPE, "image/png");
            contentProps.put(ResourceUtil.PROP_DATA, pngStream);
            resolver.create(fileRes, ResourceUtil.CONTENT_NODE, contentProps);
            resolver.commit();
        }
    }

    //
    // Tree Mapping of the flat Package list
    //

    public static TreeNode getTreeNode(JcrPackageManager manager, SlingHttpServletRequest request) throws RepositoryException {

        String path = PackageUtil.getPath(request);
        List<JcrPackage> jcrPackages = manager.listPackages();

        TreeNode treeNode = new TreeNode(path);
        for (JcrPackage jcrPackage : jcrPackages) {
            if (treeNode.addPackage(jcrPackage)) {
                break;
            }
        }

        return treeNode;
    }

    //
    // JSON mapping helpers
    //

    public static void toJson(@Nonnull JsonWriter writer, @Nonnull JcrPackage jcrPackage,
                              @Nullable Map<String, Object> additionalAttributes)
            throws RepositoryException, IOException {
        writer.beginObject();
        writer.name("definition");
        JcrPackageDefinition definition = jcrPackage.getDefinition();
        toJson(writer, definition);
        JsonUtil.jsonMapEntries(writer, additionalAttributes);
        writer.name("packageid");
        writer.beginObject();
        writer.name("name").value(definition.get("name"));
        writer.name("group").value(definition.get("group"));
        writer.name("version").value(definition.get("version"));
        if (additionalAttributes != null) {
            writer.name("downloadName").value((String) additionalAttributes.get("name"));
        }
        // no "registry" here.
        writer.endObject();
        writer.endObject();
    }

    public static void toJson(JsonWriter writer, JcrPackageDefinition definition)
            throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String version = definition.get(JcrPackageDefinition.PN_VERSION);
        String description = definition.get(JcrPackageDefinition.PN_DESCRIPTION);
        Calendar lastModified = definition.getCalendar(JcrPackageDefinition.PN_LASTMODIFIED);
        Calendar lastUnpacked = definition.getCalendar(JcrPackageDefinition.PN_LAST_UNPACKED);
        boolean includeVersions = definition.getBoolean("includeVersions");
        writer.beginObject();
        writer.name(JcrPackageDefinition.PN_GROUP).value(definition.get(JcrPackageDefinition.PN_GROUP));
        writer.name(JcrPackageDefinition.PN_NAME).value(definition.get(JcrPackageDefinition.PN_NAME));
        if (StringUtils.isNotBlank(version)) {
            writer.name(JcrPackageDefinition.PN_VERSION).value(version);
        }
        if (description != null) {
            writer.name(JcrPackageDefinition.PN_DESCRIPTION).value(description);
        }
        if (lastModified != null) {
            writer.name(JcrPackageDefinition.PN_LASTMODIFIED).value(dateFormat.format(lastModified.getTime()));
        }
        if (lastUnpacked != null) {
            writer.name(JcrPackageDefinition.PN_LAST_UNPACKED).value(dateFormat.format(lastUnpacked.getTime()));
        }
        writer.name("includeVersions").value(includeVersions);
        writer.endObject();
    }

    public static String packageToXMLResponse(JcrPackage jcrPackage) throws RepositoryException {
        final JcrPackageDefinition definition = jcrPackage.getDefinition();
        final String group = definition.get(JcrPackageDefinition.PN_GROUP);
        final String name = definition.get(JcrPackageDefinition.PN_NAME);
        final String version = definition.get(JcrPackageDefinition.PN_VERSION);
        final String filename = getFilename(jcrPackage);
        final long size = jcrPackage.getSize();
        final String createdBy = definition.getCreatedBy();
        final Calendar created = definition.getCreated();
        final Calendar lastModified = definition.getLastModified();
        final String lastModifiedBy = definition.getLastModifiedBy();
        final Calendar lastUnpacked = definition.getLastUnpacked();
        final String lastUnpackedBy = definition.getLastUnpackedBy();
        final SimpleDateFormat dateFormat = new SimpleDateFormat();
        String response =
                "<package>" +
                        "<group>" + group + "</group>" +
                        "<name>" + name + "</name>" +
                        "<version>" + version + "</version>" +
                        "<downloadName>" + filename + "</downloadName>" +
                        "<size>" + size + "</size>" +
                        (created != null ? "<created>" + dateFormat.format(created.getTime()) + "</created>" : "") +
                        "<createdBy>" + createdBy + "</createdBy>" +
                        (lastModified != null ? "<lastModified>" + dateFormat.format(lastModified.getTime()) + "</lastModified>" : "") +
                        "<lastModifiedBy>" + lastModifiedBy + "</lastModifiedBy>" +
                        (lastUnpacked != null ? "<lastUnpacked>" + dateFormat.format(lastUnpacked.getTime()) + "</lastUnpacked>" : "") +
                        "<lastUnpackedBy>" + lastUnpackedBy + "</lastUnpackedBy>" +
                        "</package>";
        return response;
    }

    // Definition properties

    public static String[] getMultiProperty(JcrPackageDefinition pckgDef, String key) {
        String[] result = new String[0];
        try {
            Property property = pckgDef.getNode().getProperty(key);
            Value[] values = property.getValues();
            result = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i].getString();
            }
        } catch (PropertyNotFoundException | PathNotFoundException pnfex) {
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    public interface DefinitionSetter<T> {

        BooleanSetter BOOLEAN = new BooleanSetter();
        StringSetter STRING = new StringSetter();
        MultiStringSetter MULTI_STRING = new MultiStringSetter();

        T get(JsonReader reader) throws IOException;

        void set(JcrPackageDefinition pckgDef, String key, Object value, boolean save)
                throws RepositoryException, ParseException;

        class BooleanSetter implements DefinitionSetter<Boolean> {

            public Boolean get(JsonReader reader) throws IOException {
                Boolean value = true;
                try {
                    value = reader.nextBoolean();
                } catch (IllegalStateException ex) {
                    String string = reader.nextString();
                    if (StringUtils.isNotBlank(string)) {
                        value = Boolean.valueOf(string);
                    }
                }
                return value;
            }

            public void set(JcrPackageDefinition pckgDef, String key, Object value, boolean save) {
                pckgDef.set(key, Boolean.valueOf(value.toString()), save);
            }
        }

        class StringSetter implements DefinitionSetter<String> {

            public String get(JsonReader reader) throws IOException {
                return XSS.filter(reader.nextString());
            }

            public void set(JcrPackageDefinition pckgDef, String key, Object value, boolean save) {
                pckgDef.set(key, value.toString(), save);
            }
        }

        class MultiStringSetter implements DefinitionSetter<String[]> {

            public String[] get(JsonReader reader) throws IOException {
                List<String> set = new ArrayList<>();
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray();
                    while (reader.peek() != JsonToken.END_ARRAY) {
                        set.add(XSS.filter(reader.nextString()));
                    }
                    reader.endArray();
                } else {
                    set.add(XSS.filter(reader.nextString()));
                }
                return set.toArray(new String[set.size()]);
            }

            public void set(JcrPackageDefinition pckgDef, String key, Object value, boolean save)
                    throws RepositoryException {
                Node node = pckgDef.getNode();
                node.setProperty(key, (String[]) value);
                if (save) {
                    node.getSession().save();
                }
            }
        }
    }

    public static final Map<String, DefinitionSetter> DEFINITION_SETTERS;

    static {
        DEFINITION_SETTERS = new HashMap<>();
        DEFINITION_SETTERS.put(DEF_AC_HANDLING, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_DEPENDENCIES, DefinitionSetter.MULTI_STRING);
        DEFINITION_SETTERS.put(DEF_DESCRIPTION, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_DISABLE_INTERMEDIATE_SAVE, DefinitionSetter.BOOLEAN);
        DEFINITION_SETTERS.put(DEF_PROVIDER_LINK, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_PROVIDER_NAME, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_PROVIDER_URL, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_REPLACES, DefinitionSetter.MULTI_STRING);
        DEFINITION_SETTERS.put(DEF_REQUIRES_RESTART, DefinitionSetter.BOOLEAN);
        DEFINITION_SETTERS.put(DEF_REQUIRES_ROOT, DefinitionSetter.BOOLEAN);
        DEFINITION_SETTERS.put(DEF_TESTED_WITH, DefinitionSetter.STRING);
        DEFINITION_SETTERS.put(DEF_INCLUDE_VERSIONS, DefinitionSetter.BOOLEAN);
    }
}
