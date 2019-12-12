package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class SlingResourceUtilTest extends SlingResourceUtil {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Test
    public void testRelativePath() {
        ec.checkThat(callRelativePath("bla", "bla/blu"), equalTo("blu"));
        ec.checkThat(callRelativePath("/", "/fol/der"), equalTo("fol/der"));
        ec.checkThat(callRelativePath("bla", "bla"), equalTo(""));
        ec.checkThat(callRelativePath("/bla", "/blu"), equalTo("../blu"));
        ec.checkThat(callRelativePath("/foo/bl/../la", "/foo/la/lu/../lu/lux"), equalTo("lu/lux"));
        ec.checkThat(callRelativePath("/bl/../la", "/la/lu/../lu/lux"), equalTo("lu/lux"));
        ec.checkThat(callRelativePath("/bl/la/lu", "/bl/la/x"), equalTo("../x"));
        ec.checkThat(callRelativePath("/bl/la/lu", "/bl/x/y"), equalTo("../../x/y"));
        ec.checkThat(callRelativePath("/blu/la/lu", "/bl/x/y"), equalTo("../../../bl/x/y"));
        ec.checkThat(callRelativePath("/blu/la/lu", "/"), equalTo("../../../"));
        ec.checkThat(callRelativePath("/blu/la/lu", "/x"), equalTo("../../../x"));
        ec.checkThat(callRelativePath("/", "/"), equalTo(""));
    }

    private String callRelativePath(String parent, String child) {
        String relpath = relativePath(parent, child);
        // that's the invariant.
        ec.checkThat(ResourceUtil.normalize(parent + "/" + relpath), is(ResourceUtil.normalize(child)));
        // check some variations
        ec.checkThat(relativePath(parent + "/", child), is(relpath));
        ec.checkThat(relativePath(parent + "/", child + "/"), is(relpath));
        ec.checkThat(relativePath(parent, child + "/"), is(relpath));
        return relpath;
    }

    @Test
    public void testAppendPaths() {
        checkAppendPaths("/bla", "blu/bluf", "/bla/blu/bluf");
        checkAppendPaths("/bla/bla/", "blu/bluf", "/bla/bla/blu/bluf");
        checkAppendPaths("bla/la", "blu", "bla/la/blu");
        checkAppendPaths("bla/la/", "/blu", "bla/la/blu");
        checkAppendPaths("/", "blu", "/blu");
        checkAppendPaths("/", "/blu", "/blu");
        for (String childpath : Arrays.asList(null, "", "/")) {
            for (String path : Arrays.asList("/", "/bla", "bla", "/bla/blu")) {
                checkAppendPaths(path, childpath, path);
            }
        }
        for (String path : Arrays.asList(null, "")) {
            for (String childpath : Arrays.asList(null, "", "/", "bla", "/bla", "bla/bluf", "/bla/bluf")) {
                checkAppendPaths(path, childpath, null);
            }
        }
    }

    protected void checkAppendPaths(String path, String childpath, String result) {
        ec.checkThat(path + " with " + childpath, appendPaths(path, childpath), equalTo(result));
        ec.checkThat(path + " with " + childpath, StringUtils.startsWith(path, "/"),
                equalTo(StringUtils.startsWith(result, "/")));
    }

    @Test
    public void testCommonParent() {
        ec.checkThat(commonParent(null), nullValue());
        ec.checkThat(commonParent(Collections.emptyList()), nullValue());
        ec.checkThat(commonParent(Collections.emptySet()), nullValue());
        ec.checkThat(commonParent(Arrays.asList(null, "")), nullValue());

        ec.checkThat(commonParent(Arrays.asList("/a/b", "/a/c")), is("/a"));
        ec.checkThat(commonParent(Arrays.asList("/a/b", "/a", "/a/d")), is("/a"));
        ec.checkThat(commonParent(Arrays.asList("/b", "/a", "/a/d")), is("/"));

        ec.checkThat(commonParent(Arrays.asList("/a/b", "", "/a", null, "/a/d")), is("/a"));

        ec.checkThat(commonParent(Arrays.asList("a/b", "a/c")), is("a"));
        ec.checkThat(commonParent(Arrays.asList("a/b", "a", "a/d")), is("a"));
        ec.checkThat(commonParent(Arrays.asList("b", "a", "a/d")), nullValue());

        ec.checkThat(commonParent(Arrays.asList("a/b", "/a/b")), nullValue()); // wrong call


    }

    /** Does not quite belong here, but needed for {@link #commonParent(Collection)}. */
    @Test
    public void testGetParent() {
        ec.checkThat(ResourceUtil.getParent("/a/b"), is("/a"));
        ec.checkThat(ResourceUtil.getParent("/a"), is("/"));
        ec.checkThat(ResourceUtil.getParent("/"), nullValue());

        ec.checkThat(ResourceUtil.getParent("workspace:/a/b"), is("workspace:/a"));
        ec.checkThat(ResourceUtil.getParent("workspace:/a"), is("workspace:/"));
        ec.checkThat(ResourceUtil.getParent("workspace:/"), nullValue());

        ec.checkThat(ResourceUtil.getParent("a/b"), is("a"));
        ec.checkThat(ResourceUtil.getParent("a"), nullValue());
        ec.checkThat(ResourceUtil.getParent(""), nullValue());
    }


}
