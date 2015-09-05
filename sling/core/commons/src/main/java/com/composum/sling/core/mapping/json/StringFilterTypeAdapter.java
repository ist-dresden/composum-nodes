package com.composum.sling.core.mapping.json;

import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.jcr.StringFilterMapping;
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
import java.util.regex.Pattern;

/**
 * The TypeAdapter implementation to write and read StringFilters instances to and from JSON text.
 */
public class StringFilterTypeAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(StringFilterTypeAdapter.class);

    public static GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(StringFilter.FilterSet.class, new FilterSetAdapter());
        builder.registerTypeAdapter(StringFilter.WhiteList.class, new PatternListAdapter());
        builder.registerTypeAdapter(StringFilter.BlackList.class, new PatternListAdapter());
        builder.registerTypeAdapter(StringFilter.class, new GeneralAdapter());
        return builder;
    }

    public static final GsonBuilder GSON_BUILDER = registerTypeAdapters(new GsonBuilder());

    public static final Gson GSON = GSON_BUILDER.create();

    public static class GeneralAdapter extends TypeAdapter<StringFilter> {

        enum JsonValues {type}

        // write

        protected void writeValues(JsonWriter writer, StringFilter value) throws IOException {
            String typeName = StringFilterMapping.getTypeName(value);
            writer.name(JsonValues.type.name()).value(typeName);
        }

        @Override
        public void write(JsonWriter writer, StringFilter value) throws IOException {
            writer.beginObject();
            writeValues(writer, value);
            writer.endObject();
        }

        // read

        protected transient Class<? extends StringFilter> type = null;
        protected transient GeneralAdapter delegate = null;

        protected StringFilter createInstance(Class<? extends StringFilter> type) throws Exception {
            StringFilter result;
            if (StringFilter.All.class.equals(type)) {
                result = StringFilter.ALL;
            } else {
                result = type.newInstance();
            }
            return result;
        }

        protected Object parseValue(JsonReader reader, String name) throws Exception {
            switch (JsonValues.valueOf(name)) {
                case type:
                    this.type = StringFilterMapping.getType(reader.nextString());
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
        public StringFilter read(JsonReader reader) throws IOException {
            StringFilter result = null;
            // reset instance state
            this.type = null;
            this.delegate = null;
            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                String name = reader.nextName();
                try {
                    if (this.delegate != null) {
                        this.delegate.parseValue(reader, name);
                    } else {
                        parseValue(reader, name);
                    }
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    throw new IOException(ex);
                }
            }
            reader.endObject();
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

    public static class PatternListAdapter extends GeneralAdapter {

        enum JsonValues {type, patterns}

        protected transient List<Pattern> patterns = null;

        // write

        @Override
        protected void writeValues(JsonWriter writer, StringFilter value) throws IOException {
            super.writeValues(writer, value);
            writer.name(JsonValues.patterns.name());
            List<Pattern> patterns = ((StringFilter.PatternList) value).getPatterns();
            writer.beginArray();
            for (Pattern pattern : patterns) {
                writer.value(pattern.pattern());
            }
            writer.endArray();
        }

        // read

        @Override
        protected StringFilter createInstance(Class<? extends StringFilter> type) throws Exception {
            StringFilter result;
            if (this.patterns != null) {
                result = type.getConstructor(List.class).newInstance(this.patterns);
            } else {
                result = super.createInstance(type);
            }
            return result;
        }

        @Override
        protected Object parseValue(JsonReader reader, String name) throws Exception {
            switch (JsonValues.valueOf(name)) {
                case patterns:
                    this.patterns = new ArrayList<>();
                    reader.beginArray();
                    while (reader.peek() != JsonToken.END_ARRAY) {
                        this.patterns.add(Pattern.compile(reader.nextString()));
                    }
                    reader.endArray();
                    return this.patterns;
                default:
                    return super.parseValue(reader, name);
            }
        }
    }

    public static class FilterSetAdapter extends GeneralAdapter {

        enum JsonValues {type, rule, set}

        protected transient StringFilter.FilterSet.Rule rule = null;
        protected transient List<StringFilter> set = null;

        // write

        @Override
        protected void writeValues(JsonWriter writer, StringFilter value) throws IOException {
            super.writeValues(writer, value);
            writer.name(JsonValues.rule.name()).value(((StringFilter.FilterSet) value).getRule().name());
            writer.name(JsonValues.set.name());
            List<StringFilter> set = ((StringFilter.FilterSet) value).getSet();
            GSON.toJson(set, set.getClass(), writer);
        }

        // read

        @Override
        protected StringFilter createInstance(Class<? extends StringFilter> type) throws Exception {
            StringFilter result;
            result = type.getConstructor(StringFilter.FilterSet.Rule.class, List.class)
                    .newInstance(this.rule, this.set);
            return result;
        }

        @Override
        protected Object parseValue(JsonReader reader, String name) throws Exception {
            switch (JsonValues.valueOf(name)) {
                case rule:
                    this.rule = StringFilter.FilterSet.Rule.valueOf(reader.nextString());
                    return this.rule;
                case set:
                    Gson gson = registerTypeAdapters(new GsonBuilder()).create();
                    this.set = gson.fromJson(reader, List.class);
                    return this.set;
                default:
                    return super.parseValue(reader, name);
            }
        }
    }
}
