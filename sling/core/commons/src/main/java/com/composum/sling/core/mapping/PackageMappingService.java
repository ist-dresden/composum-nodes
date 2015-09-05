package com.composum.sling.core.mapping;

import com.composum.sling.core.util.ResourceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.regex.Pattern;

/**
 *
 */
public abstract class PackageMappingService {

    public static final String RESOURCE_PACKAGE_GROUP = "resources";
    public static final String RESOURCE_PACKAGE_VERSION = "0.0.0-SNAPSHOT";

    public static final String META_INF_DIR_NAME = "META-INF";
    public static final String META_INF_PACKAGE_NAME = "package";
    public static final String DEFAULT_BINARY_EXT = "bin";

    public static final String JSON_FILE_EXT = "json";
    public static final String JSON_OBJECT_EXT = "gson";
    public static final String OBJECT_CLASS_PROPERTY = "class";
    public static final String OBJECT_TYPE_PROPERTY = "type";
    public static final String OBJECT_DATA_PROPERTY = "data";

    public static final String JCR_ROOT_ZIP_NAME = "jcr_root";
    public static final String JCR_ROOT_ZIP_PREFIX = JCR_ROOT_ZIP_NAME + "/";
    public static final String JCR_NAMESPACE_PATTERN = "/([a-z]+):";
    public static final String ZIP_NAMESPACE_PATTERN = "/_([a-z]+)_";

    //
    // export process attributes
    //

    /** the resolver to use for determining resources bby their paths */
    protected ResourceResolver resolver;

    /** the package definition of the 'export' call */
    protected Package servicePackage;

    //
    // Gson setup
    //

    protected static final GsonBuilder GSON_BUILDER =
            Package.registerTypeAdapters(
                    new GsonBuilder());

    protected static final Gson GSON = GSON_BUILDER.create();

    //
    // implementation helpers...
    //

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
}
