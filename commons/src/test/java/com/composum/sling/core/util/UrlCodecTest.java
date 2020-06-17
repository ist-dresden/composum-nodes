package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.*;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Tests for {@link UrlCodec}.
 */
public class UrlCodecTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected UrlCodec codec = new UrlCodec("-a-zA-Z0-9%", StandardCharsets.UTF_8);

    @Test
    public void decode() {
        ec.checkThat(codec.decode(null), nullValue());
        ec.checkThat(codec.decode(""), is(""));
        ec.checkThat(codec.decode("unencoded"), is("unencoded"));
        ec.checkThat(codec.decode("%C3%A4"), is("ä"));
        ec.checkThat(codec.decode("start%C3%A4end"), is("startäend"));
        ec.checkThat(codec.decode("äöstart%C3%A4mid%C3%A4end"), is("äöstartämidäend"));
        ec.checkThat(codec.decode("%C3"), is("\ufffd"));
        ec.checkThat(codec.decode("bla%C3blu"), is("bla\ufffdblu"));
        ec.checkThat(codec.decode("start%C3%A4mid%end"), is("startämid%end"));
        ec.checkThat(codec.decode("-_.%21%2B%7E*%27%28%29"), is("-_.!+~*'()"));
        ec.checkThat(codec.decode("%E1%A0%C0"), is("\ufffd")); // invalid UTF-8 seq.
        ec.checkThat(codec.decode(StringUtils.repeat("%2B", 1024)).length(), is(1024));

        ec.checkThat(codec.decode("an%effect"), is("an\ufffdfect")); // possibly accidential decoding

        ec.checkThat(new UrlCodec("%", Charset.forName("ISO-8859-1")).decode("%E4"), is("ä"));
    }

    @Test
    public void decodeValidated() {
        ec.checkThat(codec.decodeValidated("start%C3%A4mid%2Bend"), is("startämid+end"));
        ec.checkThat(codec.isValid("start%C3%A4mid%2Bend"), is(true));
        for (String invalid : Arrays.asList("%C3", "%", "start%C3%A4mid%end", "%E1%A0%C0")) {
            try {
                codec.decodeValidated(invalid);
                ec.addError(new AssertionError("Should fail: " + invalid));
            } catch (IllegalArgumentException e) {
                // expected.
            }
            ec.checkThat(invalid, codec.isValid(invalid), is(false));
        }
    }

    @Test
    public void isValid() {
        ec.checkThat(codec.isValid(""), is(true));
        ec.checkThat(codec.isValid(null), is(true));
        ec.checkThat(codec.isValid("start%C3%A4mid%2Bend"), is(true));
        ec.checkThat(codec.isValid("start%C3%A4mid%end"), is(false));
        ec.checkThat(codec.isValid("%C3"), is(false));
        ec.checkThat(codec.isValid("%E1%A0%C0"), is(false));
    }

}
