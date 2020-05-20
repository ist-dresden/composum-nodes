package com.composum.sling.core.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.equalTo;

public class LinkUtilTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

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
}
