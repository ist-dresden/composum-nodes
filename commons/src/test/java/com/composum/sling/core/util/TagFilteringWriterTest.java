package com.composum.sling.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class TagFilteringWriterTest {

    public static final String TEST_ROOT = "/com/composum/sling/core/util/filter/";

    protected class TagFilteringTestWriter extends TagFilteringWriter {
        public TagFilteringTestWriter(Writer writer) {
            super(writer, new String[]{"html:div"}, new String[]{"body"}, DEFAULT_TO_DROP, DEFAULT_TO_CLOSE);
        }
    }

    @Test
    public void case_00() throws Exception {
        StringWriter result;
        IOUtils.copy(new StringReader("<html>body</html>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<div>body</div>", result.toString());
        IOUtils.copy(new StringReader("<p class=\"class\">paragraph</p>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<p class=\"class\">paragraph</p>", result.toString());
        IOUtils.copy(new StringReader("<p data-x=\"<body>embedded</body>\">paragraph</p>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<p data-x=\"embedded\">paragraph</p>", result.toString());
        IOUtils.copy(new StringReader("<div><span><input type=\"text\" value=\"\"></span></div>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<div><span><input type=\"text\" value=\"\"></span></div>", result.toString());
        IOUtils.copy(new StringReader("<head>head</head>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("", result.toString());
        IOUtils.copy(new StringReader("<html data-x=\"x\"><head>head</head><body>body</body></html>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<div data-x=\"x\">body</div>", result.toString());
        IOUtils.copy(new StringReader("<html><head>head<style type=\"test\">style</style></head><body class=\"class\" data-x=\"data\"><p>body</p></body></html>"), new TagFilteringTestWriter(result = new StringWriter()));
        assertEquals("<div><p>body</p></div>", result.toString());
    }

    @Test
    public void case_01() throws Exception {
        StringWriter result = new StringWriter();
        TagFilteringTestWriter writer = new TagFilteringTestWriter(result);
        IOUtils.copy(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                TEST_ROOT + "case-01-input.html")), StandardCharsets.UTF_8), writer);
        writer.flush();
        assertEquals(IOUtils.toString(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                        TEST_ROOT + "case-01-result.html")), StandardCharsets.UTF_8)), result.toString());
    }

    @Test
    public void case_02() throws Exception {
        StringWriter result = new StringWriter();
        TagFilteringTestWriter writer = new TagFilteringTestWriter(result);
        IOUtils.copy(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                TEST_ROOT + "case-02-input.html")), StandardCharsets.UTF_8), writer);
        writer.flush();
        assertEquals(IOUtils.toString(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                        TEST_ROOT + "case-02-result.html")), StandardCharsets.UTF_8)), result.toString());
    }
}
