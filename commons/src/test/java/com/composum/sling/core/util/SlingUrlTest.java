package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.*;
import org.junit.rules.ErrorCollector;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Some tests for {@link SlingUrl}, but compare also SlingUrlCompareToResolverTest in the test project, which uses an actual resolver.
 */
public class SlingUrlTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected SlingHttpServletRequest request;
    protected ResourceResolver resolver;
    protected SlingUrl url;
    protected StringBuilder linkexamples = new StringBuilder();

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
            return matcher.matches() ? "http://host.xxx" + LinkUtil.encodePath(matcher.group(1))
                    : req.getContextPath() + LinkUtil.encodePath(uri);
        });
        when(request.getContextPath()).thenReturn("/ctx");
        when(request.getResourceResolver()).thenReturn(resolver);
        when(request.getAttribute(LinkMapper.LINK_MAPPER_REQUEST_ATTRIBUTE)).thenReturn(null);
    }

    @Test
    public void slingUrlTest() {
        url = new SlingUrl(request, newResource(resolver, "/a/bb/ccc"), "html");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("/a/bb/ccc"));
        ec.checkThat(url.getResourcePath(), is("/a/bb/ccc"));
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/ccc.html"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/a/bb/,name=ccc,extension=html,resourcePath=/a/bb/ccc]"));

        url = new SlingUrl(request, newResource(resolver, "/a/bb/ddd"))
                .selectors("m.n").extension("json").suffix("/ddd/eee/xxx.json").parameters("d&c=x%20y");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("json"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("/a/bb/ddd"));
        ec.checkThat(url.getResourcePath(), is("/a/bb/ddd"));
        ec.checkThat(url.getSuffix(), is("/ddd/eee/xxx.json"));
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/ddd.m.n.json/ddd/eee/xxx.json?d&c=x%20y"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/a/bb/,name=ddd,selectors=[m, n],extension=json,suffix=/ddd/eee/xxx.json,parameters={d=[], c=[x y]},resourcePath=/a/bb/ddd]"));

        url = new SlingUrl(request).fromUrl("/ctx/x/bb/ccc/öä ü.s.x.html/x/x/z.html?a=b&c", false);
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getResourcePath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getSuffix(), is("/x/x/z.html"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.x.html/x/x/z.html?a=b&c"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,contextPath=/ctx,path=/x/bb/ccc/,name=öä ü,selectors=[s, x],extension=html,suffix=/x/x/z.html,parameters={a=[b], c=[]},resourcePath=/x/bb/ccc/öä ü]"));

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
        ec.checkThat(url.getPathAndName(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getResourcePath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getSuffix(), is("/c/dd/e#e"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.sel.html/c/dd/e%23e?c&x=a%C3%B6%C3%BC&%C3%96%C3%9F=%26%2012&%24#%25%25%24%26"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,contextPath=/ctx,path=/x/bb/ccc/,name=öä ü,selectors=[s, sel],extension=html,suffix=/c/dd/e#e,parameters={c=[], x=[aöü], Öß=[& 12], $=[]},fragment=%%$&,resourcePath=/x/bb/ccc/öä ü]"));

        url = new SlingUrl(request).fromUrl("https://www.google.com/");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("/"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("https://www.google.com/"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=https,host=www.google.com,path=/,name=,external=true]"));


        url = new SlingUrl(request).fromUrl("https://www.google.com/ä-@ß$?x=yßz&a#aa");
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), is("aa"));
        ec.checkThat(url.getPathAndName(), is("/ä-@ß$"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("https://www.google.com/%C3%A4-%40%C3%9F%24?x=y%C3%9Fz&a#aa"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=https,host=www.google.com,path=/,name=ä-@ß$,parameters={x=[yßz], a=[]},fragment=aa,external=true]"));

        url = new SlingUrl(request).fromUrl("mailto:%C3%A4.user%40%C3%B6.domain.x", true); // "mailto:ä.user@ö.domain.x" in UTF-8
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("ä.user@ö.domain.x"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user%40%C3%B6.domain.x"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));

        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x");
        printChecks(url);
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user%40%C3%B6.domain.x"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));

        url = new SlingUrl(request).fromUrl("tel:%2B01%20123%20/%203456-78%20999", true); // +01 123 / 3456-78 999
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("+01 123 / 3456-78 999"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("tel:%2B01%20123%20/%203456-78%20999"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=tel,name=+01 123 / 3456-78 999,external=true]"));

        url = new SlingUrl(request).fromUrl("tel:+01 123 / 3456-78 999");
        printChecks(url);
        ec.checkThat(url.getUrl(), is("tel:%2B01%20123%20/%203456-78%20999"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=tel,name=+01 123 / 3456-78 999,external=true]"));

        url = new SlingUrl(request).fromUrl("some/path.ext");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=RELATIVE,path=some/,name=path,extension=ext]"));
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("ext"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("some/path"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("some/path.ext"));

        // without context path
        url = new SlingUrl(request).fromUrl("/x/bb", false);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/,name=bb,resourcePath=/x/bb]"));
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb")); // linkmapper removes /x

        url = new SlingUrl(request).fromUrl("../img/loading.gif");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=OTHER,name=../img/loading.gif]"));
        ec.checkThat(url.getUrl(), equalTo("../img/loading.gif"));
    }

    @Test
    public void additionalTests() {
        url = new SlingUrl(request).fromUrl("http://ends.with/slash/");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=ends.with,path=/slash/,name=,external=true]"));
        ec.checkThat(url.getUrl(), is("http://ends.with/slash/"));

        // there are exotic things like "ftp://myname@host.dom/%2Fetc/motd.txt" but that's a weird special case we ignore.

        url = new SlingUrl(request).fromUrl("ftp://myname@host.dom/etc/motd.txt");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=ftp,username=myname,host=host.dom,path=/etc/,name=motd,extension=txt,external=true]"));
        ec.checkThat(url.getUrl(), is("ftp://myname@host.dom/etc/motd.txt"));

        url = new SlingUrl(request).fromUrl("ftp://ftp.cs.brown.edu/pub/Effective_C%2B%2B_errata.txt", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=ftp,host=ftp.cs.brown.edu,path=/pub/,name=Effective_C++_errata,extension=txt,external=true]"));
        ec.checkThat(url.getUrl(), is("ftp://ftp.cs.brown.edu/pub/Effective_C%2B%2B_errata.txt"));

        url = new SlingUrl(request).fromUrl("ftp://myname:pass@host.dom/etc/");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=ftp,username=myname,password=pass,host=host.dom,path=/etc/,name=,external=true]"));
        ec.checkThat(url.getUrl(), is("ftp://myname:pass@host.dom/etc/"));

        url = new SlingUrl(request).fromUrl("file://localhost/etc/fstab");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,host=localhost,path=/etc/,name=fstab,external=true]"));
        ec.checkThat(url.getUrl(), is("file://localhost/etc/fstab"));


        url = new SlingUrl(request).fromUrl("file:///etc/fstab");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/etc/,name=fstab,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///etc/fstab"));


        url = new SlingUrl(request).fromUrl("file:///c:/WINDOWS/clock.avi");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/c:/WINDOWS/,name=clock,extension=avi,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///c%3A/WINDOWS/clock.avi"));


        url = new SlingUrl(request).fromUrl("file:///path/");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/path/,name=,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///path/"));


        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x", false);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user%40%C3%B6.domain.x"));

        url = new SlingUrl(request).fromUrl("//host/path", false); // "protocol relative URL"
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/,name=path,resourcePath=/path]"));
        ec.checkThat(url.getUrl(), is("//host/ctx/path"));

        url = new SlingUrl(request).fromUrl("//host/path/file.sel.txt/the/suffix", false); // "protocol relative URL with extension etc."
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/path/,name=file,selectors=[sel],extension=txt,suffix=/the/suffix,resourcePath=/path/file]"));
        ec.checkThat(url.getUrl(), is("//host/ctx/path/file.sel.txt/the/suffix"));

        url = new SlingUrl(request).fromUrl("http://host/path/file.sel.txt/the/suffix", false).setScheme(SlingUrl.SCHEME_PROTOCOL_RELATIVE_URL); // "protocol relative URL with extension etc."
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/path/,name=file,selectors=[sel],extension=txt,suffix=/the/suffix,resourcePath=/path/file]"));
        ec.checkThat(url.getUrl(), is("//host/ctx/path/file.sel.txt/the/suffix"));
    }

    @Test
    public void emailFormats() {
        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user%40%C3%B6.domain.x"));

        url = new SlingUrl(request).fromUrl("mailto:John Smith <john.smith@example.org>");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=John Smith <john.smith@example.org>,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:John%20Smith%20%3Cjohn.smith%40example.org%3E"));

        url = new SlingUrl(request).fromUrl("mailto:\"john..doe\"@example.org");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=\"john..doe\"@example.org,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%22john..doe%22%40example.org"));

        url = new SlingUrl(request).fromUrl("mailto:with+symbol@example.com");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=with+symbol@example.com,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:with%2Bsymbol%40example.com"));

        url = new SlingUrl(request).fromUrl("mailto:us'%20%%r%example.com@example.org");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=us'%20%%r%example.com@example.org,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:us%27%2520%25%25r%25example.com%40example.org"));

        url = new SlingUrl(request).fromUrl("mailto:=?utf-8?B?w5xtbMOkdXR0ZcOfdA==?= <umlauttest@example.com>");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name==?utf-8?B?w5xtbMOkdXR0ZcOfdA==?= <umlauttest@example.com>,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%3D%3Futf-8%3FB%3Fw5xtbMOkdXR0ZcOfdA%3D%3D%3F%3D%20%3Cumlauttest%40example.com%3E"));
    }

    /**
     * This checks what it does when parsing cases that cannot be parsed unambiguously from the URL String.
     * We'd have to consult the resource tree, which we do not do. Better avoid such cases.
     *
     * @see "https://sling.apache.org/documentation/the-sling-engine/url-decomposition.html#overview"
     */
    @Test
    public void ambiguousSpecialCases() {
        url = new SlingUrl(request).fromUrl("http://host/a.b/c.d/suffix");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=host,path=/,name=a,extension=b,suffix=/c.d/suffix,external=true]"));
        ec.checkThat(url.getUrl(), is("http://host/a.b/c.d/suffix"));
    }

    @Test
    public void weirdCharacters() {
        url = new SlingUrl(request).fromUrl("/a resource/with spaces");
        ec.checkThat(url.getUrl(), is("/ctx/a%20resource/with%20spaces"));
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/a resource/,name=with spaces,resourcePath=/a resource/with spaces]"));
        ec.checkThat(url.getUrl(), is("/ctx/a%20resource/with%20spaces"));

        url = new SlingUrl(request).fromUrl("/a.resource/with.periods");
        printChecks(url); // this is parsed wrong
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/,name=a,extension=resource,suffix=/with.periods,resourcePath=/a]"));
        ec.checkThat(url.getUrl(), is("/ctx/a.resource/with.periods"));
    }

    /**
     * What about + in URLs?
     */
    @Test
    public void plusEncoding() throws URISyntaxException {
        url = new SlingUrl(request).fromUrl("git+ht-tp://bla.example.net/buf+bla?foo=bar+baz", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=OTHER,scheme=git+ht-tp,name=//bla.example.net/buf+bla?foo=bar+baz,external=true]"));
        ec.checkThat(url.getUrl(), is("git+ht-tp://bla.example.net/buf%2Bbla%3Ffoo%3Dbar%2Bbaz"));

        // + is a valid character in a path and should not be encoded.
        url = new SlingUrl(request).fromUrl("http://bla.example.net/buf+bla?foo=bar+baz", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=bla.example.net,path=/,name=buf+bla,parameters={foo=[bar+baz]},external=true]"));
        ec.checkThat(url.getUrl(), is("http://bla.example.net/buf%2Bbla?foo=bar%2Bbaz"));
        // ec.checkThat(url.getUrl(), is("http://bla.example.net/buf+bla?foo=bar%2Bbaz")); FIXME(hps,11.06.20) possibly buf+bla instead of buf%2Bbla

        url = new SlingUrl(request).fromUrl("http://some-where.0.net/").resourcePath("/with space/with+plus/filä").extension("txt").selectors("raw.sel")
                .parameter("the+first paräm", "the+first valuä")
                .parameter("se&c/n%d", "va/&u%e")
                .parameter("%20", "1")
                .parameter("%20", "2")
                .fragment("nä x%20t");
        url.getUrl();
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=some-where.0.net,path=/with space/with+plus/,name=filä,selectors=[raw, sel],extension=txt,parameters={the+first paräm=[the+first valuä], se&c/n%d=[va/&u%e], %20=[1, 2]},fragment=nä x%20t,external=true]"));
        ec.checkThat(url.getUrl(), is("http://some-where.0.net/with%20space/with%2Bplus/fil%C3%A4.raw.sel.txt?the%2Bfirst%20par%C3%A4m=the%2Bfirst%20valu%C3%A4&se%26c/n%25d=va/%26u%25e&%2520=1&%2520=2#n%C3%A4%20x%2520t"));
        URI uri = new URI(url.getUrl());
        ec.checkThat(uri.getPath(), is("/with space/with+plus/filä.raw.sel.txt"));
        ec.checkThat(uri.getFragment(), is("nä x%20t"));
        ec.checkThat(uri.getQuery(), is("the+first paräm=the+first valuä&se&c/n%d=va/&u%e&%20=1&%20=2"));
    }

    /**
     * Generates code to easily create many examples.
     */
    protected void printChecks(SlingUrl url) {
        linkexamples.append("<p><a href=\"").append(url.getUrl()).append("\">").append(url.getUrl()).append("</a></p>\n");
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n        url = new SlingUrl(request, \"").append(url.getUrl()).append("\");")
                .append("\n        printChecks(url);")
                .append("\n        ec.checkThat(url.toDebugString(), is(\"").append(url.toDebugString()).append("\"));")
                .append("\n        ec.checkThat(url.getUrl(), is(\"").append(url.getUrl()).append("\"));");
        System.out.println(builder);

        // sanity check: the url decoded again and encoded again should get us the same things
        SlingUrl url2 = new SlingUrl(request).fromUrl(url.getUrl(), true);
        ec.checkThat(url2.toDebugString(), url2.getUrl(), is(url.getUrl()));
    }

    /**
     * Prints examples for manual check in browser, e.g. with https://www.w3schools.com/html/tryit.asp?filename=tryhtml_links_w3schools .
     */
    @After
    public void printLinks() {
        System.out.println();
        System.out.println(linkexamples);
        linkexamples.setLength(0);
    }

    /**
     * For debugging, e.g. with https://grokconstructor.appspot.com/do/match
     */
    @Test
    public void printPatterns() {
        new SlingUrl(request) {
            {
                System.out.println(SlingUrl.HTTP_URL_PATTERN);
                System.out.println(SlingUrl.FILE_URL_PATTERN);
                System.out.println(SlingUrl.ABSOLUTE_PATH_PATTERN);
                System.out.println(SlingUrl.RELATIVE_PATH_PATTERN);
            }
        };
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

    /**
     * Verifies that these are the same URL wrt. java.net.{@link URI}.
     */
    protected void verifySameUrl(String url1, String url2) {
        try {
            String msg = url1 + " vs. " + url2;
            URI u1 = new URI(url1);
            URI u2 = new URI(url2);
            ec.checkThat(msg, u1.getScheme(), is(u2.getScheme()));
            ec.checkThat(msg, u1.getHost(), is(u2.getHost()));
            ec.checkThat(msg, u1.getPort(), is(u2.getPort()));
            ec.checkThat(msg, u1.getPath(), is(u2.getPath()));
            ec.checkThat(msg, u1.getAuthority(), is(u2.getAuthority()));
            ec.checkThat(msg, u1.getFragment(), is(u2.getFragment()));
            ec.checkThat(msg, u1.getUserInfo(), is(u2.getUserInfo()));
            ec.checkThat(msg, u1.getSchemeSpecificPart(), is(u2.getSchemeSpecificPart()));
            ec.checkThat(msg, u1.getQuery(), is(u2.getQuery()));
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @see "https://en.wikipedia.org/wiki/List_of_URI_schemes"
     */
    protected final List<String> MISCURL_COLLECTION =
            Pattern.compile("\\|").splitAsStream("" +
                    "|afp://us%C3%A4r@host:4242/the%20pa%2Bth" +
                    "|jar:http://www.foo.com/bar/baz.jar!/COM/foo/Qu%ux.class" +
                    "|mailto:jsmith@example.com?subject=A%20Test&body=My%20idea%20is%3A%20%0A" +
                    "|s3://mybucket/puppy.jpg" +
                    "|sftp://us%C3%A4r;fingerprint=4342234@host:4242/some/path/file.txt" +
                    "|smb://workgroup;user:password@server/share/folder/file.txt" +
                    "|ssh://us%C3%A4r;fingerprint=8483823423@host:4242" +
                    "|telnet://us%C3%A4r:pas+sw=ord@host:4242/" +
                    "|telnet://us%C3%A4r:pas%swo&rd@host" +
                    "|geo:37.786971,-122.399677;crs=Moon-2011;u=35"
            )
                    .map(StringUtils::defaultString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

    /**
     * Checks that various examples of URLs of other protocols are treated correctly.
     */
    @Test
    @Ignore
    // FIXME(hps,15.06.20) imlement
    public void checkMiscUrlsDecoded() {
        for (String urlString : MISCURL_COLLECTION) {
            ec.checkSucceeds(() -> {
                SlingUrl url, url2;
                url = new SlingUrl(request).fromUrl(urlString, true);
                linkexamples.append("<p><a href=\"").append(url.getUrl()).append("\">").append(url.getUrl()).append("</a></p>\n");
                System.out.println("\n" + urlString + "\n" + url.toDebugString() + "\n" + url.getUrl());
                verifySameUrl(urlString, url.getUrl());
                url2 = new SlingUrl(request).fromUrl(url.getUrl(), true);
                ec.checkThat(urlString, url.getUrl(), is(url2.getUrl()));
                return null;
            });
        }
    }

    /**
     * Checks that various examples of URLs of other protocols are treated correctly.
     */
    @Test
    @Ignore
    // FIXME(hps,15.06.20) imlement
    public void checkMiscUrlsUndecoded() {
        for (String urlString : MISCURL_COLLECTION) {
            ec.checkSucceeds(() -> {
                SlingUrl url, url2;
                url = new SlingUrl(request).fromUrl(urlString);
                linkexamples.append("<p><a href=\"").append(url.getUrl()).append("\">").append(url.getUrl()).append("</a></p>\n");
                System.out.println("\n" + urlString + "\n" + url.toDebugString() + "\n" + url.getUrl());
                verifySameUrl(urlString, url.getUrl());
                url2 = new SlingUrl(request).fromUrl(url.getUrl(), true);
                ec.checkThat(urlString, url.getUrl(), is(url2.getUrl()));
                return null;
            });
        }
    }

    @Test
    public void pathEncodingAndMapping() {
        String path = "/x/a b+c%d/e<f]g";
        SlingUrl slingUrl;

        slingUrl = new SlingUrl(request).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("http://host.xxx/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(request, LinkMapper.RESOLVER).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("http://host.xxx/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(request, LinkMapper.CONTEXT).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/ctx/x/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(request, (LinkMapper) null).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/x/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(request, LinkMapper.RESOLVER).fromPath("/x/rpage-_@%(){}$!'+,=-\\X");
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/x/,name=rpage-_@%(){}$!'+,=-\\X,resourcePath=/x/rpage-_@%(){}$!'+,=-\\X]"));
        ec.checkThat(slingUrl.getUrl(), is("http://host.xxx/rpage-_%40%25%28%29%7B%7D%24%21%27%2B%2C%3D-%5CX"));
    }

}
