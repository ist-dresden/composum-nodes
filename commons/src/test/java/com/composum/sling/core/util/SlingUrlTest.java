package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlingUrlTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Test
    public void slingUrlTest() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getResource(anyString())).thenReturn(null);
        when(resolver.map(any(SlingHttpServletRequest.class), anyString())).thenAnswer(invocation -> {
            SlingHttpServletRequest req = invocation.getArgument(0, SlingHttpServletRequest.class);
            String uri = invocation.getArgument(1, String.class);
            Pattern hostMapping = Pattern.compile("^/x(/.*)?$");
            Matcher matcher = hostMapping.matcher(uri);
            return matcher.matches() ? "http://host.xxx" + matcher.group(1) : req.getContextPath() + uri;
        });
        when(request.getContextPath()).thenReturn("/ctx");
        when(request.getResourceResolver()).thenReturn(resolver);
        when(request.getAttribute(LinkMapper.LINK_MAPPER_REQUEST_ATTRIBUTE)).thenReturn(null);

        SlingUrl url;
        url = new SlingUrl(request, newResource(resolver, "/a/bb/ccc"), "html");
        ec.checkThat(url.getUrl(), equalTo("/ctx/a/bb/ccc.html"));
        url = new SlingUrl(request, newResource(resolver, "/a/bb/ddd"))
                .selectors("m.n").extension("json").suffix("/ddd/eee/xxx.json").parameters("d&c=x%20y");
        ec.checkThat(url.isExternal(), equalTo(false));
        ec.checkThat(url.getUrl(), equalTo("/ctx/a/bb/ddd.m.n.json/ddd/eee/xxx.json?d&c=x%20y"));
        url = new SlingUrl(request, "/ctx/x/bb/ccc/öä ü.s.x.html/x/x/z.html?a=b&c", false);
        ec.checkThat(url.isExternal(), equalTo(false));
        ec.checkThat(url.getUrl(), equalTo("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.x.html/x/x/z.html?a=b&c"));

        url.selector("sel").removeSelector("x")
                .suffix(newResource(resolver, "/c/dd/e#e"))
                .removeParameter("a")
                .parameter("x", "aöü")
                .parameter("Öß", "& 12")
                .parameter("$")
                .fragment("%%$&");
        ec.checkThat(url.isExternal(), equalTo(false));
        ec.checkThat(url.getUrl(), equalTo("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.sel.html/c/dd/e%23e?c&x=a%C3%B6%C3%BC&%C3%96%C3%9F=%26%2012&%24#%25%25%24%26"));

        url = new SlingUrl(request, "https://www.google.com/");
        ec.checkThat(url.isExternal(), equalTo(true));
        ec.checkThat(url.getUrl(), equalTo("https://www.google.com/"));
        url = new SlingUrl(request, "https://www.google.com/ä-@ß$?x=yßz&a#aa");
        ec.checkThat(url.isExternal(), equalTo(true));
        ec.checkThat(url.getUrl(), equalTo("https://www.google.com/ä-@ß$?x=yßz&a#aa"));

        url = new SlingUrl(request, "mailto:ä.user@ö.domain.x");
        ec.checkThat(url.isSpecial(), equalTo(true));
        ec.checkThat(url.getUrl(), equalTo("mailto:ä.user@ö.domain.x"));
        url = new SlingUrl(request, "tel:+01 123 / 3456-78 999");
        ec.checkThat(url.isSpecial(), equalTo(true));
        ec.checkThat(url.getUrl(), equalTo("tel:+01 123 / 3456-78 999"));
    }

    protected Resource newResource(ResourceResolver resolver, String path) {
        String name = StringUtils.substringAfterLast(path, "/");
        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(path);
        when(resource.getName()).thenReturn(name);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(path)).thenReturn(resource);
        return resource;
    }
}
