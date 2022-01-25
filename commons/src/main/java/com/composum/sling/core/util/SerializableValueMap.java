package com.composum.sling.core.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SerializableValueMap extends ValueMapDecorator {

    public SerializableValueMap(Map<String, Object> base) {
        super(base instanceof ValueMap ? new HashMap<>(base) : base);
    }
}
