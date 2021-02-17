package com.composum.sling.core.pckgmgr.regpckg.util;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

public interface RegistryUtil {

    String NO_REGISTRY = "other";
    String NO_GROUP = "nogroup";
    String NO_VERSION = Version.EMPTY.toString();

    Pattern REGISTRY_CLASS_NS = Pattern.compile("^.*\\.(.+)PackageRegistry$");

    List<String> BOOL_TRUE = new ArrayList<String>() {{
        add("true");
        add("on");
    }};

    static String namespace(@Nonnull final PackageRegistry registry) {
        Matcher matcher = REGISTRY_CLASS_NS.matcher(registry.getClass().getName());
        return matcher.matches() ? matcher.group(1).toLowerCase() : NO_REGISTRY;
    }

    @Nonnull
    static String namespace(@Nonnull final String namespaceOrPath) {
        Matcher matcher = Packages.REGISTRY_BASED_PATH.matcher(namespaceOrPath);
        return matcher.matches() ? matcher.group("ns") : namespaceOrPath;
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
        StringBuilder path = new StringBuilder();
        String group = packageId.getGroup();
        String name = packageId.getName();
        String version = packageId.getVersionString();
        if (namespace != null) {
            path.append("/@").append(namespace);
        }
        path.append('/').append(StringUtils.isNotBlank(group) ? group : NO_GROUP)
                .append('/').append(name)
                .append('/').append(StringUtils.isNotBlank(version) ? version : NO_VERSION);
        return path.toString();
    }

    static RegisteredPackage open(@Nonnull final BeanContext context,
                                  @Nullable final String namespace, @Nonnull final PackageId packageId)
            throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        return open(service.getRegistries(context.getResolver()), namespace, packageId);
    }

    static RegisteredPackage open(@Nonnull final BeanContext context,
                                  @Nonnull final String path)
            throws IOException {
        PackageRegistries service = context.getService(PackageRegistries.class);
        return open(service.getRegistries(context.getResolver()), path);
    }

    static RegisteredPackage open(@Nonnull final PackageRegistries.Registries registries,
                                  @Nonnull final String path)
            throws IOException {
        String namespace = RegistryUtil.namespace(path);
        PackageId packageId = RegistryUtil.fromPath(path);
        return open(registries, namespace, packageId);
    }

    static RegisteredPackage open(@Nonnull final PackageRegistries.Registries registries,
                                  @Nullable final String namespace, @Nonnull final PackageId packageId)
            throws IOException {
        PackageRegistry registry = null;
        if (StringUtils.isNotBlank(namespace)) {
            registry = registries.getRegistry(namespace);
        }
        return registry != null ? registry.open(packageId) : registries.open(packageId);
    }

    @Nonnull
    static String requestPath(SlingHttpServletRequest request) {
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
    static String getDownloadURI(@Nullable final PackageRegistry registry, @Nonnull final PackageId id) {
        StringBuilder uri = new StringBuilder("/bin/cpm/package.download.zip");
        uri.append(toPath(registry, id));
        return uri.toString();
    }

    static boolean booleanProperty(@Nonnull final PackageProperties properties,
                                   @Nonnull final String propertyKey, final boolean defaultValue) {
        String value = properties.getProperty(propertyKey);
        return StringUtils.isNotBlank(value)
                ? (BOOL_TRUE.contains(value.toLowerCase()) || Boolean.parseBoolean(value))
                : defaultValue;
    }

    static String date(Calendar calendar) {
        return calendar != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(calendar.getTime()) : null;
    }

    @Nonnull
    static Map<String, Object> properties(@Nonnull final RegisteredPackage pckg) throws IOException {
        Map<String, Object> properties = new PropertyMap();
        PackageProperties pckgProps = pckg.getPackageProperties();
        properties.put(PackageProperties.NAME_CREATED, pckgProps.getCreated());
        properties.put(PackageProperties.NAME_CREATED_BY, pckgProps.getCreatedBy());
        properties.put(PackageProperties.NAME_DESCRIPTION, pckgProps.getDescription());
        properties.put(PackageProperties.NAME_LAST_MODIFIED, pckgProps.getLastModified());
        properties.put(PackageProperties.NAME_LAST_MODIFIED_BY, pckgProps.getLastModifiedBy());
        properties.put(PackageProperties.NAME_LAST_WRAPPED, pckgProps.getLastWrapped());
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

        public <T> T add(@Nonnull final String key, @Nonnull final T value) {
            put(key, value);
            return value;
        }

        public Object adapt(@Nullable Object value) {
            if (value instanceof Calendar) {
                value = date((Calendar) value);
            }
            return value;
        }
    }
}
