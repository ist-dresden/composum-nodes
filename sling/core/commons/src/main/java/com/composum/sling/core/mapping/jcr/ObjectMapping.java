package com.composum.sling.core.mapping.jcr;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rw on 19.05.15.
 */
public class ObjectMapping {

    //
    // resource filter for mapped objects
    //

    public static final ObjectFilter OBJECT_FILTER = new ObjectFilter();

    /**
     * accepts all resources which can be mapped to object by this mapper
     */
    public static class ObjectFilter extends ResourceFilter.AbstractResourceFilter {

        @Override
        public boolean accept(Resource resource) {
            return getStrategy(resource) != null;
        }

        @Override
        public boolean isRestriction() {
            return false;
        }

        @Override
        public void toString(@Nonnull StringBuilder builder) {
            builder.append("Object(...)");
        }
    }

    //
    // general entry point
    //

    public static Object fromResource(Resource resource) throws Exception {
        Object object = null;
        if (resource != null) {
            MappingStrategy strategy = getStrategy(resource);
            if (strategy != null) {
                object = strategy.fromResource(resource);
            }
        }
        return object;
    }

    public static void toResource(Resource resource, Object object) throws RepositoryException {
        if (resource != null && object != null) {
            MappingStrategy strategy = getStrategy(object.getClass());
            if (strategy != null) {
                strategy.toResource(resource, object);
            }
        }
    }

    public static MappingStrategy getStrategy(Resource resource) {
        for (int i = 0; i < FROM_RESOURCE_FILTERS.size(); i++) {
            if (FROM_RESOURCE_FILTERS.get(i).accept(resource)) {
                return FROM_RESOURCE_STRATEGIES.get(i);
            }
        }
        return null;
    }

    public static MappingStrategy getStrategy(Class<?> objectType) {
        return TO_RESOURCE_STRATEGIES.get(objectType);
    }

    //
    // filters
    //

    protected static final ResourceFilter RESOURCE_FILTER_FILTER = new ResourceFilter.ResourceTypeFilter(
            new StringFilter.WhiteList(
                    "^" + ResourceFilterMapping.RESOURCE_FILTER_TYPE + "$"
            ));

    public static class ResourceFilterStrategy implements MappingStrategy<ResourceFilter> {

        @Override
        public ResourceFilter fromResource(Resource resource) throws Exception {
            return ResourceFilterMapping.fromResource(resource);
        }

        @Override
        public void toResource(Resource resource, ResourceFilter filter) throws RepositoryException {
            ResourceFilterMapping.toResource(resource, filter);
        }
    }

    protected static final ResourceFilter STRING_FILTER_FILTER = new ResourceFilter.ResourceTypeFilter(
            new StringFilter.WhiteList(
                    "^" + StringFilterMapping.STRING_FILTER_TYPE + "$"
            ));

    public static class StringFilterStrategy implements MappingStrategy<StringFilter> {

        @Override
        public StringFilter fromResource(Resource resource) throws Exception {
            return StringFilterMapping.fromResource(resource);
        }

        @Override
        public void toResource(Resource resource, StringFilter filter) throws RepositoryException {
            StringFilterMapping.toResource(resource, filter);
        }
    }

    //
    // strategy collections
    //

    public interface MappingStrategy<T> {

        T fromResource(Resource resource) throws Exception;

        void toResource(Resource resource, T object) throws RepositoryException;
    }

    protected static final ResourceFilterStrategy RESOURCE_FILTER_STRATEGY = new ResourceFilterStrategy();
    protected static final StringFilterStrategy STRING_FILTER_STRATEGY = new StringFilterStrategy();

    protected static final List<ResourceFilter> FROM_RESOURCE_FILTERS;
    protected static final List<MappingStrategy> FROM_RESOURCE_STRATEGIES;

    protected static final Map<Class<?>, MappingStrategy> TO_RESOURCE_STRATEGIES;

    static {
        TO_RESOURCE_STRATEGIES = new HashMap<>();
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.FilterSet.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.ResourceTypeFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.NodeTypeFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.PrimaryTypeFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.PathFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.NameFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.FolderFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(ResourceFilter.AllFilter.class, RESOURCE_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(StringFilter.FilterSet.class, STRING_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(StringFilter.WhiteList.class, STRING_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(StringFilter.BlackList.class, STRING_FILTER_STRATEGY);
        TO_RESOURCE_STRATEGIES.put(StringFilter.All.class, STRING_FILTER_STRATEGY);
        FROM_RESOURCE_FILTERS = new ArrayList<>();
        FROM_RESOURCE_STRATEGIES = new ArrayList<>();
        FROM_RESOURCE_FILTERS.add(RESOURCE_FILTER_FILTER);
        FROM_RESOURCE_STRATEGIES.add(RESOURCE_FILTER_STRATEGY);
        FROM_RESOURCE_FILTERS.add(STRING_FILTER_FILTER);
        FROM_RESOURCE_STRATEGIES.add(STRING_FILTER_STRATEGY);
    }
}
