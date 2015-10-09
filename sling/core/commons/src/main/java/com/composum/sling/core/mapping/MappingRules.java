package com.composum.sling.core.mapping;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * the set of rules for filtering during the resource hierarchy traversal
 * TODO: make it configurable via OSGi
 */
public class MappingRules {

    private static final Logger LOG = LoggerFactory.getLogger(MappingRules.class);

    public static final String CONTENT_NODE_FILE_NAME = "_content";

    public enum ContentNodeType {json, xml}

    public static final Charset CHARSET = Charset.forName("UTF-8");

    /** the default Date format for output and parsing */
    public static final SimpleDateFormat MAP_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** the default Date format for output and parsing */
    public static final SimpleDateFormat ECMA_DATE_FORMAT =
            new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");

    /** the set of provided Date formats for parsing from String value */
    public static final SimpleDateFormat[] DATE_PATTERNS = new SimpleDateFormat[]{
            ECMA_DATE_FORMAT,
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'Z"),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),
            MAP_DATE_FORMAT,
            new SimpleDateFormat("yyyy-MM-dd HH:mm"),
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yy-MM-dd"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss 'GMT'Z"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss Z"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm"),
            new SimpleDateFormat("dd.MM.yyyy"),
            new SimpleDateFormat("dd.MM.yy")
    };

    /** the name for the set of properties in the JSON view to a resource */
    public static final Pattern TYPED_PROPERTY_STRING = Pattern.compile("^\\{([A-Za-z]+)\\}(.*)$");

    /** the name for the set of property objects in the external view to a resource */
    public static final String PROPERTIES_NAME = "_properties_";

    /**
     * the name for children names array (an optional and additional value
     * to remember the right order of the children)
     */
    public static final String CHILD_ORDER_NAME = "_child_order_";

    /** the default filter for node traversal - system nodes, servlet paths and ACLs are disabled */
    public static final ResourceFilter DEFAULT_NODE_FILTER =
            new ResourceFilter.FilterSet(
                    ResourceFilter.FilterSet.Rule.and,
                    new ResourceFilter.NameFilter(new StringFilter.BlackList(
                            "^rep:(repo)?[Pp]olicy$")),
                    new ResourceFilter.PathFilter(new StringFilter.BlackList(
                            "^/system(/.*)$", "^/services(/.*)$", "^/bin(/.*)$"))
            );

    /** the default filter for export and import - system nodes and servlet paths are disabled */
    public static final ResourceFilter MAPPING_NODE_FILTER =
            new ResourceFilter.PathFilter(new StringFilter.BlackList(
                    "^/system", "^/services", "^/bin")
            );

    /** the default property name filter to restrict the export - all properties are exported by default */
    public static final StringFilter MAPPING_EXPORT_FILTER = StringFilter.ALL;

    /** the default property name filter to restrict the import - creation properties are disabled */
    public static final StringFilter MAPPING_IMPORT_FILTER = new StringFilter.BlackList(
            "jcr:created", "jcr:createdBy", "jcr:uuid"
    );

    /** the default rule set for general import an export features */
    public static final MappingRules DEFAULT_MAPPING_RULES =
            new MappingRules(MAPPING_NODE_FILTER, MAPPING_EXPORT_FILTER, MAPPING_IMPORT_FILTER,
                    new PropertyFormat(PropertyFormat.Scope.value, PropertyFormat.Binary.base64),
                    0, MappingRules.ChangeRule.update);

    /**
     * the settings set for property transformation
     */
    public static class PropertyFormat {

        /**
         * the scope for the external property representation
         * - value:      use a short name / value - attribute format with type embedded
         * ...           in the string value (a '{type}...' string formatting is used)
         * - object:     use an object for each property with name, type, value and multi attributes
         * - definition: the extended object format with additional definition attributes (auto, protected)
         */
        public enum Scope {
            value, object, definition
        }

        /**
         * the binary transformation rules
         * - skip: don't export or import binary properties
         * - link: build an URL to download the binary content (export only, ignored during import)
         * - base64: embed binary content as base64 encoded string (export) and decode this during import
         */
        public enum Binary {
            skip, link, base64
        }

        public final Scope scope;
        public final Binary binary;

        public PropertyFormat(Scope scope, Binary binary) {
            this.scope = scope != null ? scope : Scope.value;
            this.binary = binary != null ? binary : Binary.base64;
        }
    }

    /**
     * the change policy values
     * - skip:   for packages - skip one aspect (e.g. the ACLs)
     * - extend: add new values only, don't change existing values even if different in th external object
     * - merge:  change all values using the external form and let the values not present external unchanged
     * - update: change all values and remove all values and nodes which are not present in the external object
     */
    public enum ChangeRule {
        skip, extend, merge, update
    }

    //
    // the mapping rules attributes
    //

    /** the filter for the resources in the hierarchy */
    public final ResourceFilter resourceFilter;
    /** the filter for the properties (their names) of a resource */
    public final StringFilter exportPropertyFilter;
    /** the filter for the properties (their names) of a resource */
    public final StringFilter importPropertyFilter;
    /** the format for the properties in the JSON text */
    public final PropertyFormat propertyFormat;
    /** the maximum depth for hierarchy traversal (0: infinity traversal) */
    public final int maxDepth;
    /** the change policy */
    public final ChangeRule changeRule;

    /**
     * Generates a resource traversal filter for generation JSON objects.
     *
     * @param resourceFilter       the filter for the resources in the hierarchy
     * @param exportPropertyFilter the filter for the properties (their names) of a resource
     * @param importPropertyFilter the filter for the properties (their names) of a resource
     * @param maxDepth             the maximum depth for hierarchy traversal (0: infinity traversal)
     */
    public MappingRules(ResourceFilter resourceFilter,
                        StringFilter exportPropertyFilter, StringFilter importPropertyFilter,
                        PropertyFormat propertyFormat, Integer maxDepth, ChangeRule changeRule) {
        this.resourceFilter = resourceFilter;
        this.exportPropertyFilter = exportPropertyFilter;
        this.importPropertyFilter = importPropertyFilter;
        this.propertyFormat = propertyFormat;
        this.maxDepth = maxDepth;
        this.changeRule = changeRule;

    }

    /**
     * the copy constructor to clone and modify rules partially
     *
     * @param template the template with all the default settings
     */
    public MappingRules(MappingRules template, ResourceFilter resourceFilter,
                        StringFilter exportPropertyFilter, StringFilter importPropertyFilter,
                        PropertyFormat propertyFormat, Integer maxDepth, ChangeRule changeRule) {
        this.resourceFilter = resourceFilter != null
                ? resourceFilter : template.resourceFilter;
        this.exportPropertyFilter = exportPropertyFilter != null
                ? exportPropertyFilter : template.exportPropertyFilter;
        this.importPropertyFilter = importPropertyFilter != null
                ? importPropertyFilter : template.importPropertyFilter;
        this.propertyFormat = propertyFormat != null
                ? propertyFormat : template.propertyFormat;
        this.maxDepth = maxDepth != null
                ? maxDepth : template.maxDepth;
        this.changeRule = changeRule != null
                ? changeRule : template.changeRule;
    }
}
