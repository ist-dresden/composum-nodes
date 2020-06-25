package com.composum.sling.core.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
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
        ec.checkThat(url.getResource().getPath(), is("/a/bb/ccc"));

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
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/ddd.m.n.json/ddd/eee/xxx.json?d&c=x+y"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/a/bb/,name=ddd,selectors=[m, n],extension=json,suffix=/ddd/eee/xxx.json,parameters={d=[], c=[x y]},resourcePath=/a/bb/ddd]"));
        ec.checkThat(url.getResource().getPath(), is("/a/bb/ddd"));

        url = new SlingUrl(request).fromUrl("/ctx/x/bb/ccc/öä ü.s.x.html/x/x/z.html?a=b&c#fragged", false);
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), is("html"));
        ec.checkThat(url.isExternal(), is(false));
        ec.checkThat(url.getFragment(), is("fragged"));
        ec.checkThat(url.getPathAndName(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getResourcePath(), is("/x/bb/ccc/öä ü"));
        ec.checkThat(url.getSuffix(), is("/x/x/z.html"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.x.html/x/x/z.html?a=b&c#fragged"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/bb/ccc/,name=öä ü,selectors=[s, x],extension=html,suffix=/x/x/z.html,parameters={a=[b], c=[]},fragment=fragged,resourcePath=/x/bb/ccc/öä ü]"));

        url.selector("sel").removeSelector("x")
                .suffix(newResource(resolver, "/c/dd/e%e"))
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
        ec.checkThat(url.getSuffix(), is("/c/dd/e%e"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/bb/ccc/%C3%B6%C3%A4%20%C3%BC.s.sel.html/c/dd/e%25e?c&x=a%C3%B6%C3%BC&%C3%96%C3%9F=%26+12&$#%25%25$&"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/bb/ccc/,name=öä ü,selectors=[s, sel],extension=html,suffix=/c/dd/e%e,parameters={c=[], x=[aöü], Öß=[& 12], $=[]},fragment=%%$&,resourcePath=/x/bb/ccc/öä ü]"));

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
        ec.checkThat(url.getUrl(), is("https://www.google.com/%C3%A4-@%C3%9F$?x=y%C3%9Fz&a#aa"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=https,host=www.google.com,path=/,name=ä-@ß$,parameters={x=[yßz], a=[]},fragment=aa,external=true]"));

        url = new SlingUrl(request).fromUrl("mailto:%C3%A4.user@%C3%B6.domain.x", true); // "mailto:ä.user@ö.domain.x" in UTF-8
        printChecks(url);
        ec.checkThat(url.getContextPath(), is("/ctx"));
        ec.checkThat(url.getExtension(), nullValue());
        ec.checkThat(url.isExternal(), is(true));
        ec.checkThat(url.getFragment(), nullValue());
        ec.checkThat(url.getPathAndName(), is("ä.user@ö.domain.x"));
        ec.checkThat(url.getResourcePath(), nullValue());
        ec.checkThat(url.getSuffix(), nullValue());
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user@%C3%B6.domain.x"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));

        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x");
        printChecks(url);
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user@%C3%B6.domain.x"));
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
        ec.checkThat(url.getUrl(), is("tel:+01%20123%20/%203456-78%20999"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=tel,name=+01 123 / 3456-78 999,external=true]"));

        url = new SlingUrl(request).fromUrl("tel:+01 123 / 3456-78 999");
        printChecks(url);
        ec.checkThat(url.getUrl(), is("tel:+01%20123%20/%203456-78%20999"));
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
        url = new SlingUrl(request).fromUrl("http://ends.with:987/slash/");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=ends.with,port=987,path=/slash/,name=,external=true]"));
        ec.checkThat(url.getUrl(), is("http://ends.with:987/slash/"));

        // ; in the path is encoded since path parameters aren't normally used in Sling,
        // but it's admissible in JCR resource names and only works there when quoted.
        // so we use type OTHER when it's not encoded in the URL - probably some outside thing.
        url = new SlingUrl(request).fromUrl("http://path.param/res;parm=val?quer=ry");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=OTHER,scheme=http,name=//path.param/res;parm=val?quer=ry,external=true]"));
        ec.checkThat(url.getUrl(), is("http://path.param/res;parm=val?quer=ry"));

        // there are exotic things like "ftp://myname@host.dom/%2Fetc/motd.txt" but that's a weird special case we ignore.

        url = new SlingUrl(request).fromUrl("ftp://myname@host.dom/etc/motd.txt");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=ftp,username=myname,host=host.dom,path=/etc/,name=motd,extension=txt,external=true]"));
        ec.checkThat(url.getUrl(), is("ftp://myname@host.dom/etc/motd.txt"));

        url = new SlingUrl(request).fromUrl("ftp://ftp.cs.brown.edu/pub/Effective_C%2B%2B_errata.txt", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=ftp,host=ftp.cs.brown.edu,path=/pub/,name=Effective_C++_errata,extension=txt,external=true]"));
        ec.checkThat(url.getUrl(), is("ftp://ftp.cs.brown.edu/pub/Effective_C++_errata.txt"));

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

        url = new SlingUrl(request).fromUrl("file:/etc/fstab"); // this is "normalized" to file:///etc/fstab
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/etc/,name=fstab,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///etc/fstab"));

        url = new SlingUrl(request).fromUrl("file:///c:/WINDOWS/clock.avi");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/c:/WINDOWS/,name=clock,extension=avi,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///c:/WINDOWS/clock.avi"));


        url = new SlingUrl(request).fromUrl("file:///path/");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=FILE,scheme=file,path=/path/,name=,external=true]"));
        ec.checkThat(url.getUrl(), is("file:///path/"));


        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x", false);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user@%C3%B6.domain.x"));

        url = new SlingUrl(request).fromUrl("//host/path", false); // "protocol relative URL"
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/,name=path,external=true]"));
        ec.checkThat(url.getUrl(), is("//host/path"));

        url = new SlingUrl(request).fromUrl("//host/path/file.sel.txt/the/suffix", false); // "protocol relative URL with extension etc."
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/path/,name=file,selectors=[sel],extension=txt,suffix=/the/suffix,external=true]"));
        ec.checkThat(url.getUrl(), is("//host/path/file.sel.txt/the/suffix"));

        url = new SlingUrl(request).fromUrl("http://host/path/file.sel.txt/the/suffix", false).setScheme(SlingUrl.SCHEME_PROTOCOL_RELATIVE_URL); // "protocol relative URL with extension etc."
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=,host=host,path=/path/,name=file,selectors=[sel],extension=txt,suffix=/the/suffix,external=true]"));
        ec.checkThat(url.getUrl(), is("//host/path/file.sel.txt/the/suffix"));

        newResource(resolver, "/bla/blu");
        url = new SlingUrl(request).fromPathOrUrl("/bla/blu");
        ec.checkThat(url.getResource().getPath(), is("/bla/blu"));
    }

    @Test
    public void emailFormats() {
        url = new SlingUrl(request).fromUrl("mailto:ä.user@ö.domain.x");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=ä.user@ö.domain.x,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%C3%A4.user@%C3%B6.domain.x"));

        url = new SlingUrl(request).fromUrl("mailto:John Smith <john.smith@example.org>");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=John Smith <john.smith@example.org>,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:John%20Smith%20%3Cjohn.smith@example.org%3E"));

        url = new SlingUrl(request).fromUrl("mailto:\"john..doe\"@example.org");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=\"john..doe\"@example.org,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:%22john..doe%22@example.org"));

        url = new SlingUrl(request).fromUrl("mailto:with+symbol@example.com");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=with+symbol@example.com,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:with+symbol@example.com"));

        url = new SlingUrl(request).fromUrl("mailto:us'%20%%r%example.com@example.org"); // broken: literal % signs
        printChecks(url);
        ec.checkThat(url.getName(), is("us' %%r%example.com@example.org"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=us' %%r%example.com@example.org,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:us'%20%25%25r%25example.com@example.org"));

        // unclear whether this actually works in practice - that utf-8 encoding disagrees at least with chrome + mac email
        url = new SlingUrl(request).fromUrl("mailto:=?utf-8?B?w5xtbMOkdXR0ZcOfdA==?= <umlauttest@example.com>");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name==?utf-8?B?w5xtbMOkdXR0ZcOfdA==?= <umlauttest@example.com>,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:=?utf-8?B?w5xtbMOkdXR0ZcOfdA==?=%20%3Cumlauttest@example.com%3E"));
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

        url = new SlingUrl(request).fromUrl("http://somewhere.net/bla%25/name%25.ext%25?parm%25=val%25#frag%25"
                .replaceAll("%25", "%3A%3F%26%23%40%25%2B%3D"));
        printChecks(url); // this is parsed wrong
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=somewhere.net,path=/bla:?&#@%+=/,name=name:?&#@%+=,extension=ext:?&#@%+=,parameters={parm:?&#@%+==[val:?&#@%+=]},fragment=frag:?&#@%+=,external=true]"));
        ec.checkThat(url.getUrl(), is("http://somewhere.net/bla:%3F&%23@%25+=/name:%3F&%23@%25+=.ext:%3F&%23@%25+=?parm:%3F%26%23@%25%2B%3D=val:%3F%26%23@%25%2B%3D#frag:?&%23@%25+="));
    }

    /**
     * What about + in URLs?
     */
    @Test
    public void plusEncoding() throws URISyntaxException {
        url = new SlingUrl(request).fromUrl("git+ht-tp://bla.example.net/buf+bla?foo+bar=bar+baz", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=OTHER,scheme=git+ht-tp,name=//bla.example.net/buf+bla?foo+bar=bar+baz,external=true]"));
        ec.checkThat(url.getUrl(), is("git+ht-tp://bla.example.net/buf+bla?foo+bar=bar+baz"));

        // + is a valid character in a path and should not be encoded.
        url = new SlingUrl(request).fromUrl("http://bla.example.net/buf+bla?foo+bar=bar+baz", true);
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=bla.example.net,path=/,name=buf+bla,parameters={foo bar=[bar baz]},external=true]"));
        ec.checkThat(url.getUrl(), is("http://bla.example.net/buf+bla?foo+bar=bar+baz"));

        url = new SlingUrl(request).fromUrl("http://some-where.0.net/").resourcePath("/with space/with+plus/filä").extension("txt").selectors("raw.sel")
                .parameter("the+first paräm", "the+first valuä")
                .parameter("se&c/n%d", "va/&u%e")
                .parameter("%20", "1")
                .parameter("%20", "2")
                // .parameter("with space", "space value")
                .fragment("nä x%20t");
        url.getUrl();
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,scheme=http,host=some-where.0.net,path=/with space/with+plus/,name=filä,selectors=[raw, sel],extension=txt,parameters={the+first paräm=[the+first valuä], se&c/n%d=[va/&u%e], %20=[1, 2]},fragment=nä x%20t,external=true]"));
        ec.checkThat(url.getUrl(), is("http://some-where.0.net/with%20space/with+plus/fil%C3%A4.raw.sel.txt?the%2Bfirst+par%C3%A4m=the%2Bfirst+valu%C3%A4&se%26c/n%25d=va/%26u%25e&%2520=1&%2520=2#n%C3%A4%20x%2520t"));
        URI uri = new URI(url.getUrl());
        ec.checkThat(uri.getPath(), is("/with space/with+plus/filä.raw.sel.txt"));
        ec.checkThat(uri.getFragment(), is("nä x%20t"));
        // interestingly, this yields nonsense, since both space and + are + :
        ec.checkThat(uri.getQuery(), is("the+first+paräm=the+first+valuä&se&c/n%d=va/&u%e&%20=1&%20=2"));
    }

    @Test
    public void namespacePrefixes() {
        url = new SlingUrl(request, newResource(resolver, "/a/bb/jcr:content/ccc"), "html");
        printChecks(url);
        ec.checkThat(url.getPathAndName(), is("/a/bb/_jcr_content/ccc"));
        ec.checkThat(url.getResourcePath(), is("/a/bb/jcr:content/ccc"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/a/bb/_jcr_content/,name=ccc,extension=html,resourcePath=/a/bb/jcr:content/ccc]"));
        ec.checkThat(url.getUrl(), is("/ctx/a/bb/_jcr_content/ccc.html"));

        url = new SlingUrl(request).fromPath("/bla/jcr:content/blu");
        printChecks(url);
        ec.checkThat(url.getPathAndName(), is("/bla/_jcr_content/blu"));
        ec.checkThat(url.getResourcePath(), is("/bla/jcr:content/blu"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/bla/_jcr_content/,name=blu,resourcePath=/bla/jcr:content/blu]"));
        ec.checkThat(url.getUrl(), is("/ctx/bla/_jcr_content/blu"));

        url = new SlingUrl(request).fromUrl("/__bla_x/_jcr_content");
        printChecks(url);
        ec.checkThat(url.getPathAndName(), is("/__bla_x/_jcr_content"));
        ec.checkThat(url.getResourcePath(), is("/_bla_x/jcr:content"));
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/__bla_x/,name=_jcr_content,resourcePath=/_bla_x/jcr:content]"));
        ec.checkThat(url.getUrl(), is("/ctx/__bla_x/_jcr_content"));
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
        SlingUrl url2 = new SlingUrl(request, url.linkMapper).fromUrl(url.getUrl(), true);
        ec.checkThat(url2.toDebugString(), url2.getUrl(), is(url.getUrl()));
        if (!"host.xxx".equals(url2.getHost())) { // host.xxx has been a significant change in the url by the linkmapper
            ec.checkThat(url2.toDebugString(), url2.toDebugString(), is(url.toDebugString()));
        }

        try {
            URI uri = new URI(url.getUrl());
            ec.checkThat(uri.toString(), is(url.getUrl()));
            SlingUrl url3 = new SlingUrl(request, url.linkMapper).fromUri(uri);
            ec.checkThat(uri + " -> " + url3.toDebugString(), url3.getUrl(), is(url.getUrl()));
            // url2 as reference because mapping can introduce host.xxx
            ec.checkThat(uri + " -> " + url3.toDebugString(), url3.toDebugString(), is(url2.toDebugString()));
        } catch (URISyntaxException e) {
            System.out.println("Not an URI: " + url.getUrl() + " - " + e);
        }
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
            ec.checkThat(msg, u2.getScheme(), is(u1.getScheme()));
            ec.checkThat(msg, u2.getHost(), is(u1.getHost()));
            ec.checkThat(msg, u2.getPort(), is(u1.getPort()));
            ec.checkThat(msg, u2.getPath(), is(u1.getPath()));
            ec.checkThat(msg, u2.getAuthority(), is(u1.getAuthority()));
            ec.checkThat(msg, u2.getFragment(), is(u1.getFragment()));
            ec.checkThat(msg, u2.getUserInfo(), is(u1.getUserInfo()));
            ec.checkThat(msg, u2.getSchemeSpecificPart(), is(u1.getSchemeSpecificPart()));
            ec.checkThat(msg, u2.getQuery(), is(u1.getQuery()));
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @see "https://en.wikipedia.org/wiki/List_of_URI_schemes"
     * @see "https://raw.githubusercontent.com/web-platform-tests/wpt/master/url/resources/urltestdata.json"
     */
    // TODO perhaps extend with stuff from https://raw.githubusercontent.com/web-platform-tests/wpt/master/url/resources/urltestdata.json
    protected final List<String> MISCURL_COLLECTION =
            Pattern.compile("\\|").splitAsStream("" +
                    "|afp://us%C3%A4r@host:4242/the%20pa%2Bth" +
                    "|jar:http://www.foo.com/bar/baz.jar!/COM/foo/Qu%2Bux.class" +
                    "|mailto:jsmith@example.com?subject=A%20Test&body=My%20idea%20is%3A%20%0A" +
                    "|s3://mybucket/puppy.jpg" +
                    "|sftp://us%C3%A4r;fingerprint=4342234@host:4242/some/path/file.txt" +
                    "|smb://workgroup;user:password@server/share/folder/file.txt" +
                    "|ssh://us%C3%A4r;fingerprint=8483823423@host:4242" +
                    "|telnet://us%C3%A4r:pas+sw=ord@host:4242/" +
                    "|telnet://us%C3%A4r:pas%25swo&rd@host" +
                    "|geo:37.786971,-122.399677;crs=Moon-2011;u=35"
            )
                    .map(StringUtils::defaultString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

    /**
     * Checks that various examples of URLs of other protocols are treated correctly.
     */
    @Test
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
     * Ignored, since it does not make much sense.
     */
    @Test
    public void checkMiscUrlsUndecoded() {
        for (String urlString : MISCURL_COLLECTION) {
            ec.checkSucceeds(() -> {
                if (!urlString.contains("mailto:")) { // the mailto example is currently the only one where there are %.
                    SlingUrl url, url2;
                    url = new SlingUrl(request).fromUrl(urlString, false);
                    linkexamples.append("<p><a href=\"").append(url.getUrl()).append("\">").append(url.getUrl()).append("</a></p>\n");
                    System.out.println("\n" + urlString + "\n" + url.toDebugString() + "\n" + url.getUrl());
                    verifySameUrl(urlString, url.getUrl());
                    url2 = new SlingUrl(request).fromUrl(url.getUrl(), true);
                    ec.checkThat(urlString, url.getUrl(), is(url2.getUrl()));
                }
                return null;
            });
        }
    }

    @Test
    public void pathEncodingAndMapping() {
        String path = "/x/a b+c%d/e<f]g";

        url = new SlingUrl(request).fromPath(path);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/a%20b+c%25d/e%3Cf%5Dg"));

        url = new SlingUrl(request, LinkMapper.RESOLVER).fromPath(path);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/a%20b+c%25d/e%3Cf%5Dg"));

        url = new SlingUrl(request, LinkMapper.CONTEXT).fromPath(path);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(url.getUrl(), is("/ctx/x/a%20b+c%25d/e%3Cf%5Dg"));

        url = new SlingUrl(request, (LinkMapper) null).fromPath(path);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/a b+c%d/,name=e<f]g,resourcePath=/x/a b+c%d/e<f]g]"));
        ec.checkThat(url.getUrl(), is("/x/a%20b+c%25d/e%3Cf%5Dg"));

        url = new SlingUrl(request, LinkMapper.RESOLVER).fromPath("/x/rpage-_@%(){}$!'+,=-\\X");
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=HTTP,path=/x/,name=rpage-_@%(){}$!'+,=-\\X,resourcePath=/x/rpage-_@%(){}$!'+,=-\\X]"));
        ec.checkThat(url.getUrl(), is("http://host.xxx/rpage-_@%25()%7B%7D$!'+,=-%5CX"));
    }

    /**
     * Demonstrates behavior of {@link org.apache.commons.codec.net.URLCodec}.
     */
    @Test
    public void urlCodecTest() throws EncoderException, DecoderException {
        URLCodec codec = new URLCodec(); // UTF-8
        ec.checkThat(codec.encode(" <>#%\"{}|\\^[]`"), is("+%3C%3E%23%25%22%7B%7D%7C%5C%5E%5B%5D%60")); // excluded characters
        ec.checkThat(codec.encode("-_.!+~*'()"), is("-_.%21%2B%7E*%27%28%29")); // "unreserved" characters
        ec.checkThat(codec.encode(";/?:@&=+$,"), is("%3B%2F%3F%3A%40%26%3D%2B%24%2C")); // reserved characters
        ec.checkThat(codec.encode("abzABZ09"), is("abzABZ09")); // alphanum
        ec.checkThat(codec.encode("ä-ö-\u20AC"), is("%C3%A4-%C3%B6-%E2%82%AC")); // examples of other stuff. (last one is euro symbol)
        ec.checkThat(codec.decode("+%3C%3E%23%25%22%7B%7D%7C%5C%5E%5B%5D%60-_.%21%2B%7E*%27%28%29%3B%2F%3F%3A%40%26%3D%2B%24%2CabzABZ09%C3%A4-%C3%B6-%E2%82%AC"), is(" <>#%\"{}|\\" +
                "^[]`-_.!+~*'();/?:@&=+$,abzABZ09ä-ö-€"));
        ec.checkThat(codec.decode("^[]`-_.!+~*'();/?:@&=+$,abzABZ09ä-ö-€"), is("^[]`-_.! ~*'();/?:@&= $,abzABZ09?-?-?")); // replaces invalid characters by ?
        ec.checkThat(codec.encode(" "), is("+"));
        ec.checkThat(codec.decode("+"), is(" "));
    }

    @Test
    public void percentInSpecialOrOther() {
        url = new SlingUrl(request).fromUrl("mailto:the%having <mailadd@ress.com>");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=SPECIAL,scheme=mailto,name=the%having <mailadd@ress.com>,external=true]"));
        ec.checkThat(url.getUrl(), is("mailto:the%25having%20%3Cmailadd@ress.com%3E"));

        url = new SlingUrl(request).fromUrl("impo+ssible:whata%sign");
        printChecks(url);
        ec.checkThat(url.toDebugString(), is("SlingUrl[type=OTHER,scheme=impo+ssible,name=whata%sign,external=true]"));
        ec.checkThat(url.getUrl(), is("impo+ssible:whata%sign"));
    }

    @Test
    public void specialAccessors() {
        url = new SlingUrl(request).fromPath("/a/b/c");
        ec.checkThat(url.getPathAndNameExt(), is("/a/b/c"));
        printChecks(url);
        ec.checkThat(this.url.toDebugString(), is("SlingUrl[type=HTTP,path=/a/b/,name=c,resourcePath=/a/b/c]"));
        ec.checkThat(this.url.getUrl(), is("/ctx/a/b/c"));

        url = new SlingUrl(request).setPathAndNameExt("bla/name.ext");
        ec.checkThat(url.getPathAndNameExt(), is("bla/name.ext"));
        printChecks(url);
        ec.checkThat(this.url.toDebugString(), is("SlingUrl[type=RELATIVE,path=bla/,name=name,extension=ext]"));
        ec.checkThat(this.url.getUrl(), is("bla/name.ext"));

        url = new SlingUrl(request).setPathAndNameExt("/bla/name.and.ext");
        ec.checkThat(url.getPathAndNameExt(), is("/bla/name.and.ext"));
        ec.checkThat(this.url.toDebugString(), is("SlingUrl[type=HTTP,path=/bla/,name=name.and,extension=ext,resourcePath=/bla/name.and]"));
        ec.checkThat(this.url.getUrl(), is("/ctx/bla/name.and.ext"));

        url.setExtension(".bla");
        ec.checkThat(this.url.toDebugString(), is("SlingUrl[type=HTTP,path=/bla/,name=name.and,extension=bla,resourcePath=/bla/name.and]"));
        ec.checkThat(this.url.getUrl(), is("/ctx/bla/name.and.bla"));

        url.selector("sel1", "sel2");
        url.setExtension("blub");
        ec.checkThat(this.url.toDebugString(), is("SlingUrl[type=HTTP,path=/bla/,name=name.and,selectors=[sel1, sel2],extension=blub,resourcePath=/bla/name.and]"));
        ec.checkThat(this.url.getUrl(), is("/ctx/bla/name.and.sel1.sel2.blub")); // dangerous: parse this again and it's wrong.
    }

}
