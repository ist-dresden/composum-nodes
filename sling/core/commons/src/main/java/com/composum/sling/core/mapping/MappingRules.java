package com.composum.sling.core.mapping;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * the set of rules for filtering during the resource hierarchy traversal
 * - uses a DateParser instance which is not thread safe
 * - instances of MappingRules are also not thread safe
 * - use one single instance per thread!
 * TODO: make it configurable via OSGi
 */
public class MappingRules {

    private static final Logger LOG = LoggerFactory.getLogger(MappingRules.class);

    public static final String CONTENT_NODE_FILE_NAME = "_content";

    public enum ContentNodeType {json, xml}

    public static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * the default Date format for output and parsing
     */
    public static final String MAP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * the default Date format for output and parsing
     */
    public static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    /**
     * the set of provided Date formats for parsing from String value
     */
    public static final String[] DATE_PATTERNS = new String[]{
            ECMA_DATE_FORMAT,
            "ISO8601",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "EEE MMM dd yyyy HH:mm:ss z",
            "EEE MMM dd yyyy HH:mm:ss Z",
            "EEE MMM dd yyyy HH:mm:ss X",
            "yyyy-MM-dd HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss Z",
            "yyyy-MM-dd HH:mm:ss X",
            "yyyy-MM-dd'T'HH:mm:ss",
            MAP_DATE_FORMAT,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yy-MM-dd",
            "dd.MM.yyyy HH:mm:ss 'GMT'Z",
            "dd.MM.yyyy HH:mm:ss z",
            "dd.MM.yyyy HH:mm:ss Z",
            "dd.MM.yyyy HH:mm:ss X",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy",
            "dd.MM.yy"
    };

    /**
     * the name for the set of properties in the JSON view to a resource
     */
    public static final Pattern TYPED_PROPERTY_STRING = Pattern.compile("^\\{([A-Za-z]+)\\}(.*)$");

    /**
     * the name for the set of property objects in the external view to a resource
     */
    public static final String PROPERTIES_NAME = "_properties_";

    /**
     * the name for children names array (an optional and additional value
     * to remember the right order of the children)
     */
    public static final String CHILD_ORDER_NAME = "_child_order_";

    /**
     * the default filter for node traversal - system nodes, servlet paths and ACLs are disabled
     */
    public static final ResourceFilter DEFAULT_NODE_FILTER =
            new ResourceFilter.FilterSet(
                    ResourceFilter.FilterSet.Rule.and,
                    new ResourceFilter.NameFilter(new StringFilter.BlackList(
                            "^rep:(repo)?[Pp]olicy$")),
                    new ResourceFilter.PathFilter(new StringFilter.BlackList(
                            "^/system(/.*)$", "^/services(/.*)$", "^/bin(/.*)$"))
            );

    /**
     * the default filter for export and import - system nodes and servlet paths are disabled
     */
    public static final ResourceFilter MAPPING_NODE_FILTER =
            new ResourceFilter.PathFilter(new StringFilter.BlackList(
                    "^/system", "^/services", "^/bin")
            );

    /**
     * the default property name filter to restrict the export - all properties are exported by default
     */
    public static final StringFilter MAPPING_EXPORT_FILTER = StringFilter.ALL;

    /**
     * the default property name filter to restrict the import - creation properties are disabled
     */
    public static final StringFilter MAPPING_IMPORT_FILTER = new StringFilter.BlackList(
            "jcr:created", "jcr:createdBy", "jcr:uuid"
    );

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

    public static class DateParser {

        protected List<SimpleDateFormat> formatList;

        public DateParser() {
            this (Arrays.asList(DATE_PATTERNS));
        }

        public DateParser(Iterable<String> datePatterns) {
            formatList = new ArrayList<>();
            for (String pattern : datePatterns) {
                try {
                    formatList.add(new SimpleDateFormat(pattern));
                } catch (Throwable t) {
                    LOG.warn("Date format '" + pattern + "' igonred (" + t.toString() + ")");
                }
            }
        }

        /**
         * parse string to create a date by trying the configured patterns
         * @param string the date string
         * @return the date if a pattern matches, otherwise 'null'
         */
        public Date parse(String string) {
            Date date = null;
            if (StringUtils.isNotBlank(string)) {
                // try some date patterns...
                for (SimpleDateFormat dateFormat : formatList) {
                    try {
                        date = dateFormat.parse(string);
                        // break after first usable pattern
                        break;
                    } catch (ParseException pex) {
                        // try next...
                    }
                }
            }
            return date;
        }
    }

    //
    // the mapping rules attributes
    //

    /**
     * the filter for the resources in the hierarchy
     */
    public final ResourceFilter resourceFilter;
    /**
     * the filter for the properties (their names) of a resource
     */
    public final StringFilter exportPropertyFilter;
    /**
     * the filter for the properties (their names) of a resource
     */
    public final StringFilter importPropertyFilter;
    /**
     * the format for the properties in the JSON text
     */
    public final PropertyFormat propertyFormat;
    /**
     * the maximum depth for hierarchy traversal (0: infinity traversal)
     */
    public final int maxDepth;
    /**
     * the change policy
     */
    public final ChangeRule changeRule;
    /**
     * the change policy
     */
    public final DateParser dateParser;

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
        this.dateParser = new DateParser();
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
        this.dateParser = new DateParser();
    }

    /**
     * the default rule set for general import an export features
     */
    public static MappingRules getDefaultMappingRules() {
        return new MappingRules(MAPPING_NODE_FILTER, MAPPING_EXPORT_FILTER, MAPPING_IMPORT_FILTER,
                new PropertyFormat(PropertyFormat.Scope.value, PropertyFormat.Binary.base64),
                0, MappingRules.ChangeRule.update);
    }
}
