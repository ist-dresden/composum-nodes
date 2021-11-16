package com.composum.sling.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * a 'Map' which generates its values dynamically to offer a dynamic value set for the scripts
 */
public class CollectingMap<Key, Value> extends HashMap<Key, Value> {

    public interface Factory<Key, Value> {

        @Nullable
        Value value(@NotNull Key key);
    }

    protected final Factory<Key, Value> factory;

    public CollectingMap(@NotNull final Factory<Key, Value> factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(Object key) {
        Value value = super.get(key);
        if (value == null) {
            value = factory.value((Key) key);
            if (value != null) {
                put((Key) key, value);
            }
        }
        return value;
    }
}
