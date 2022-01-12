package com.composum.sling.core.proxy;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link LazyInputStream}.
 */
public class GenericProxyReaderTest {

    public static final String TEST_ROOT = "/com/composum/sling/core/proxy/filter/";

    protected class GenericProxyTestReader extends GenericProxyReader {
        public GenericProxyTestReader(Reader reader) {
            super(reader, new String[]{"html:div"}, new String[]{"body"}, DEFAULT_TO_DROP);
        }
    }

    @Test
    public void case_00() throws Exception {
        assertEquals("<div>body</div>", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<html>body</html>"))));
        assertEquals("<p class=\"class\">paragraph</p>", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<p class=\"class\">paragraph</p>"))));
        assertEquals("<p data-x=\"embedded\">paragraph</p>", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<p data-x=\"<body>embedded</body>\">paragraph</p>"))));
        assertEquals("<input type=\"text\" value=\"\" />", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<input type=\"text\" value=\"\" />"))));
        assertEquals("", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<head>head</head>"))));
        assertEquals("<div data-x=\"x\">body</div>", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<html data-x=\"x\"><head>head</head><body>body</body></html>"))));
        assertEquals("<div><p>body</p></div>", IOUtils.toString(new GenericProxyTestReader(
                new StringReader("<html><head>head<style type=\"test\">style</style></head><body class=\"class\" data-x=\"data\"><p>body</p></body></html>"))));
    }

    @Test
    public void case_01() throws Exception {
        GenericProxyReader filterReader = new GenericProxyTestReader(
                new InputStreamReader(getClass().getResourceAsStream(
                        TEST_ROOT + "case-01-input.html"), StandardCharsets.UTF_8));
        String result = IOUtils.toString(filterReader);
        assertEquals(IOUtils.toString(new InputStreamReader(getClass().getResourceAsStream(
                TEST_ROOT + "case-01-result.html"), StandardCharsets.UTF_8)), result);
    }

    @Test
    public void case_02() throws Exception {
        GenericProxyReader filterReader = new GenericProxyTestReader(
                new InputStreamReader(getClass().getResourceAsStream(
                        TEST_ROOT + "case-02-input.html"), StandardCharsets.UTF_8));
        String result = IOUtils.toString(filterReader);
        assertEquals(IOUtils.toString(new InputStreamReader(getClass().getResourceAsStream(
                TEST_ROOT + "case-02-result.html"), StandardCharsets.UTF_8)), result);
    }
}
