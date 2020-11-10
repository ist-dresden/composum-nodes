package com.composum.sling.core.mapping.json;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A ResourceFilter is useful to describe a general way to define scopes in resource hierarchy.
 * Such a filter accepts only resources which properties are matching to filter patterns.
 * These filters can be combined in filter sets with various combination rules.
 */

/**
 * The TypeAdapter implementation to write and read StringFilters instances to and from JSON text.
 */
public class ResourceFilterTypeAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(StringFilterTypeAdapter.class);

    public static GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(ResourceFilter.FilterSet.class, new FilterSetAdapter());
        builder.registerTypeAdapter(ResourceFilter.TypeFilter.class, new TypeFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.NameFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.PathFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.PrimaryTypeFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.NodeTypeFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.ResourceTypeFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.MimeTypeFilter.class, new PatternFilterAdapter());
        builder.registerTypeAdapter(ResourceFilter.FolderFilter.class, new PredefinedFilterAdapter(ResourceFilter.FOLDER));
        builder.registerTypeAdapter(ResourceFilter.AllFilter.class, new PredefinedFilterAdapter(ResourceFilter.ALL));
        builder.registerTypeAdapter(ResourceFilter.class, new GeneralAdapter());
        builder = StringFilterTypeAdapter.registerTypeAdapters(builder);
        return builder;
    }

    public static final GsonBuilder GSON_BUILDER = registerTypeAdapters(new GsonBuilder());

    public static final Gson GSON = GSON_BUILDER.create();

    public static class GeneralAdapter extends TypeAdapter<ResourceFilter> {

        enum JsonValues {type, filter}

        // write

        protected void writeValues(JsonWriter writer, ResourceFilter value) throws IOException {
            String typeName = ResourceFilterMapping.getTypeName(value);
            writer.name(JsonValues.type.name()).value(typeName);
        }

        @Override
        public void write(JsonWriter writer, ResourceFilter value) throws IOException {
            writer.beginObject();
            writeValues(writer, value);
            writer.endObject();
        }

        // read

        protected transient Class<? extends ResourceFilter> type = null;
        protected transient GeneralAdapter delegate = null;

        protected ResourceFilter createInstance(Class<? extends ResourceFilter> type) throws Exception {
            ResourceFilter result;
            result = type.newInstance();
            return result;
        }

        protected Object parseValue(JsonReader reader, String name) throws IOException {
            switch (JsonValues.valueOf(name)) {
                case type:
                    try {
                        this.type = ResourceFilterMapping.getType(reader.nextString());
                    } catch (Exception ex) {
                        LOG.error(ex.getMessage(), ex);
                        throw new IOException(ex);
                    }
                    if (this.type != null) {
                        TypeAdapter adapter = GSON.getAdapter(this.type);
                        if (adapter instanceof GeneralAdapter) {
                            this.delegate = (GeneralAdapter) adapter;
                        }
                    }
                    return this.type;
            }
            return null;
        }

        @Override
        public ResourceFilter read(JsonReader reader) throws IOException {
            ResourceFilter result = null;
            // reset instance state
            this.type = null;
            this.delegate = null;
            reader.beginObject();
            // parse all JSON values...
            while (reader.peek() != JsonToken.END_OBJECT) {
                String name = reader.nextName();
                if (this.delegate != null) {
                    this.delegate.parseValue(reader, name);
                } else {
                    parseValue(reader, name);
                }
            }
            reader.endObject();
            // create instance
            if (this.type != null) {
                try {
                    if (this.delegate != null) {
                        result = this.delegate.createInstance(this.type);
                    } else {
                        result = createInstance(this.type);
                    }
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    throw new IOException(ex.toString());
                }
            }
            return result;
        }
    }

    public static class PredefinedFilterAdapter extends GeneralAdapter {

        private final ResourceFilter instance;

        public PredefinedFilterAdapter(ResourceFilter instance) {
            this.instance = instance;
        }

        // read

        @Override
        protected ResourceFilter createInstance(Class<? extends ResourceFilter> type) throws Exception {
            return this.instance;
        }
    }

    public static class TypeFilterAdapter extends GeneralAdapter {

        enum JsonValues {type, filter, restriction}

        protected transient String filter = null;

        // write

        @Override
        protected void writeValues(JsonWriter writer, ResourceFilter value) throws IOException {
            ResourceFilter.TypeFilter typeFilter = (ResourceFilter.TypeFilter) value;
            super.writeValues(writer, value);
            writer.name(JsonValues.filter.name());
            StringBuilder buf = new StringBuilder();
            typeFilter.typeNamesToString(buf);
            writer.value(buf.toString());
        }

        // read

        @Override
        protected ResourceFilter createInstance(Class<? extends ResourceFilter> type) throws Exception {
            ResourceFilter result;
            if (this.filter != null) {
                result = type.getConstructor(String.class).newInstance(this.filter);
            } else {
                result = super.createInstance(type);
            }
            return result;
        }

        @Override
        protected Object parseValue(JsonReader reader, String name) throws IOException {
            switch (JsonValues.valueOf(name)) {
                case filter:
                    this.filter = reader.nextString();
                    return this.filter;
                default:
                    return super.parseValue(reader, name);
            }
        }
    }

    public static class PatternFilterAdapter extends GeneralAdapter {

        enum JsonValues {type, filter}

        protected transient StringFilter filter = null;

        // write

        @Override
        protected void writeValues(JsonWriter writer, ResourceFilter value) throws IOException {
            super.writeValues(writer, value);
            writer.name(JsonValues.filter.name());
            StringFilter filter = ((ResourceFilter.PatternFilter) value).getFilter();
            GSON.toJson(filter, filter.getClass(), writer);
        }

        // read

        @Override
        protected ResourceFilter createInstance(Class<? extends ResourceFilter> type) throws Exception {
            ResourceFilter result;
            if (this.filter != null) {
                result = type.getConstructor(StringFilter.class).newInstance(this.filter);
            } else {
                result = super.createInstance(type);
            }
            return result;
        }

        @Override
        protected Object parseValue(JsonReader reader, String name) throws IOException {
            switch (JsonValues.valueOf(name)) {
                case filter:
                    this.filter = GSON.fromJson(reader, StringFilter.class);
                    return this.filter;
                default:
                    return super.parseValue(reader, name);
            }
        }
    }

    public static class FilterSetAdapter extends GeneralAdapter {

        enum JsonValues {type, rule, set}

        protected transient ResourceFilter.FilterSet.Rule rule = null;
        protected transient List<ResourceFilter> set = null;

        // write

        @Override
        protected void writeValues(JsonWriter writer, ResourceFilter value) throws IOException {
            super.writeValues(writer, value);
            writer.name(JsonValues.rule.name()).value(((ResourceFilter.FilterSet) value).getRule().name());
            writer.name(JsonValues.set.name());
            List<ResourceFilter> set = ((ResourceFilter.FilterSet) value).getSet();
            GSON.toJson(set, set.getClass(), writer);
        }

        // read

        @Override
        protected ResourceFilter createInstance(Class<? extends ResourceFilter> type) throws Exception {
            ResourceFilter result;
            result = type.getConstructor(ResourceFilter.FilterSet.Rule.class, List.class)
                    .newInstance(this.rule, this.set);
            return result;
        }

        @Override
        protected Object parseValue(JsonReader reader, String name) throws IOException {
            switch (JsonValues.valueOf(name)) {
                case rule:
                    this.rule = ResourceFilter.FilterSet.Rule.valueOf(reader.nextString());
                    return this.rule;
                case set:
                    this.set = new ArrayList<>();
                    Gson gson = registerTypeAdapters(new GsonBuilder()).create();
                    reader.beginArray();
                    while (reader.peek() != JsonToken.END_ARRAY) {
                        ResourceFilter filter = gson.fromJson(reader, ResourceFilter.class);
                        this.set.add(filter);
                    }
                    reader.endArray();
                    return this.set;
                default:
                    return super.parseValue(reader, name);
            }
        }
    }
}
