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
}
