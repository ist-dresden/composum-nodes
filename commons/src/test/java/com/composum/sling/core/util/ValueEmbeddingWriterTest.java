package com.composum.sling.core.util;

import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class ValueEmbeddingWriterTest {

    @Test
    public void testJsonScript() throws Exception {
        String source = "{\"ab${1;=%+8.3f}\":\"xy${xy}z\",\"x\":\"${read}\"}";
        StringWriter result;
        ValueEmbeddingWriter writer;
        writer = new ValueEmbeddingWriter(result = new StringWriter(), new HashMap<String, Object>() {{
            put("1", 22.03f);
            put("xy", "yx");
            put("read", new StringReader("reader content - ${embedded}"));
            put("embedded", new StringReader("!reader embedded in reader!"));
        }}, Locale.GERMAN, getClass());
        writer.write(source);
        writer.flush();
        assertEquals("{\"ab= +22,030\":\"xyyxz\",\"x\":\"reader content - !reader embedded in reader!\"}",
                result.toString());
        writer = new ValueEmbeddingWriter(result = new StringWriter(), new HashMap<String, Object>() {{
            put("xy", "yx");
            put("read", new StringReader("reader content - ${embedded}"));
        }}, Locale.GERMAN, getClass());
        writer.write(source);
        writer.flush();
        assertEquals("{\"ab\":\"xyyxz\",\"x\":\"reader content - \"}", result.toString());
    }

    @Test
    public void testSpecialCases() throws Exception {
        String source = "{\"\\\\a\\${xx}b${1}\":\"xy${xy}z\"}x${ab";
        StringWriter result;
        ValueEmbeddingWriter writer;
        writer = new ValueEmbeddingWriter(result = new StringWriter(), new HashMap<String, Object>() {{
            put("1", "22");
            put("xy", "yx");
        }});
        writer.write(source);
        writer.flush();
        assertEquals("{\"\\a${xx}b22\":\"xyyxz\"}x${ab", result.toString());
    }
}
