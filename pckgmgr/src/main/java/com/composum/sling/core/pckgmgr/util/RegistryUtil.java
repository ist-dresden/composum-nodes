package com.composum.sling.core.pckgmgr.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface RegistryUtil {

    String NO_GROUP = "nogroup";
    String NO_VERSION = "noversion";

    Pattern TREE_PATH_PATTERN = Pattern.compile("^/(?<group>(.+))/(?<name>([^/]+))/(?<version>([^/]+))$");

    List<String> BOOL_TRUE = new ArrayList<String>() {{
        add("true");
        add("on");
    }};

    @Nonnull
    static PackageId fromTreePath(@Nonnull final String path) {
        PackageId id;
        Matcher matcher = TREE_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            String group = matcher.group("group");
            String name = matcher.group("name");
            String version = matcher.group("version");
            id = new PackageId(NO_GROUP.equals(group) ? "" : group, name, NO_VERSION.equals(version) ? null : version);
        } else {
            id = new PackageId("", path, "");
        }
        return id;
    }

    @Nonnull
    static String toTreePath(@Nonnull final PackageId id) {
        StringBuilder path = new StringBuilder();
        String group = id.getGroup();
        String name = id.getName();
        String version = id.getVersionString();
        path.append('/').append(StringUtils.isNotBlank(group) ? group : NO_GROUP)
                .append('/').append(name)
                .append('/').append(StringUtils.isNotBlank(version) ? version : NO_VERSION);
        return path.toString();
    }

    @Nonnull
    static String getTreePath(SlingHttpServletRequest request) {
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
    static String getDownloadURI(@Nonnull final PackageId id) {
        StringBuilder uri = new StringBuilder("/bin/cpm/package.download.zip");
        uri.append(toTreePath(id));
        return uri.toString();
    }

    static boolean booleanProperty(@Nonnull final PackageProperties properties,
                                   @Nonnull final String propertyKey, final boolean defaultValue) {
        String value = properties.getProperty(propertyKey);
        return StringUtils.isNotBlank(value)
                ? (BOOL_TRUE.contains(value.toLowerCase()) || Boolean.parseBoolean(value))
                : defaultValue;
    }
}
