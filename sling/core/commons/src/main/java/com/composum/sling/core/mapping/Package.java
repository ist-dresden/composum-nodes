package com.composum.sling.core.mapping;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.mapping.json.ResourceFilterTypeAdapter;
import com.composum.sling.core.util.ResourceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

/**
 * Created by rw on 09.05.15.
 */
public class Package {

    public static final String RESOURCE_TYPE_PACKAGE = "composum/sling/console/package";
    public static final String RESOURCE_TYPE_PACKAGE_PATH = RESOURCE_TYPE_PACKAGE + "/path";

    public static class PackagePath {

        public String path;
        public ResourceFilter filter;
        public MappingRules.ChangeRule changeRule;

        public PackagePath(String path) {
            this(path, ResourceFilter.ALL);
        }

        public PackagePath(String path, ResourceFilter filter) {
            this(path, filter, MappingRules.ChangeRule.update);
        }

        public PackagePath(String path, ResourceFilter filter, MappingRules.ChangeRule changeRule) {
            this();
            this.path = path.trim().replaceAll("^(/.*)/$", "$1"); // remove trailing spaces and ending '/'
            this.filter = filter;
            this.changeRule = changeRule;
        }

        protected PackagePath() {
        }
    }

    public static class PackageOptions {

        public MappingRules.ChangeRule aclPolicy = MappingRules.ChangeRule.merge;

        public PackageOptions() {
        }

        public PackageOptions(MappingRules.ChangeRule aclPolicy) {
            this();
            this.aclPolicy = aclPolicy;
        }
    }

    public static class Thumbnail {

        public transient File file;
        public transient ZipEntry zipEntry;
        public transient Resource resource;
    }

    public String group;
    public String name;
    public String version;

    public Calendar created;
    public Thumbnail thumbnail;

    public PackageOptions options;
    public List<PackagePath> pathList;

    protected transient Resource resource;

    /** the set of mapping rules for the package path elements */
    protected transient Map<String, MappingRules> mappingRules = new HashMap<>();


    public Package(String group, String name, String version,
                   PackageOptions options, PackagePath... pathList) {
        this();
        this.group = group;
        this.name = name;
        this.version = version;
        this.options = options;
        this.pathList = new ArrayList<>();
        Collections.addAll(this.pathList, pathList);
        // sort path list to avoid failures in overlapping paths
        Collections.sort(this.pathList, new Comparator<PackagePath>() {
            @Override
            public int compare(PackagePath path1, PackagePath path2) {
                return path1.path.compareTo(path2.path);
            }
        });
    }

    public Package(String group, String name, String version,
                   PackageOptions options, List<PackagePath> pathList) {
        this();
        this.group = group;
        this.name = name;
        this.version = version;
        this.options = options;
        this.pathList = pathList;
    }

    protected Package() {
    }

    /**
     * Retrieve the MappingRules for one resource using the best matching package path definition.
     *
     * @param resourcePath
     * @return
     */
    public MappingRules getMappingRules(String resourcePath) {
        // use the best matching path in the list to determine the rules
        // this allows overlapping path elements in the package
        Package.PackagePath pkgPath = getMatchingPath(resourcePath, null);
        if (pkgPath != null) {
            MappingRules rules = this.mappingRules.get(pkgPath.path);
            if (rules == null) {
                rules = new MappingRules(MappingRules.getDefaultMappingRules(),
                        pkgPath.filter, null, null, null, null, pkgPath.changeRule);
                this.mappingRules.put(pkgPath.path, rules);
            }
            return rules;
        } else {
            return MappingRules.getDefaultMappingRules();
        }
    }

    /**
     * Retrieves the best matching path in the package definition for one resource path.
     *
     * @param resourcePath
     * @param defaultValue
     * @return
     */
    public Package.PackagePath getMatchingPath(String resourcePath, Package.PackagePath defaultValue) {
        Package.PackagePath result = defaultValue;
        int maxMatchingPathLength = 0;
        for (Package.PackagePath pkgPath : this.pathList) {
            if (resourcePath.startsWith(pkgPath.path)) {
                int pkgPathLength = pkgPath.path.length();
                if (maxMatchingPathLength < pkgPathLength) {
                    maxMatchingPathLength = pkgPathLength;
                    result = pkgPath;
                }
            }
        }
        return result;
    }

    //
    // repository mapping
    //

    public static Package fromResource(Resource resource) throws Exception {
        Package pkg = null;
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            List<ResourceHandle> pkgPathResources = handle.getChildrenByResourceType(RESOURCE_TYPE_PACKAGE_PATH);
            List<PackagePath> pkgPathList = new ArrayList<>();
            for (ResourceHandle pathRes : pkgPathResources) {
                pkgPathList.add(pathFromResource(pathRes));
            }
            pkg = new Package(
                    handle.getProperty("group", (String) null),
                    handle.getProperty("name", (String) null),
                    handle.getProperty("version", (String) null),
                    optionsFromResource(resource.getChild("options")),
                    pkgPathList);
            pkg.resource = resource;
        }
        return pkg;
    }

    public static PackagePath pathFromResource(Resource resource) throws Exception {
        PackagePath pkgPath = null;
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            Resource filter = resource.getChild("filter");
            pkgPath = new PackagePath(
                    handle.getProperty("path", (String) null),
                    ResourceFilterMapping.fromResource(filter),
                    MappingRules.ChangeRule.valueOf(handle.getProperty("changeRule",
                            MappingRules.ChangeRule.update.name())));
        }
        return pkgPath;
    }

    public static PackageOptions optionsFromResource(Resource resource) throws RepositoryException {
        PackageOptions options = null;
        if (resource != null) {
            ValueMap properties = resource.adaptTo(ValueMap.class);
            options = new PackageOptions(
                    MappingRules.ChangeRule.valueOf(properties.get("aclPolicy",
                            MappingRules.ChangeRule.merge.name())));
        }
        return options;
    }

    public static void toResource(Resource packageRoot, String groupPath, Package pkg) throws RepositoryException {
        Resource parent = ResourceUtil.getOrCreateChild(packageRoot, groupPath != null ? groupPath : pkg.group, null);
        Resource resource = ResourceUtil.getOrCreateChild(parent,
                pkg.name + "-" + pkg.version, ResourceUtil.TYPE_UNSTRUCTURED);
        toResource(resource, pkg);
    }

    public static void toResource(Resource resource, Package pkg) throws RepositoryException {
        ResourceHandle handle = ResourceHandle.use(resource);
        handle.setProperty(ResourceUtil.PROP_RESOURCE_TYPE, RESOURCE_TYPE_PACKAGE);
        handle.setProperty("group", pkg.group);
        handle.setProperty("name", pkg.name);
        handle.setProperty("version", pkg.version);
        Resource pathRes;
        for (int i = 0; i < pkg.pathList.size(); i++) {
            pathRes = ResourceUtil.getOrCreateChild(resource, "path-" + i, ResourceUtil.TYPE_UNSTRUCTURED);
            pathToResource(pathRes, pkg.pathList.get(i));
        }
        Resource options = ResourceUtil.getOrCreateChild(resource, "options", ResourceUtil.TYPE_UNSTRUCTURED);
        optionsToResource(options, pkg.options);
    }

    public static void pathToResource(Resource resource, PackagePath pkgPath) throws RepositoryException {
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            handle.setProperty(ResourceUtil.PROP_RESOURCE_TYPE, RESOURCE_TYPE_PACKAGE_PATH);
            handle.setProperty("path", pkgPath.path);
            Resource filter = ResourceUtil.getOrCreateChild(resource, "filter", ResourceUtil.TYPE_UNSTRUCTURED);
            ResourceFilterMapping.toResource(filter, pkgPath.filter);
            handle.setProperty("changeRule", pkgPath.changeRule.name());
        }
    }

    public static void optionsToResource(Resource resource, PackageOptions options) throws RepositoryException {
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            handle.setProperty("aclPolicy", options.aclPolicy.name());
        }
    }

    //
    // JSON mapping
    //

    public static final PackageTypeAdapter PACKAGE_TYPE_ADAPTER = new PackageTypeAdapter();

    public static GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(Package.class, PACKAGE_TYPE_ADAPTER);
        builder = ResourceFilterTypeAdapter.registerTypeAdapters(builder);
        return builder;
    }

    public static final GsonBuilder GSON_BUILDER = registerTypeAdapters(new GsonBuilder());

    public static final Gson GSON = GSON_BUILDER.create();

    public static class PackageTypeAdapter extends TypeAdapter<Package> {

        @Override
        public void write(JsonWriter writer, Package value) throws IOException {
            ResourceFilterTypeAdapter.GSON.toJson(value, Package.class, writer);
        }

        @Override
        public Package read(JsonReader reader) throws IOException {
            Package value = ResourceFilterTypeAdapter.GSON.fromJson(reader, Package.class);
            return value;
        }
    }
}
