package com.composum.sling.core.mapping.jcr;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rw on 19.05.15.
 */
public class StringFilterMapping {

    private static final Logger LOG = LoggerFactory.getLogger(StringFilterMapping.class);

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_RULE = "rule";
    public static final String PROPERTY_PATTERNS = "patterns";
    public static final String NODE_NAME_ENTRY = "entry";
    public static final String STRING_FILTER_TYPE = "composum/sling/core/filter/strings";

    //
    // String mapping (useful for OSGi configuration properties)
    //

    public static final Pattern FILTER_SET_PATTERN = Pattern.compile(
            "^(and|or|first|last)\\{(.+)\\}$"
    );
    public static final Pattern STRING_PATTERN = Pattern.compile(
            "^(\\+|-|All)('(.*)')?$"
    );
    public static final String DEFAULT_FILTER_TYPE = "+";
    public static final String ALL_FILTER_TYPE = "All";

    public static StringFilter fromString(String rules) {
        StringFilter filter = StringFilter.ALL;
        Matcher matcher = FILTER_SET_PATTERN.matcher(rules);
        if (matcher.matches()) {
            String type = matcher.group(1);
            String values = matcher.group(2);
            try {
                StringFilter.FilterSet.Rule rule = StringFilter.FilterSet.Rule.valueOf(type);
                List<StringFilter> filters = new ArrayList<>();
                String nextRule = "";
                for (String item : StringUtils.split(values, ',')) {
                    nextRule += item;
                    if (StringUtils.isBlank(nextRule) ||
                            STRING_PATTERN.matcher(nextRule).matches() ||
                            FILTER_SET_PATTERN.matcher(nextRule).matches()) {
                        filters.add(fromString(nextRule));
                        nextRule = "";
                    } else {
                        nextRule += ",";
                    }
                }
                filter = new StringFilter.FilterSet(rule, filters);
            } catch (Exception iaex) {
                LOG.error("invalid filter rule: '" + rules + "'");
            }
        } else {
            matcher = STRING_PATTERN.matcher(rules);
            if (matcher.matches()) {
                String type = matcher.group(1);
                if (StringUtils.isBlank(type)) {
                    type = DEFAULT_FILTER_TYPE;
                }
                String values = matcher.group(3);
                try {
                    StringFilter.FilterSet.Rule rule = StringFilter.FilterSet.Rule.valueOf(type);
                    List<StringFilter> filters = new ArrayList<>();
                    String nextRule = "";
                    for (String item : StringUtils.split(values, ',')) {
                        nextRule += item;
                        if (STRING_PATTERN.matcher(nextRule).matches() || StringUtils.isBlank(nextRule)) {
                            filters.add(fromString(nextRule));
                            nextRule = "";
                        } else {
                            nextRule += ",";
                        }
                    }
                    filter = new StringFilter.FilterSet(rule, filters);
                } catch (IllegalArgumentException iaex) {
                    if (ALL_FILTER_TYPE.equals(type)) {
                        filter = StringFilter.ALL;
                    } else if ("-".equals(type)) {
                        filter = new StringFilter.BlackList(values);
                    } else {
                        filter = new StringFilter.WhiteList(values);
                    }
                }
            } else {
                LOG.error("invalid filter rule: '" + rules + "'");
            }
        }
        return filter;
    }

    //
    // general entry point
    //

    public static StringFilter fromResource(Resource resource) throws Exception {
        StringFilter filter = null;
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            String typeName = handle.getProperty(PROPERTY_TYPE);
            Class<? extends StringFilter> type = getType(typeName);
            MappingStrategy strategy = getStrategy(type);
            filter = strategy.fromResource(resource);
        }
        return filter;
    }

    public static void toResource(Resource resource, StringFilter filter) throws RepositoryException {
        if (resource != null) {
            MappingStrategy strategy = getStrategy(filter.getClass());
            strategy.toResource(resource, filter);
        }
    }

    //
    // strategy implementations
    //

    protected static final Map<Class<? extends StringFilter>, MappingStrategy> STRATEGY_MAP;

    static {
        STRATEGY_MAP = new HashMap<>();
        STRATEGY_MAP.put(StringFilter.FilterSet.class, new FilterSetStrategy());
        STRATEGY_MAP.put(StringFilter.WhiteList.class, new PatternFilterStrategy());
        STRATEGY_MAP.put(StringFilter.BlackList.class, new PatternFilterStrategy());
        STRATEGY_MAP.put(StringFilter.All.class, new AllFilterStrategy());
    }

    protected static final MappingStrategy DEFAULT_STRATEGY = new GeneralStrategy();

    public interface MappingStrategy {

        StringFilter fromResource(Resource resource) throws Exception;

        void toResource(Resource resource, StringFilter filter) throws RepositoryException;
    }

    public static class GeneralStrategy implements MappingStrategy {

        protected StringFilter createInstance(ResourceHandle resource,
                                              Class<? extends StringFilter> type)
                throws Exception {
            StringFilter filter = type.newInstance();
            return filter;
        }

        @Override
        public StringFilter fromResource(Resource resource) throws Exception {
            ResourceHandle handle = ResourceHandle.use(resource);
            String typeName = handle.getProperty(PROPERTY_TYPE, (String) null);
            Class<? extends StringFilter> type = getType(typeName);
            StringFilter filter = createInstance(handle, type);
            return filter;
        }

        @Override
        public void toResource(Resource resource, StringFilter filter) throws RepositoryException {
            ResourceHandle handle = ResourceHandle.use(resource);
            handle.setProperty(ResourceUtil.PROP_RESOURCE_TYPE, STRING_FILTER_TYPE);
            handle.setProperty(PROPERTY_TYPE, getTypeName(filter));
        }
    }

    public static class AllFilterStrategy extends GeneralStrategy {

        @Override
        protected StringFilter createInstance(ResourceHandle resource,
                                              Class<? extends StringFilter> type) throws Exception {
            return StringFilter.ALL;
        }
    }

    public static class PatternFilterStrategy extends GeneralStrategy {

        @Override
        protected StringFilter createInstance(ResourceHandle resource,
                                              Class<? extends StringFilter> type)
                throws Exception {
            String[] patternStrings = resource.getProperty(PROPERTY_PATTERNS, String[].class);
            List<Pattern> patterns = new ArrayList<>();
            for (String pattern : patternStrings) {
                patterns.add(Pattern.compile(pattern));
            }
            StringFilter filter = type.getConstructor(List.class).newInstance(patterns);
            return filter;
        }

        @Override
        public void toResource(Resource resource, StringFilter filter) throws RepositoryException {
            super.toResource(resource, filter);
            ResourceHandle handle = ResourceHandle.use(resource);
            List<Pattern> patternList = ((StringFilter.PatternList) filter).getPatterns();
            List<String> valueList = new ArrayList<>();
            for (Pattern pattern : patternList) {
                valueList.add(pattern.pattern());
            }
            handle.setProperty(PROPERTY_PATTERNS, valueList);
        }
    }

    public static class FilterSetStrategy extends GeneralStrategy {

        @Override
        protected StringFilter createInstance(ResourceHandle resource,
                                              Class<? extends StringFilter> type)
                throws Exception {
            StringFilter.FilterSet.Rule rule = StringFilter.FilterSet.Rule.valueOf(
                    resource.getProperty(PROPERTY_RULE, (String) null));
            List<ResourceHandle> filterResources = resource.getChildrenByResourceType(STRING_FILTER_TYPE);
            List<StringFilter> filterList = new ArrayList<>();
            for (ResourceHandle filterRes : filterResources) {
                StringFilter filter = StringFilterMapping.fromResource(filterRes);
                filterList.add(filter);
            }
            StringFilter filter = type.getConstructor(
                    StringFilter.FilterSet.Rule.class, List.class)
                    .newInstance(rule, filterList);
            return filter;
        }

        @Override
        public void toResource(Resource resource, StringFilter filter) throws RepositoryException {
            super.toResource(resource, filter);
            StringFilter.FilterSet filterSet = (StringFilter.FilterSet) filter;
            ResourceHandle handle = ResourceHandle.use(resource);
            StringFilter.FilterSet.Rule rule = filterSet.getRule();
            handle.setProperty(PROPERTY_RULE, rule.name());
            List<StringFilter> set = filterSet.getSet();
            ResourceHandle entry;
            for (int i = 0; i < set.size(); i++) {
                entry = ResourceHandle.use(ResourceUtil.getOrCreateChild(resource,
                        NODE_NAME_ENTRY + "-" + i, ResourceUtil.TYPE_UNSTRUCTURED));
                StringFilterMapping.toResource(entry, set.get(i));
            }
        }
    }

    //
    // type mapping
    //

    public static MappingStrategy getStrategy(Class<? extends StringFilter> type) {
        MappingStrategy strategy = STRATEGY_MAP.get(type);
        return strategy != null ? strategy : DEFAULT_STRATEGY;
    }

    public static final Pattern SIMPLIFY_TYPE_PATTERN =
            Pattern.compile("^" + StringFilter.class.getName() + ".([A-Za-z]+)$");
    public static final Pattern IS_SIMPLIFIED_TYPE_PATTERN = Pattern.compile("^[A-Za-z]+$");

    public static Class<? extends StringFilter> getType(String typeName) throws Exception {
        Class<? extends StringFilter> type;
        if (StringFilterMapping.IS_SIMPLIFIED_TYPE_PATTERN.matcher(typeName).matches()) {
            typeName = StringFilter.class.getName() + "$" + typeName;
        }
        type = (Class<StringFilter>) Class.forName(typeName);
        return type;
    }

    public static String getTypeName(StringFilter value) {
        String typeName = value.getClass().getName();
        Matcher simplifyTypeMatcher = StringFilterMapping.SIMPLIFY_TYPE_PATTERN.matcher(typeName);
        if (simplifyTypeMatcher.matches()) {
            typeName = simplifyTypeMatcher.group(1);
        }
        return typeName;
    }
}
