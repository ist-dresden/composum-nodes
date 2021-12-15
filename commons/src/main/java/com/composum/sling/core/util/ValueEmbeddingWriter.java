package com.composum.sling.core.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;

import static com.composum.sling.core.util.ValueEmbeddingReader.TYPE_RESOURCE;

/**
 * a filter writer implementation to embed values from a value map for written placeholders
 */
public class ValueEmbeddingWriter extends FilterWriter {

    interface TokenWriter {

        boolean write(char token) throws IOException;

        void flush() throws IOException;
    }

    protected class TextWriter implements TokenWriter {

        protected boolean escape = false;

        public boolean write(char token) throws IOException {
            switch (token) {
                case '\\':
                    if (escape) {
                        pass('\\');
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                case '$':
                    if (escape) {
                        pass(token);
                        escape = false;
                    } else {
                        writerStack.push(new ValueWriter());
                    }
                    break;
                default:
                    if (escape) {
                        pass('\\');
                        escape = false;
                    }
                    pass(token);
                    break;
            }
            return false;
        }

        public void flush() throws IOException {
            if (escape) {
                pass('\\');
            }
        }
    }

    protected class ValueWriter implements TokenWriter {

        protected StringBuilder keyBuffer = null;

        public boolean write(char token) throws IOException {
            switch (token) {
                case '{':
                    if (keyBuffer == null) {
                        keyBuffer = new StringBuilder();
                    } else if (keyBuffer.length() == 0) {
                        pass("${{");
                        return true;
                    }
                    break;
                case '}':
                    if (keyBuffer == null) {
                        pass("$}");
                    } else if (keyBuffer.length() == 0) {
                        pass("${}");
                    } else {
                        final ValueEmbeddingReader.Key key = new ValueEmbeddingReader.Key(keyBuffer.toString().trim());
                        if (TYPE_RESOURCE.equals(key.type)) {
                            if (resourceContext != null) {
                                try (InputStream stream = resourceContext.getResourceAsStream(key.name);
                                     InputStreamReader reader = stream != null
                                             ? new InputStreamReader(stream, StandardCharsets.UTF_8) : null) {
                                    embed(key, reader);
                                }
                            } else if (keepUnresolvable) {
                                pass("${" + key + "}");
                            }
                        } else {
                            Object value = values.get(key.name);
                            if (value == null && resourceBundle != null) {
                                try {
                                    value = resourceBundle.getString(key.name);
                                } catch (MissingResourceException mrex) {
                                    value = key.name;
                                }
                            }
                            if (value instanceof Reader) {
                                embed(key, (Reader) value);
                            } else if (value != null) {
                                String string;
                                if (StringUtils.isNotBlank(key.format)) {
                                    StringBuilder builder = new StringBuilder();
                                    Formatter formatter = new Formatter(builder);
                                    formatter.format(locale, key.format, value);
                                    string = builder.toString();
                                } else {
                                    string = value.toString();
                                }
                                pass(string);
                            } else if (keepUnresolvable) {
                                pass("${" + key + "}");
                            }
                        }
                    }
                    return true;
                default:
                    if (keyBuffer != null) {
                        keyBuffer.append(token);
                    } else {
                        pass("$" + token);
                        return true;
                    }
                    break;
            }
            return false;
        }

        public void flush() throws IOException {
            if (keyBuffer == null) {
                pass("$");
            } else {
                pass("${" + keyBuffer);
            }
        }

        protected void embed(@NotNull final ValueEmbeddingReader.Key key, @Nullable final Reader reader)
                throws IOException {
            if (reader != null) {
                wrappedWriter.flush();
                IOUtils.copy(new ValueEmbeddingReader(reader, values, locale, resourceContext, resourceBundle),
                        wrappedWriter);
            } else if (keepUnresolvable) {
                pass("${" + key + "}");
            }
        }
    }

    protected final Writer wrappedWriter;
    protected final ValueMap values;
    protected final Locale locale;
    protected final Class<?> resourceContext;
    protected final ResourceBundle resourceBundle;
    protected boolean keepUnresolvable = false;

    protected final Stack<TokenWriter> writerStack = new Stack<>();

    /**
     * Creates a tag filtering reader to filter out the tags configured as sets of tag names
     *
     * @param writer the writer to filter
     * @param values the set of available placeholders
     */
    public ValueEmbeddingWriter(@NotNull final Writer writer, @NotNull Map<String, Object> values) {
        this(writer, values, null, null, null);
    }

    /**
     * Creates a tag filtering reader to filter out the tags configured as sets of tag names
     *
     * @param writer          the writer to filter
     * @param values          the set of available placeholders
     * @param locale          the locale to use for value formatting
     * @param resourceContext the context class for resource loading
     */
    public ValueEmbeddingWriter(@NotNull final Writer writer, @NotNull Map<String, Object> values,
                                @Nullable Locale locale, @Nullable Class<?> resourceContext) {
        this(writer, values, locale, resourceContext, null);
    }

    /**
     * Creates a tag filtering reader to filter out the tags configured as sets of tag names
     *
     * @param writer          the writer to filter
     * @param values          the set of available placeholders
     * @param locale          the locale to use for value formatting
     * @param resourceContext the context class for resource loading
     * @param resourceBundle  the translations bundle (switches translation on)
     */
    public ValueEmbeddingWriter(@NotNull final Writer writer, @NotNull Map<String, Object> values,
                                @Nullable Locale locale, @Nullable Class<?> resourceContext,
                                @Nullable ResourceBundle resourceBundle) {
        super(writer);
        wrappedWriter = writer;
        this.values = values instanceof ValueMap ? ((ValueMap) values) : new ValueMapDecorator(values);
        this.locale = locale != null ? locale : Locale.getDefault();
        this.resourceContext = resourceContext != null ? resourceContext : values.getClass();
        this.resourceBundle = resourceBundle;
        writerStack.push(new TextWriter());
    }

    public boolean isKeepUnresolvable(final boolean... decision) {
        return decision.length > 0 ? (keepUnresolvable = decision[0]) : keepUnresolvable;
    }

    //
    // Writer...
    //

    @Override
    public void flush() throws IOException {
        while (!writerStack.isEmpty()) {
            writerStack.peek().flush();
            writerStack.pop();
        }
        super.flush();
    }

    @Override
    public void write(int token) throws IOException {
        if (writerStack.peek().write((char) token)) {
            writerStack.pop();
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        write(str.toCharArray(), off, len);
    }

    /**
     * uses the single token read() method to fill the buffer
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int count = 0; count < len; count++) {
            write(cbuf[off + count]);
        }
    }

    //
    // final write
    //

    public void pass(String str) throws IOException {
        wrappedWriter.write(str);
    }

    public void pass(String str, int off, int len) throws IOException {
        wrappedWriter.write(str, off, len);
    }

    public void pass(char[] cbuf, int off, int len) throws IOException {
        wrappedWriter.write(cbuf, off, len);
    }

    public void pass(int chr) throws IOException {
        wrappedWriter.write(chr);
    }
}
