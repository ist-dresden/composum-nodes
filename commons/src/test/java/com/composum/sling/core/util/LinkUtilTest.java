package com.composum.sling.core.util;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LinkUtilTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Mock
    protected SlingHttpServletRequest request;

    @Mock
    protected ResourceResolver resolver = Mockito.mock(ResourceResolver.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(request.getResourceResolver()).thenReturn(resolver);
        when(resolver.map(any(), anyString())).thenAnswer(
                (invocation) -> LinkUtil.encodePath(invocation.getArgument(1))
        );
    }

    @Test
    public void codecTest() throws Exception {
        LinkCodec codec = new LinkCodec();
        String input, output;
        ec.checkThat(output = codec.encode(
                input = "/a/bb/ccc/d ef?äö=#+*:.;,_-$%&"),
                equalTo("/a/bb/ccc/d%20ef%3F%C3%A4%C3%B6%3D%23%2B*%3A.%3B%2C_-%24%25%26"));
        ec.checkThat(codec.decode(output), equalTo(input));
        ec.checkThat(output = codec.encodeUrl(
                input = "/a/bb/ccc/d ef?äö=#+*:.;,_-$%&"),
                equalTo("/a/bb/ccc/d%20ef?%C3%A4%C3%B6=#%2B*:.;%2C_-%24%25&"));
        ec.checkThat(codec.decode(output), equalTo(input));
    }

    @Test
    public void testPathMapping() {
        ec.checkThat(LinkUtil.getUrl(request, "/content/charsettests/page with+spaces"), is("/content/charsettests/page%20with+spaces"));
    }

    @Test
    public void testNamespacePrefixUnEscaping() {
        for (String untouched : Arrays.asList(null, "", "/", "//", "/bla", "bla/", "/bla/", "a/b//c",
                "test_image.jpg", "_testimage.jpg", ":foo", ":foo:foo", "_blo:foo")) {
            namespacePrefixEscapeCheck(untouched, untouched);
        }
        namespacePrefixEscapeCheck("jcr:content", "_jcr_content");
        namespacePrefixEscapeCheck("_un_der", "__un_der");
        namespacePrefixEscapeCheck("cq:test:image.jpg", "_cq_test:image.jpg");
        namespacePrefixEscapeCheck("cq:content/a/jcr:content/b/_un_der", "_cq_content/a/_jcr_content/b/__un_der");
    }

    private void namespacePrefixEscapeCheck(String path, String escapedPath) {
        ec.checkThat(path, LinkUtil.namespacePrefixEscape(path), is(escapedPath));
        ec.checkThat(path, LinkUtil.namespacePrefixUnescape(escapedPath), is(path));
    }

    @Test
    public void testCodecs() throws Exception {
        String toencode =
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$+___|\\^`___<>%\"___äöü___abc123";
        ec.checkThat(LinkUtil.encode(toencode), is(
                "/jcr:___%20%7E%3F=%3B:&%25%23___-_*@___()%7B%7D%5B%5D___'!$+___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // orig LinkCodec: "/jcr%3A___%20%7E%3F%3D%3B%3A%26%25%23___-_*%40___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // -> not encoded anymore =:&@()!'!$+
        ));
        ec.checkThat(LinkUtil.encodePath(toencode), is(
                "/_jcr____%20%7E%3F=%3B:&%25%23___-_*@___()%7B%7D%5B%5D___'!$+___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                //        "/_jcr____%20%7E%3F%3D%3B%3A%26%25%23___-_*%40___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // originally : /jcr%3A___%2B... : no replacement of _jcr_, space -> +
        ));
        ec.checkThat(LinkUtil.encodeUrl(request, toencode), is(
                // this test is no longer meaningful since that does a real URL parsing and rebuilding. That's the reason the = vanishes.
                "/_jcr____%20%7E?;:&%25#___-_*@___()%7B%7D%5B%5D___'!$+___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // orig LinkCodec: "/jcr:___%20%7E?=;:&%25#___-_*@___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // -> @ encoded, but ()'!$+ not.
                // FIXME(hps,19.06.20) where is the =???
                // originally: "/jcr%3A___+%7E%3F%3D%3B%3A%26%25%23___-_*%40___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"
                // + instead of space, encodes :?=;:&#@
        ));

        ec.checkThat(LinkUtil.decode(toencode), is(
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$+___|\\^`___<>%\"___äöü___abc123"
                // orig slingurl: did not decode +
        ));
        ec.checkThat(LinkUtil.decodePath(toencode), is(
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$+___|\\^`___<>%\"___äöü___abc123"
                // orig slingurl: did not decode +
        ));
//        ec.checkThat(LinkUtil.decodeUrl(request, toencode), is(
//                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$%2B___|\\^`___<>%\"___äöü___abc123"));
        // originally: "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$+___|\^`___<>%\"___äöü___abc123" : decodes +

        String encoded = "%2F%6A%63%72%3A%5F%5F%5F%20%7E%3F%3D%3B%3A%26%25%23%5F%5F%5F%2D%5F%2A%40%5F%5F%5F%28%29%7B%7D%5B%5D%5F%5F%5F%27%21%24%2B%5F%5F%5F%7C%5C%5E%60%5F%5F%5F%3C%3E%25%22%5F%5F%5F%C3%A4%C3%B6%C3%BC%5F%5F%5F%61%62%63%31%32%33";
        ec.checkThat(new UrlCodec("z", StandardCharsets.UTF_8).encodeValidated(toencode), is(encoded));
        ec.checkThat(URLDecoder.decode(encoded, "UTF-8"), is(toencode));

        ec.checkThat(LinkUtil.decode(encoded), is(toencode));
        // originally: "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$ ___|\^`___<>%\"___äöü___abc123" : decoded + to space (wrong!)
        ec.checkThat(LinkUtil.decodePath(encoded), is(toencode));
//        ec.checkThat(LinkUtil.decodeUrl(request, encoded), is(toencode));
    }
}
