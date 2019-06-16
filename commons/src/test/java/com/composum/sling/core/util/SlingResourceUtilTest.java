package com.composum.sling.core.util;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class SlingResourceUtilTest extends SlingResourceUtil {

    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

    @Test
    public void relativePath() {
        errorCollector.checkThat(callRelativePath("bla", "bla/blu"), equalTo("blu"));
        errorCollector.checkThat(callRelativePath("/", "/fol/der"), equalTo("fol/der"));
        errorCollector.checkThat(callRelativePath("bla", "bla"), equalTo(""));
        errorCollector.checkThat(callRelativePath("/bla", "/blu"), equalTo("../blu"));
        errorCollector.checkThat(callRelativePath("/foo/bl/../la", "/foo/la/lu/../lu/lux"), equalTo("lu/lux"));
        errorCollector.checkThat(callRelativePath("/bl/../la", "/la/lu/../lu/lux"), equalTo("lu/lux"));
        errorCollector.checkThat(callRelativePath("/bl/la/lu", "/bl/la/x"), equalTo("../x"));
        errorCollector.checkThat(callRelativePath("/bl/la/lu", "/bl/x/y"), equalTo("../../x/y"));
        errorCollector.checkThat(callRelativePath("/blu/la/lu", "/bl/x/y"), equalTo("../../../bl/x/y"));
        errorCollector.checkThat(callRelativePath("/blu/la/lu", "/"), equalTo("../../../"));
        errorCollector.checkThat(callRelativePath("/blu/la/lu", "/x"), equalTo("../../../x"));
        errorCollector.checkThat(callRelativePath("/", "/"), equalTo(""));
    }

    private String callRelativePath(String parent, String child) {
        String relpath = relativePath(parent, child);
        // that's the invariant.
        errorCollector.checkThat(ResourceUtil.normalize(parent + "/" + relpath), is(ResourceUtil.normalize(child)));
        // check some variations
        errorCollector.checkThat(relativePath(parent + "/", child), is(relpath));
        errorCollector.checkThat(relativePath(parent + "/", child + "/"), is(relpath));
        errorCollector.checkThat(relativePath(parent, child + "/"), is(relpath));
        return relpath;
    }
}
