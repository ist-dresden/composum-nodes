package com.composum.sling.core;

import com.composum.sling.core.util.PropertyUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.composum.sling.core.ResourceModel.SERVICE_KEY;

@Restricted(key = SERVICE_KEY)
public class ResourceModel extends AbstractSlingBean {

    public static final String SERVICE_KEY = "nodes/repository/resources";

    private transient ValueMap propertyMap;
    private transient ValueMap values;

    /**
     * initialize bean using the context an the resource given explicitly
     */
    public ResourceModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    /**
     * initialize bean using the context with the 'resource' attribute within
     */
    public ResourceModel(BeanContext context) {
        super(context);
    }

    /**
     * if this constructor is used, the bean must be initialized using the 'initialize' method!
     */
    public ResourceModel() {
    }

    public <T> T getProperty(String name, T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = getProperty(name, type);
        return value != null ? value : defaultValue;
    }

    public <T> T getProperty(String name, Class<T> type) {
        return getValues().get(name, type);
    }

    public ValueMap getValues() {
        if (values == null) {
            values = resource.getValueMap();
        }
        return values;
    }

    // generic property access via generic Map for direct use in templates

    public abstract class GenericMap extends HashMap<String, Object> implements ValueMap {

        public static final String UNDEFINED = "<undefined>";

        @Override
        @Nullable
        public Object get(@NotNull final Object key) {
            return get((String) key, Object.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(@NotNull String name, @NotNull Class<T> type) {
            if (name.startsWith("_jcr_")) {
                name = "jcr:" + name.substring(5);
            }
            Object value = super.get(name);
            if (value == null) {
                value = getValue(name, type);
                super.put(name, value != null ? value : UNDEFINED);
            }
            return value != UNDEFINED ? (T) value : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NotNull
        public <Type> Type get(@NotNull String name, @NotNull Type defaultValue) {
            Type value = (Type) get(name, defaultValue.getClass());
            return value != null ? value : defaultValue;
        }

        protected abstract <T> T getValue(String key, @NotNull Class<T> type);
    }

    public class GenericProperty extends GenericMap {

        @Override
        @Nullable
        public <T> T getValue(String key, @NotNull Class<T> type) {
            return getProperty(key, type);
        }
    }

    @NotNull
    public ValueMap getProperty() {
        if (propertyMap == null) {
            propertyMap = new GenericProperty();
        }
        return propertyMap;
    }
}
