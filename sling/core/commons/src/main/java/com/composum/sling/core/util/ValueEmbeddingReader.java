package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

/**
 * replace all '${key}' elements in the stream by their values from the value map;
 * escape '$' and '\' with a prepended '\'
 * - a key '${resource:/a/path/to/resource} is replaced by the content of the resource loaded by the class loader
 * - if a value is a Reader the content of the reader is copied as value; caution a reader object can be read only once
 * - a key can contain a format string (${name;format}); in this case the embedded value is formatted (@see #java.util.Formatter)
 */
public class ValueEmbeddingReader extends Reader {

    public static final String TYPE_RESOURCE = "resource";

    public static final char TYPE_SEPARATOR = ':';
    public static final char FORMAT_SEPARATOR = ';';

    public static final int BUFSIZE = 512;

    public static class Key {

        public final String type;
        public final String name;
        public final String format;

        public Key(String key) {
            int typeSep = key.indexOf(TYPE_SEPARATOR);
            if (typeSep > 0) {
                type = key.substring(0, typeSep);
                key = key.substring(typeSep + 1);
            } else {
                type = null;
            }
            int formatSep = key.indexOf(FORMAT_SEPARATOR);
            if (formatSep > 0) {
                name = key.substring(0, formatSep);
                format = key.substring(formatSep + 1);
            } else {
                name = key;
                format = null;
            }
        }
    }

    protected final Reader reader;
    protected final ValueMap values;
    protected final Locale locale;
    protected final Class resourceContext;

    protected boolean eof = false;
    protected char[] buf = new char[BUFSIZE * 2]; // let place for values (max length:  BUFSIZE)
    protected int off = 0;
    protected int len = 0;

    private transient Reader embed;

    /**
     * @param reader the text to read - probably with embedded value placeholders
     * @param values the set of available placeholders
     */
    public ValueEmbeddingReader(@Nonnull Reader reader, @Nonnull Map<String, Object> values) {
        this(reader, new ValueMapDecorator(values), null, null);
    }

    /**
     * @param reader          the text to read - probably with embedded value placeholders
     * @param values          the set of available placeholders
     * @param locale          the locale to use for value formatting
     * @param resourceContext the context class for resource loading
     */
    public ValueEmbeddingReader(@Nonnull Reader reader, @Nonnull Map<String, Object> values,
                                @Nullable Locale locale, @Nullable Class resourceContext) {
        this(reader, new ValueMapDecorator(values), locale, resourceContext);
    }

    /**
     * @param reader the text to read - probably with embedded value placeholders
     * @param values the set of available placeholders
     */
    public ValueEmbeddingReader(@Nonnull Reader reader, @Nonnull ValueMap values) {
        this(reader, values, null, null);
    }

    /**
     * @param reader          the text to read - probably with embedded value placeholders
     * @param values          the set of available placeholders
     * @param locale          the locale to use for value formatting
     * @param resourceContext the context class for resource loading
     */
    public ValueEmbeddingReader(@Nonnull Reader reader, @Nonnull ValueMap values,
                                @Nullable Locale locale, @Nullable Class resourceContext) {
        this.reader = reader;
        this.values = values;
        this.locale = locale != null ? locale : Locale.getDefault();
        this.resourceContext = resourceContext != null ? resourceContext : values.getClass();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int read(@Nonnull char[] cbuf, int off, int len) throws IOException {
        if (embed != null) {
            if (this.len > 0) { // flush buffer before embedding a reader
                return copy(cbuf, off, len);
            }
            int count = embed.read(cbuf, off, len); // embed a readers content
            if (count >= 0) {
                return count;
            }
            embed.close(); // stop embedding on readers EOF
            embed = null;
        }
        if (this.len < len && !eof) {
            load();
        }
        if (this.len < 1) {
            return -1;
        }
        return copy(cbuf, off, len);
    }

    protected int copy(@Nonnull char[] cbuf, int off, int len) {
        int count = Math.min(this.len, len);
        if (count > 0) {
            System.arraycopy(this.buf, this.off, cbuf, off, count);
            this.off += count;
            this.len -= count;
        }
        return count;
    }

    protected void load() throws IOException {
        if (off > 0) {
            System.arraycopy(buf, off, buf, 0, len);
            off = 0;
        }
        while (!eof && len < BUFSIZE) {
            int token = reader.read();
            if (token < 0) {
                eof = true;
            } else if (token == '\\') { // escaped '$' od '\'?
                int next = reader.read();
                if (next < 0) {
                    buf[len++] = '\\';
                    eof = true;
                } else {
                    if (next == '\\' || next == '$') {
                        buf[len++] = (char) next;
                    } else {
                        buf[len++] = '\\';
                        buf[len++] = (char) next;
                    }
                }
            } else if (token == '$') { // '${...} ?
                int next = reader.read();
                if (next < 0) {
                    buf[len++] = '$';
                    eof = true;
                } else {
                    if (next == '{') {
                        StringBuilder keyBuffer = new StringBuilder();
                        while (!eof && (next = reader.read()) != '}') {
                            if (next < 0) {
                                eof = true;
                            } else {
                                keyBuffer.append((char) next);
                            }
                        }
                        if (!eof) {
                            Key key = new Key(keyBuffer.toString().trim());
                            if (TYPE_RESOURCE.equals(key.type)) {
                                InputStream stream = resourceContext.getResourceAsStream(key.name);
                                if (stream != null) {
                                    // recursive embedding of a resource file with the given value set
                                    embed = new ValueEmbeddingReader(
                                            new InputStreamReader(stream, StandardCharsets.UTF_8), values);
                                    return; // stop buffering up to the end of the embedded reader
                                }
                            } else {
                                Object value = values.get(key.name);
                                if (value instanceof Reader) {
                                    // recursive embedding of a reader object with the given value set
                                    embed = new ValueEmbeddingReader((Reader) value, values);
                                    return; // stop buffering up to the end of the embedded reader
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
                                    string.getChars(0, string.length(), buf, len);
                                    len += string.length();
                                }
                            }
                        } else {
                            buf[len++] = '$';
                            buf[len++] = '{';
                            keyBuffer.getChars(0, keyBuffer.length(), buf, len);
                            len += keyBuffer.length();
                        }
                    } else {
                        buf[len++] = '$';
                        buf[len++] = (char) next;
                    }
                }
            } else {
                buf[len++] = (char) token;
            }
        }
    }
}
