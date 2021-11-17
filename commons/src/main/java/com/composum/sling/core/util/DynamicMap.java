package com.composum.sling.core.util;

import org.jetbrains.annotations.Nullable;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * a 'Map' which generates its values dynamically to offer a dynamic value set for the scripts
 */
public class DynamicMap implements Map<String, String> {

    public interface Source {

        @NotNull
        String issue(@NotNull String key);
    }

    protected final Source source;

    public DynamicMap(@NotNull final Source source) {
        this.source = source;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return true;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public String get(Object key) {
        return source.issue((String) key);
    }

    @Nullable
    @Override
    public String put(String key, String value) {
        return null;
    }

    @Override
    public String remove(Object key) {
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends String> m) {
    }

    @Override
    public void clear() {
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<String> values() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        return Collections.emptySet();
    }
}
