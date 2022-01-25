package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A ValueMap which supports nested maps and resolves paths to properties of nested maps.
 * This is useful if you want to use placeholders for a complex set of values,
 * e.g. in a ValueEmbeddingReader.
 * You can use either a path (delimiter: '/') or an object (delimiter: '.') notation,
 * e.g. ${nested/immediate/prop} or ${nested.immediate.prop}.
 * the path notation has precedence over the object notation,
 * e.g. ${nested/prop.name} references the 'prop.name' property of the 'nested' map.
 */
public class StructuredValueMap implements ValueMap {

    protected final ValueMap base;

    public StructuredValueMap(Map<String, Object> base) {
        this.base = base instanceof ValueMap ? (ValueMap) base : new ValueMapDecorator(base);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull String name, @NotNull Class<T> type) {
        return (T) _get(name, type);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull String name, @NotNull T defaultValue) {
        T value = get(name, (Class<T>) defaultValue.getClass());
        return value == null ? defaultValue : value;
    }

    @Override
    public int size() {
        return _size(this.base);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return key != null && _containsKey(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return value != null && _containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return key != null ? _get(key.toString(), null) : null;
    }

    /**
     * puts a new value at the path given by the name;
     * if path starts with '/' the path delimiter is used to build nested maps but ignored in the key
     *
     * @param key   the key (the path) of the value to store
     * @param value the value to store
     * @return the removed value; maybe 'null'
     */
    @Nullable
    @Override
    public Object put(String key, Object value) {
        return key != null ? _put(key, value) : null;
    }

    @Override
    public Object remove(Object key) {
        return key != null ? _remove(key.toString()) : null;
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> m) {
        for (String key : m.keySet()) {
            put(key, m.get(key));
        }
    }

    @Override
    public void clear() {
        this.base.clear();
    }

    /**
     * @return the stripped set of keys including nested keys as paths and excluding the nested maps itself
     */
    @NotNull
    @Override
    public Set<String> keySet() {
        HashSet<String> result = new LinkedHashSet<>();
        _keySet(this.base, result, "");
        return result;
    }

    /**
     * @return the stripped set of values including nested values and excluding the nested maps itself
     */
    @NotNull
    @Override
    public Collection<Object> values() {
        Collection<Object> result = new ArrayList<>();
        _values(this.base, result);
        return result;
    }

    /**
     * @return the entry set of the base map, no nested members stripped
     */
    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return this.base.entrySet();
    }

    // get

    protected Object _get(@NotNull final String name, @Nullable final Class<?> type) {
        return _get(name.startsWith("/") ? name.substring(1) : name,
                type, this.base, name.contains("/") ? "/" : ".");
    }

    @SuppressWarnings("unchecked")
    protected Object _get(@NotNull final String name, @Nullable final Class<?> type,
                          @NotNull final Map<String, Object> map, @NotNull final String delimiter) {
        Object value = _get(map, name, type);
        if (value == null) {
            String[] path = StringUtils.split(name, delimiter, 2);
            if (path.length > 1) {
                Object nested = map.get(path[0]);
                if (nested instanceof Map) {
                    value = _get(path[1], type, (Map<String, Object>) nested, delimiter);
                }
            }
        }
        return value;
    }

    protected Object _get(@NotNull final Map<String, Object> map, @NotNull final String name, @Nullable final Class<?> type) {
        Object value;
        if (type != null) {
            if (map instanceof ValueMap) {
                value = ((ValueMap) map).get(name, type);
            } else {
                value = new ValueMapDecorator(map).get(name, type);
            }
        } else {
            value = map.get(name);
        }
        return value;
    }

    // put

    protected Object _put(@NotNull final String name, @Nullable final Object value) {
        return _put(name.startsWith("/") ? name.substring(1) : name,
                value, this.base, name.contains("/") ? "/" : ".");
    }

    @SuppressWarnings("unchecked")
    protected Object _put(@NotNull final String name, @Nullable final Object value,
                          @NotNull final Map<String, Object> map, @NotNull final String delimiter) {
        if (!map.containsKey(name)) {
            String[] path = StringUtils.split(name, delimiter, 2);
            if (path.length > 1) {
                Object nested = map.get(path[0]);
                if (nested == null) {
                    map.put(path[0], nested = new ValueMapDecorator(new HashMap<>()));
                }
                if (nested instanceof Map) {
                    return _put(path[1], value, (Map<String, Object>) nested, delimiter);
                }
            }
        }
        return map.put(name, value);
    }

    // remove

    protected Object _remove(@NotNull final String name) {
        return _remove(name.startsWith("/") ? name.substring(1) : name,
                this.base, name.contains("/") ? "/" : ".");
    }

    @SuppressWarnings("unchecked")
    protected Object _remove(@NotNull final String name,
                             @NotNull final Map<String, Object> map, @NotNull final String delimiter) {
        if (!map.containsKey(name)) {
            String[] path = StringUtils.split(name, delimiter, 2);
            if (path.length > 1) {
                Object nested = map.get(path[0]);
                if (nested instanceof Map) {
                    return _remove(path[1], (Map<String, Object>) nested, delimiter);
                }
            }
        }
        return map.remove(name);
    }

    // sets

    @SuppressWarnings("unchecked")
    protected void _keySet(Map<String, Object> map, Set<String> result, String prefix) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                _keySet((Map<String, Object>) value, result, prefix + key + "/");
            } else {
                result.add(prefix + key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void _values(Map<String, Object> map, Collection<Object> result) {
        for (Object value : map.values()) {
            if (value instanceof Map) {
                _values((Map<String, Object>) value, result);
            } else {
                result.add(value);
            }
        }
    }

    // other

    @SuppressWarnings("unchecked")
    protected int _size(Map<String, Object> map) {
        int size = map.size();
        for (Object value : map.values()) {
            if (value instanceof Map) {
                size += _size((Map<String, Object>) value) - 1; // count not the map itself
            }
        }
        return size;
    }

    protected boolean _containsKey(@NotNull final String name) {
        return _containsKey(name.startsWith("/") ? name.substring(1) : name,
                this.base, name.contains("/") ? "/" : ".");
    }

    @SuppressWarnings("unchecked")
    protected boolean _containsKey(@NotNull final String name,
                                   @NotNull final Map<String, Object> map, @NotNull final String delimiter) {
        if (map.containsKey(name)) {
            return true;
        }
        String[] path = StringUtils.split(name, delimiter, 2);
        if (path.length > 1) {
            Object nested = map.get(path[0]);
            if (nested instanceof Map) {
                return _containsKey(path[1], (Map<String, Object>) nested, delimiter);
            }
        }
        return false;
    }

    protected boolean _containsValue(@NotNull final Object value) {
        return _containsValue(value, this.base);
    }

    @SuppressWarnings("unchecked")
    protected boolean _containsValue(@NotNull final Object value, @NotNull final Map<String, Object> map) {
        if (map.containsValue(value)) {
            return true;
        }
        for (Object val : map.values()) {
            if (val instanceof Map) {
                if (_containsValue(value, (Map<String, Object>) value)) {
                    return true;
                }
            }
        }
        return false;
    }
}
