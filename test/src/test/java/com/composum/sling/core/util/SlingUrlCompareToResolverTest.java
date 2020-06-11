package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;

/**
 * We compare the parsing of {@link SlingUrl} with {@link org.apache.sling.api.resource.ResourceResolver#resolve(HttpServletRequest, String)} for special cases.
 */
public class SlingUrlCompareToResolverTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected boolean firstLine = true;

    protected Resource newResource(String path) {
        return context.build().resource(path).commit().getCurrentParent();
    }

    /**
     * If there is no dot in the resource path, it works nicely.
     */
    @Test
    public void normal() {
        Resource normal = newResource("/ab/cd/jcr:content/ef");
        for (String url : Arrays.asList("/ab", "/ab/cd", "/ab/cd.html", "/ab.html/cd.html", "/ab/cd/jcr:content", "/ab/cd.html/jcr:content/ef", "/ab/cd/jcr:content.html", "/ab/cd/jcr:content/ef", "/ab.html", "/ab/cd/jcr:content/ef.raw.html", "/ab/cd.bla.html/blubber", "/ab/cd.a.sel.ec.tor.html/qux.html")) {
            checkUrl(url, true);
        }
    }

    /**
     * If there is no resource, https://sling.apache.org/documentation/the-sling-engine/url-decomposition.html#overview says we should take what's up to the first dot as
     * resource path. Confusingly, that's however not what {@link org.apache.sling.api.resource.ResourceResolver#resolve(HttpServletRequest, String)} actually does: it takes
     * the full path. Reported in https://issues.apache.org/jira/browse/SLING-9508 .
     */
    @Test
    public void noResource() {
        // we ignore  which IMHO brokenly resolves as /ab/cd.html , similarly /ab.html
        //

        // these are as expected, but that's when there is no dot.
        for (String url : Arrays.asList("/ab", "/ab/cd", "/ab/cd/jcr:content", "/ab/cd/jcr:content/ef")) {
            checkUrl(url, true);
        }
        // these are IMHO broken at ResourceResolver#resolve - it just takes the whole path but shouldn't according to the doc.
        for (String url : Arrays.asList("/ab/cd.html", "/ab.html", "/ab/cd/jcr:content/ef.raw.html", "/ab/cd.bla.html/blubber", "/ab/cd.a.sel.ec.tor.html/qux.html", "/a.b.bla.html", "/a.b/c/d.e.html", "/a.b.html/c/d.e.html", "/a.b/x/d.e.html")) {
            checkUrl(url, false);
        }
    }

    /**
     * If a resource path contains a dot at some level, we rarely can parse it correctly without considering the resource tree.
     */
    @Test
    public void resourceWithDots() {
        Resource weird = newResource("/a.b/c/d.e");
        // these can be parsed correctly without considering the resource tree. Rarely happens.
        for (String url : Arrays.asList("/a.b")) {
            checkUrl(url, true);
        }

        // for these the algorithm takes the wrong pick
        for (String url : Arrays.asList("/a.b.selector.html", "/a.b/c", "/a.b/c.html", "/a.b/c/d.e.html", "/a.b.html/c/d.e.html", "/a.b/x/d.e.html")) {
            checkUrl(url, false);
        }
    }

    /**
     * Verifies that SlingUrl gives the same resourcepath as {@link org.apache.sling.api.resource.ResourceResolver#resolve(HttpServletRequest, String)}.
     * If invert is true, we expect a different result - that is not good, but sometimes inevitable.
     */
    protected void checkUrl(String url, boolean expectedEqual) {
        SlingUrl slingUrl = new SlingUrl(context.request()).fromUrl(url);
        Resource resolved = context.resourceResolver().resolve(context.request(), url);
        ec.checkThat(url, resolved, notNullValue());
        String resolvedPath = resolved != null ? resolved.getPath() : null;
        if (expectedEqual) {
            ec.checkThat(url, slingUrl.getResourcePath(), is(resolvedPath));
        } else {
            ec.checkThat(url, slingUrl.getResourcePath(), not(is(resolvedPath)));
        }
        int pad = 40;
        if (firstLine) {
            firstLine = false;
            System.out.println(
                    StringUtils.rightPad("  url", pad + 4)
                            + StringUtils.rightPad("  resolvedPath", pad + 2)
                            + StringUtils.rightPad("slingUrl.getResourcePath()", pad));
        }
        System.out.println(
                (expectedEqual ? "  " : "B ")
                        + (slingUrl.getResourcePath().equals(resolvedPath) ? "  " : "D ") // Difference spotted
                        + StringUtils.rightPad(url, pad)
                        + (ResourceUtil.isNonExistingResource(resolved) ? "N " : "  ") // Nonexisting
                        + StringUtils.rightPad(resolvedPath, pad)
                        + StringUtils.rightPad(slingUrl.getResourcePath(), pad));
    }

    @Test
    public void pathEncodingAndMapping() {
        String path = "/content/a b+c%d/e<f]g";
        SlingUrl slingUrl;
        context.request().setContextPath("/ctx");

        slingUrl = new SlingUrl(context.request()).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/content/a b+c%d/,name=e<f]g,resourcePath=/content/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/ctx/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(context.request(), LinkMapper.RESOLVER).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/content/a b+c%d/,name=e<f]g,resourcePath=/content/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/ctx/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(context.request(), LinkMapper.CONTEXT).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/content/a b+c%d/,name=e<f]g,resourcePath=/content/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/ctx/content/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(context.request(), (LinkMapper) null).fromPath(path);
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/content/a b+c%d/,name=e<f]g,resourcePath=/content/a b+c%d/e<f]g]"));
        ec.checkThat(slingUrl.getUrl(), is("/content/a%20b%2Bc%25d/e%3Cf%5Dg"));

        slingUrl = new SlingUrl(context.request(), LinkMapper.RESOLVER).fromPath("/content/rpage-_@%(){}$!'+,=-\\X");
        ec.checkThat(slingUrl.getUrl(), is("/ctx/rpage-_@%25()%7B%7D$!'%2B,=-%5CX"));
        ec.checkThat(slingUrl.toDebugString(), is("SlingUrl[type=HTTP,path=/content/,name=rpage-_@%(){}$!'+,=-\\X,resourcePath=/content/rpage-_@%(){}$!'+,=-\\X]"));
    }

    /**
     * Checks for encoding problems using special characters.
     *
     * @see "https://issues.apache.org/jira/browse/SLING-9514"
     */
    @Test
    public void resolverMapCheck() throws UnsupportedEncodingException, URISyntaxException {
        ResourceResolver resolver = context.resourceResolver();
        // we explicitly don't check # and ? which are legal in resource names, but using them is probably a very bad idea.
        for (char specialChar : ":_&@%[](){}<$>!'\"*+,;=-/|\\".toCharArray()) {
            String path = "/bla/r" + specialChar + "r";
            String url = resolver.map(context.request(), path);
            Resource resource = resolver.resolve(URLDecoder.decode(url, StandardCharsets.UTF_8.name()));
            // System.out.println(path + "\t" + url + "\t" + resource.getPath());
            ec.checkThat("" + specialChar, resource.getPath(), is(path));
            ec.checkThat("" + specialChar, new URI(url).getPath(), is(path));

            // LinkMapper should treat everything right, too
            url = LinkMapper.RESOLVER.mapUri(context.request(), path);
            resource = resolver.resolve(URLDecoder.decode(url, StandardCharsets.UTF_8.name()));
            ec.checkThat("" + specialChar, resource.getPath(), is(path));
            ec.checkThat("" + specialChar, new URI(url).getPath(), is(path));
        }
    }

}
