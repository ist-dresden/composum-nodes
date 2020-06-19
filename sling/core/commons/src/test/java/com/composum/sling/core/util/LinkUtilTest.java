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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
        ec.checkThat(LinkUtil.getUrl(request, "/content/charsettests/page with+spaces"), is("/content/charsettests/page%20with%2Bspaces"));
    }

    @Test
    public void testCodecs() throws Exception {
        String toencode =
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$+___|\\^`___<>%\"___äöü___abc123";
        ec.checkThat(LinkUtil.encode(toencode), is(
                "/jcr%3A___%20%7E%3F%3D%3B%3A%26%25%23___-_*%40___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"));
        ec.checkThat(LinkUtil.encodePath(toencode), is(
                "/_jcr____%20%7E%3F%3D%3B%3A%26%25%23___-_*%40___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"));
        ec.checkThat(LinkUtil.encodeUrl(toencode), is(
                "/jcr:___%20%7E?=;:&%25#___-_*@___%28%29%7B%7D%5B%5D___%27%21%24%2B___%7C%5C%5E%60___%3C%3E%25%22___%C3%A4%C3%B6%C3%BC___abc123"));

        ec.checkThat(LinkUtil.decode(toencode), is(
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$%2B___|\\^`___<>%\"___äöü___abc123"));
        ec.checkThat(LinkUtil.decodePath(toencode), is(
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$%2B___|\\^`___<>%\"___äöü___abc123"));
        ec.checkThat(LinkUtil.decodeUrl(toencode), is(
                "/jcr:___ ~?=;:&%#___-_*@___(){}[]___'!$%2B___|\\^`___<>%\"___äöü___abc123"));

        String encoded = "%2F%6A%63%72%3A%5F%5F%5F%20%7E%3F%3D%3B%3A%26%25%23%5F%5F%5F%2D%5F%2A%40%5F%5F%5F%28%29%7B%7D%5B%5D%5F%5F%5F%27%21%24%2B%5F%5F%5F%7C%5C%5E%60%5F%5F%5F%3C%3E%25%22%5F%5F%5F%C3%A4%C3%B6%C3%BC%5F%5F%5F%61%62%63%31%32%33";
        ec.checkThat(new UrlCodec("z", StandardCharsets.UTF_8).encodeValidated(toencode), is(encoded));
        ec.checkThat(URLDecoder.decode(encoded, "UTF-8"), is(toencode));

        ec.checkThat(LinkUtil.decode(encoded), is(toencode));
        ec.checkThat(LinkUtil.decodePath(encoded), is(toencode));
        ec.checkThat(LinkUtil.decodeUrl(encoded), is(toencode));
    }
}
