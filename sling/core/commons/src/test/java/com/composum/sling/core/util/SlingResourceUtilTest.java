package com.composum.sling.core.util;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class SlingResourceUtilTest extends SlingResourceUtil {

    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

    @Test
    public void relativePath() {
        errorCollector.checkThat(relativePath("bla", "bla/blu"), equalTo("blu"));
        errorCollector.checkThat(relativePath("/", "/fol/der"), equalTo("fol/der"));
        errorCollector.checkThat(relativePath("bla", "bla"), equalTo(""));
        errorCollector.checkThat(relativePath("/foo/bl/../la", "/foo/la/lu/../lu/lux"), equalTo("lu/lux"));
        errorCollector.checkThat(relativePath("/bl/../la", "/la/lu/../lu/lux"), equalTo("lu/lux"));
    }
}
