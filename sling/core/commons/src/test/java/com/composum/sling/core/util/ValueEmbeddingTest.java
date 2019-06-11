package com.composum.sling.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class ValueEmbeddingTest {

    @Test
    public void testJsonScript() throws Exception {
        String source = "{\"ab${1;=%+8.3f}\":\"xy${xy}z\",\"x\":\"${read}\"}";
        ValueEmbeddingReader reader;
        reader = new ValueEmbeddingReader(new StringReader(source), new HashMap<String, Object>() {{
            put("1", 22.03f);
            put("xy", "yx");
            put("read", new StringReader("reader content - ${embedded}"));
            put("embedded", new StringReader(   "!reader embedded in reader!"));
        }}, Locale.GERMAN, getClass());
        assertEquals("{\"ab= +22,030\":\"xyyxz\",\"x\":\"reader content - !reader embedded in reader!\"}",
                IOUtils.toString(reader));
        reader = new ValueEmbeddingReader(new StringReader(source), new HashMap<String, Object>() {{
            put("xy", "yx");
            put("read", new StringReader("reader content - ${embedded}"));
        }}, Locale.GERMAN, getClass());
        assertEquals("{\"ab\":\"xyyxz\",\"x\":\"reader content - \"}", IOUtils.toString(reader));
    }

    @Test
    public void testSpecialCases() throws Exception {
        String source = "{\"\\\\a\\${xx}b${1}\":\"xy${xy}z\"}x${ab";
        String result = "{\"\\a${xx}b22\":\"xyyxz\"}x${ab";
        ValueEmbeddingReader reader = new ValueEmbeddingReader(new StringReader(source),
                new HashMap<String, Object>() {{
                    put("1", "22");
                    put("xy", "yx");
                }});
        assertEquals(result, IOUtils.toString(reader));
    }

    @Test
    public void testJsonWithValues() throws Exception {
        try (
                InputStream stream = getClass().getResourceAsStream("/com/composum/sling/core/util/with-values.json");
                Reader streamReader = new InputStreamReader(stream, UTF_8)
        ) {
            ValueEmbeddingReader reader = new ValueEmbeddingReader(streamReader, new HashMap<String, Object>() {{
                put("segment", "SFTVWRBWRZTRTVRZRRZRTVHBSVERTHVRHZRDTVRDHRDVHHDRTBRHVDZVRHVHZETZVHBZEHETHVBZBH");
                put("base", "AXEFWERGEETZVHTZJHBVZHZHCVETZFHCRWHVZCFGVECHVECHG");
                put("key", "_");
                put("aaa", "_AAA_");
                put("bbb", "_BBB_");
                put("read", new StringReader("--- this is a readers content ---"));
            }});
            String result = IOUtils.toString(reader);
            System.out.println(result);
        }
    }
}
