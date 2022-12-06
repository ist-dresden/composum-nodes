package com.composum.sling.core.pckgmgr.regpckg.util;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.core.pckgmgr.Packages.PACKAGE_PATH;
import static com.composum.sling.core.pckgmgr.Packages.REGISTRY_BASED_PATH;
import static com.composum.sling.core.pckgmgr.Packages.REGISTRY_PATH_PREFIX;

public interface RegistryUtil {

    String NO_REGISTRY = "other";
    String NO_GROUP = "nogroup";
    String NO_VERSION = "-";

    Pattern REGISTRY_CLASS_NS = Pattern.compile("^.*\\.(.+)PackageRegistry$");

    List<String> BOOL_TRUE = new ArrayList<String>() {{
        add("true");
        add("on");
    }};

    @Nonnull
    static String namespace(@Nonnull final PackageRegistry registry) {
        Matcher matcher = REGISTRY_CLASS_NS.matcher(registry.getClass().getName());
        return matcher.matches() ? matcher.group(1).toLowerCase() : NO_REGISTRY;
    }

    @Nullable
    static String namespace(@Nonnull final String namespacedPathOrPath) {
        Matcher matcher = Packages.REGISTRY_BASED_PATH.matcher(namespacedPathOrPath);
        return matcher.matches() ? matcher.group("ns") : null;
    }

    /** Returns true if this is a path going to a registry - that is /@{registryname}>/path */
    static boolean isRegistryBasedPath(String path) {
        return StringUtils.isNotBlank(path) && StringUtils.isNotBlank(namespace(path));
    }

    @Nullable
    static String pathWithoutNamespace(@Nullable String fullPath) {
        String path = null;
        if (fullPath != null) {
            Matcher m = REGISTRY_BASED_PATH.matcher(fullPath);
            if (m.matches()) {
                path = m.group("path");
            }
        }
        return path;
    }

    /** Adds the namespace to the path if it isn't already in there. */
    @Nullable
    static String pathWithNamespace(@Nullable String namespace, @Nullable String path) {
        if (StringUtils.isBlank(namespace) || StringUtils.equals(namespace(path), namespace)) {
            return path;
        }
        return "/" + REGISTRY_PATH_PREFIX + namespace + StringUtils.defaultString(path);
    }

    @Nonnull
    static PackageId fromPath(@Nonnull final String path) {
        PackageId id;
        Matcher matcher = PACKAGE_PATH.matcher(path);
        if (matcher.matches()) {
            String namespace = matcher.group("ns");
            String group = matcher.group("group");
            String name = matcher.group("name");
            String version = matcher.group("version");
            id = new PackageId(group == null || NO_GROUP.equals(group) ? "" : group,
                    name,
                    version == null || NO_VERSION.equals(version) ? null : version);
        } else {
            id = new PackageId("", path, "");
        }
        return id;
    }

    @Nonnull
    static String toPath(@Nullable final PackageRegistry registry, @Nonnull final PackageId packageId) {
        return toPath(registry != null ? namespace(registry) : null, packageId);
    }

    @Nonnull
    static String toPath(@Nullable final String namespace, @Nonnull final PackageId packageId) {
        String versionAppendix = StringUtils.isNotBlank(packageId.getVersionString()) ? packageId.getVersionString() : NO_VERSION;
        return toPackagePath(namespace, packageId) + '/' + versionAppendix;
    }

    /** Path for the package, without the version. */
    @Nonnull
    static String toPackagePath(@Nullable final String namespace, @Nonnull final PackageId packageId) {
        StringBuilder path = new StringBuilder();
        String group = packageId.getGroup();
        String name = packageId.getName();
        if (StringUtils.isNotBlank(namespace)) {
            path.append("/@").append(namespace);
        }
        path.append('/').append(StringUtils.isNotBlank(group) ? group : NO_GROUP)
                .append('/').append(name);
        return path.toString();
    }

    @Nullable
    static Pair<String, RegisteredPackage> open(@Nonnull final BeanContext context,
                                                @Nullable final String namespace, @Nonnull final PackageId packageId)
            throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        return open(service.getRegistries(context.getResolver()), namespace, packageId);
    }

    @Nullable
    static Pair<String, RegisteredPackage> open(@Nonnull final BeanContext context,
                                                @Nonnull final String path)
            throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        return open(service.getRegistries(context.getResolver()), path);
    }

    @Nullable
    static Pair<String, RegisteredPackage> open(@Nonnull final PackageRegistries.Registries registries,
                                                @Nonnull final String path)
            throws IOException {
        String namespace = RegistryUtil.namespace(path);
        PackageId packageId = RegistryUtil.fromPath(path);
        return open(registries, namespace, packageId);
    }

    @Nullable
    static Pair<String, RegisteredPackage> open(@Nonnull final PackageRegistries.Registries registries,
                                                @Nullable final String namespace, @Nonnull final PackageId packageId)
            throws IOException {
        PackageRegistry registry = null;
        if (StringUtils.isNotBlank(namespace)) {
            registry = registries.getRegistry(namespace);
        }
        if (registry != null) {
            RegisteredPackage pckg = registry.open(packageId);
            return pckg != null ? Pair.of(namespace, pckg) : null;
        }
        return registries.open(packageId);
    }

    @Nonnull
    static String requestPath(@Nonnull SlingHttpServletRequest request) {
        return PackageUtil.getPath(request);
    }

    @Nonnull
    static String getFilename(@Nonnull final PackageId id) {
        StringBuilder filename = new StringBuilder();
        filename.append(id.getName());
        String version = id.getVersionString();
        if (StringUtils.isNotBlank(version)) {
            filename.append('-').append(version);
        }
        filename.append(".zip");
        return filename.toString();
    }

    @Nonnull
    static String getDownloadURI(@Nullable String namespace, @Nonnull PackageId id) {
        StringBuilder uri = new StringBuilder("/bin/cpm/package.download.zip");
        uri.append(toPath(namespace, id));
        return uri.toString();
    }

    static boolean booleanProperty(@Nonnull final PackageProperties properties,
                                   @Nonnull final String propertyKey, final boolean defaultValue) {
        String value = properties.getProperty(propertyKey);
        return StringUtils.isNotBlank(value)
                ? (BOOL_TRUE.contains(value.toLowerCase()) || Boolean.parseBoolean(value))
                : defaultValue;
    }

    /** Formats a date in ISO8601 format without timezone. */
    @Nullable
    static String date(@Nullable Calendar calendar) {
        return calendar != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(calendar.getTime()) : null;
    }

    public static void toJson(JsonWriter writer, String namespace, PackageId packageId) throws IOException {
        writer.beginObject();
        writer.name("name").value(packageId.getName());
        writer.name("group").value(packageId.getGroup());
        writer.name("version").value(packageId.getVersionString());
        writer.name("downloadName").value((String) packageId.getDownloadName());
        writer.name("registry").value(namespace);
        writer.endObject();
    }
    /**
     * Not for public use, only {@link #readPackagePropertyDate(Calendar, String)}. Parses a weird format com.day.jcr.vault:content-package-maven-plugin produces but that cannot be parsed by {@link ISO8601#parse(String)}, for example 2021-05-26T15:12:21.673+0200 instead of 2021-05-26T15:12:21.673+02:00 , see {@link #format(Calendar, String)}.
     */
    static final Pattern BROKEN_DATEFMT_PATTERN = Pattern.compile("(?<notimezone>.*)(?<timezonestart>[+-][0-9][0-9])(?<timezoneend>[0-9][0-9])");

    /**
     * Workaround for <a href="https://issues.apache.org/jira/browse/JCRVLT-526>JCRVLT-526</a> to read dates from {@link PackageProperties}.
     * Tries to correct for a weird format com.day.jcr.vault:content-package-maven-plugin produces but that cannot be parsed by {@link ISO8601#parse(String)}, for example 2021-05-26T15:12:21.673+0200 instead of 2021-05-26T15:12:21.673+02:00 .
     * Usage e.g. {code}format(packageProps.getLastModified(), packageProps.getProperty(PackageProperties.NAME_LAST_MODIFIED)){code}
     */
    static Calendar readPackagePropertyDate(Calendar rawDate, String dateRep) {
        Calendar date = rawDate;
        if (date == null && StringUtils.isNotBlank(dateRep)) {
            Matcher brokenFmt = BROKEN_DATEFMT_PATTERN.matcher(dateRep);
            if (brokenFmt.matches()) {
                date = ISO8601.parse(brokenFmt.group("notimezone") + brokenFmt.group("timezonestart") + ":" + brokenFmt.group("timezoneend"));
            }
        }
        return date;
    }

    @Nonnull
    static Map<String, Object> properties(@Nonnull final RegisteredPackage pckg) throws IOException {
        Map<String, Object> properties = new PropertyMap();
        PackageProperties pckgProps = pckg.getPackageProperties();
        properties.put(PackageProperties.NAME_CREATED,
                readPackagePropertyDate(pckgProps.getCreated(), pckgProps.getProperty(PackageProperties.NAME_CREATED)));
        properties.put(PackageProperties.NAME_CREATED_BY, pckgProps.getCreatedBy());
        properties.put(PackageProperties.NAME_DESCRIPTION, pckgProps.getDescription());
        properties.put(PackageProperties.NAME_LAST_MODIFIED,
                readPackagePropertyDate(pckgProps.getLastModified(), pckgProps.getProperty(PackageProperties.NAME_LAST_MODIFIED)));
        properties.put(PackageProperties.NAME_LAST_MODIFIED_BY, pckgProps.getLastModifiedBy());
        properties.put(PackageProperties.NAME_LAST_WRAPPED,
                readPackagePropertyDate(pckgProps.getLastWrapped(), pckgProps.getProperty(PackageProperties.NAME_LAST_WRAPPED)));
        properties.put(PackageProperties.NAME_LAST_WRAPPED_BY, pckgProps.getLastWrappedBy());
        properties.put(PackageProperties.NAME_PACKAGE_TYPE, pckgProps.getPackageType());
        properties.put(PackageProperties.NAME_AC_HANDLING, pckgProps.getACHandling());
        properties.put(PackageProperties.NAME_REQUIRES_ROOT, pckgProps.requiresRoot());
        properties.put(PackageProperties.NAME_REQUIRES_RESTART, pckgProps.getProperty(PackageProperties.NAME_REQUIRES_RESTART));
        SubPackageHandling subPckgHdlng = pckgProps.getSubPackageHandling();
        if (subPckgHdlng != null) {
            List<Map<String, Object>> entryList = new ArrayList<>();
            for (SubPackageHandling.Entry entry : subPckgHdlng.getEntries()) {
                Map<String, Object> entryProps = new LinkedHashMap<>();
                entryList.add(entryProps);
                entryProps.put("option", entry.getOption());
                entryProps.put("group", entry.getGroupName());
                entryProps.put("package", entry.getPackageName());
            }
            if (entryList.size() > 0) {
                properties.put(PackageProperties.NAME_SUB_PACKAGE_HANDLING, entryList);
            }
        }
        properties.put(PackageProperties.NAME_USE_BINARY_REFERENCES, pckgProps.getProperty(PackageProperties.NAME_USE_BINARY_REFERENCES));
        return properties;
    }

    class PropertyMap extends LinkedHashMap<String, Object> {

        @Override
        public Object put(@Nonnull final String key, @Nullable final Object value) {
            if (value != null) {
                return super.put(key, adapt(value));
            }
            return null;
        }

        @Nonnull
        public <T> T add(@Nonnull final String key, @Nonnull final T value) {
            put(key, value);
            return value;
        }

        @Nullable
        public Object adapt(@Nullable Object value) {
            if (value instanceof Calendar) {
                value = date((Calendar) value);
            }
            return value;
        }
    }
}
