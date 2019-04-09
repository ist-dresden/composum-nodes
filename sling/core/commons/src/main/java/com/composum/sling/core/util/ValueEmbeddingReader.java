package com.composum.sling.core.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * replace all '${key}' elements in the stream by their values from the value map;
 * escape '$' and '\' with a prepended '\'
 * - a key '${resource:/a/path/to/resource} is replaced by the content of the resource
 * - if a value is a Reader the content of the reader is copied as value
 */
public class ValueEmbeddingReader extends Reader {

    public static final String TYPE_RESOURE = "resource:";

    public static final int BUFSIZE = 512;

    protected final Reader reader;
    protected final ValueMap values;

    protected boolean eof = false;
    protected char[] buf = new char[BUFSIZE * 2]; // let place for values (max length:  BUFSIZE)
    protected int off = 0;
    protected int len = 0;

    private transient Reader embed;

    public ValueEmbeddingReader(Reader reader, Map<String, Object> values) {
        this(reader, new ValueMapDecorator(values));
    }

    public ValueEmbeddingReader(Reader reader, ValueMap values) {
        this.reader = reader;
        this.values = values;
        this.embed = null;
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
                        StringBuilder key = new StringBuilder();
                        while (!eof && (next = reader.read()) != '}') {
                            if (next < 0) {
                                eof = true;
                            } else {
                                key.append((char) next);
                            }
                        }
                        if (!eof) {
                            String name = key.toString().trim();
                            if (name.startsWith(TYPE_RESOURE)) {
                                InputStream stream = getClass().getResourceAsStream(
                                        name.substring(TYPE_RESOURE.length()));
                                if (stream != null) {
                                    // recursive embedding of a resource file with the given value set
                                    embed = new ValueEmbeddingReader(
                                            new InputStreamReader(stream, StandardCharsets.UTF_8), values);
                                    return; // stop buffering up to the end of the embedded reader
                                }
                            } else {
                                Object value = values.get(name);
                                if (value instanceof Reader) {
                                    // recursive embedding of a reader object with the given value set
                                    embed = new ValueEmbeddingReader((Reader) value, values);
                                    return; // stop buffering up to the end of the embedded reader
                                } else if (value != null) {
                                    String string = value.toString();
                                    string.getChars(0, string.length(), buf, len);
                                    len += string.length();
                                }
                            }
                        } else {
                            buf[len++] = '$';
                            buf[len++] = '{';
                            key.getChars(0, key.length(), buf, len);
                            len += key.length();
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
