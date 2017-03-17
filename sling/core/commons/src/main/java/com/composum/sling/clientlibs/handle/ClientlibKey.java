package com.composum.sling.clientlibs.handle;

import java.util.HashMap;
import java.util.Map;

public abstract class ClientlibKey {

    public static final String PROP_REL = "rel";

    public final Clientlib.Type type;
    public final String keyPath;
    public final Map<String, String> properties;

    private transient String key;

    protected ClientlibKey(final Clientlib.Type type, final String keyPath,
                           Map<String, String> properties) {
        this.type = type;
        this.keyPath = Clientlib.getUnminifiedSibling(keyPath);
        this.properties = properties != null ? properties : new HashMap<String, String>();
        String value;
        if ((value = properties.get(PROP_REL)) != null) {
            this.properties.put(PROP_REL, value);
        }
    }

    protected String getKey() {
        if (key == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(type);
            builder.append(':');
            builder.append(keyPath);
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                builder.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }
            key = builder.toString();
        }
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClientlibKey
                && type == ((ClientlibKey) other).type
                && keyPath.equals(((ClientlibKey) other).keyPath)
                && properties.equals(((ClientlibKey) other).properties);
    }

    @Override
    public int hashCode() {
        return keyPath.hashCode() |
                type.hashCode() |
                properties.hashCode();
    }

    @Override
    public String toString() {
        return getKey();
    }
}
