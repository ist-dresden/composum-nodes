package com.composum.sling.core.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * replace all '${key}' elements in the stream by their values from the value map; escape '$' and '\' with a prepended '\'
 */
public class ValueEmbeddingReader extends Reader {

    public static final int BUFSIZE = 512;

    protected final Reader reader;
    protected final ValueMap values;

    protected boolean eof = false;
    protected char[] buf = new char[BUFSIZE * 2]; // let place for values (max length:  BUFSIZE)
    protected int off = 0;
    protected int len = 0;

    public ValueEmbeddingReader(Reader reader, Map<String, Object> values) {
        this(reader, new ValueMapDecorator(values));
    }

    public ValueEmbeddingReader(Reader reader, ValueMap values) {
        this.reader = reader;
        this.values = values;
    }

    @Override
    public int read(@Nonnull char[] cbuf, int off, int len) throws IOException {
        if (this.len < len && !eof) {
            load();
        }
        if (this.len < 1) {
            return -1;
        }
        int count = Math.min(this.len, len);
        if (count > 0) {
            System.arraycopy(this.buf, this.off, cbuf, off, count);
            this.off += count;
            this.len -= count;
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        reader.close();
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
                            String value = values.get(key.toString().trim(), String.class);
                            if (value != null) {
                                value.getChars(0, value.length(), buf, len);
                                len += value.length();
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
