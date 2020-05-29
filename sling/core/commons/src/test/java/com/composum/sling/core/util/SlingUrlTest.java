package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlingUrlTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected SlingHttpServletRequest request;
    protected ResourceResolver resolver;

    @Before
    public void setup() {
        request = mock(SlingHttpServletRequest.class);
        resolver = mock(ResourceResolver.class);
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
    }

    @Test
    public void slingUrlTest() {
        SlingUrl url;
        url = new SlingUrl(request, newResource(resolver, "/a/bb/ccc"), "html");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("/a/bb/ccc"));
        ec.checkThat(url.getResourcePath(), is("/a/bb/ccc"));
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/ccc.html"));

        url = new SlingUrl(request, newResource(resolver, "/a/bb/ddd"))
                .selectors("m.n").extension("json").suffix("/ddd/eee/xxx.json").parameters("d&c=x%20y");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("json"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("/a/bb/ddd"));
        ec.checkThat(url.getResourcePath(), is("/a/bb/ddd"));
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), is("/ddd/eee/xxx.json"));
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/ddd.m.n.json/ddd/eee/xxx.json?d&c=x%20y"));

        url = new SlingUrl(request, "/ctx/x/bb/ccc/öä ü.s.x.html/x/x/z.html?a=b&c", false);
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getResourcePath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), is("/x/x/z.html"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.x.html/x/x/z.html?a=b&c"));

        url.selector("sel").removeSelector("x")
                .suffix(newResource(resolver, "/c/dd/e#e"))
                .removeParameter("a")
                .parameter("x", "aöü")
                .parameter("Öß", "& 12")
                .parameter("$")
                .fragment("%%$&");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), is("%%$&"));
        ec.checkThat(url.getPath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getResourcePath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), is("/c/dd/e#e"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.sel.html/c/dd/e%23e?c&x=a%C3%B6%C3%BC&%C3%96%C3%9F=%26%2012&%24#%25%25%24%26"));

        url = new SlingUrl(request, "https://www.google.com/");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("/"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("https://www.google.com/"));

        url = new SlingUrl(request, "https://www.google.com/ä-@ß$?x=yßz&a#aa");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), is("aa"));
        ec.checkThat(url.getPath(), is("/ä-@ß$"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.isSpecial(), is(false));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("https://www.google.com/ä-@ß$?x=yßz&a#aa"));

        url = new SlingUrl(request, "mailto:ä.user@ö.domain.x");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("nullä.user@ö.domain.x"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.isSpecial(), is(true));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("mailto:ä.user@ö.domain.x"));

        url = new SlingUrl(request, "tel:+01 123 / 3456-78 999");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPath(), is("null+01 123 / 3456-78 999"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.isSpecial(), is(true));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("tel:+01 123 / 3456-78 999"));
    }

    protected void printChecks(SlingUrl url) {
        // hook for using assertion code generator
    }

    /**
     * A currently unfixed bug - check that we don't change the behaviour, but first extend tests.
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void bugRelativeUrl() {
        SlingUrl url;
        url = new SlingUrl(request, "../img/loading.gif");
        printChecks(url);
        ec.checkThat(url.isSpecial(), equalTo(true));
        ec.checkThat(url.getUrl(), equalTo("mailto:ä.user@ö.domain.x"));
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
