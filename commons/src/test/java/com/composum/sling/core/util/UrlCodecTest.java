package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link UrlCodec}.
 */
public class UrlCodecTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected final UrlCodec codec = new UrlCodec("-+a-zA-Z0-9", StandardCharsets.UTF_8);
    protected final UrlCodec isocodec = new UrlCodec("a-zA-Z0-9", Charset.forName("ISO-8859-1"));

    protected final String URL_UNSAFE_CHARS = "\"<>#%{}|\\^~[]`"; // always encode

    @Test
    public void encode() throws UnsupportedEncodingException {
        ec.checkThat(codec.encode(null), nullValue());
        ec.checkThat(codec.encode(""), is(""));
        ec.checkThat(codec.encode("ab-cd+"), is("ab-cd+"));
        ec.checkThat(codec.encode("ä"), is("%C3%A4"));
        ec.checkThat(codec.encode("a$bäc%x"), is("a%24b%C3%A4c%25x"));
        ec.checkThat(codec.encode(StringUtils.repeat("ä", 1024)), is(StringUtils.repeat("%C3%A4", 1024)));
        ec.checkThat(codec.encode("()$äöü@"), is(URLEncoder.encode("()$äöü@", "UTF-8")));

        ec.checkThat(isocodec.encode("äöü"), is("%E4%F6%FC"));
    }

    @Test
    public void encodeInvalidChars() throws UnsupportedEncodingException {
        ec.checkThat(isocodec.encode("notiso‰"), is("notiso%3F"));
        ec.checkThat(isocodec.encode("‰notiso"), is("%3Fnotiso"));
        ec.checkThat(isocodec.encode("not*‰¼½¾™„”“”‘’‚’/iso"),
                is("not%2A" + StringUtils.repeat("%3F", 14) + "iso"));

        UrlCodec isocodecWithQMark = new UrlCodec("?a-zA-Z0-9", Charset.forName("ISO-8859-1"));

        ec.checkThat(isocodecWithQMark.encode("not*‰¼½¾™„”“”‘’‚’/iso"),
                is("not%2A" + StringUtils.repeat("?", 14) + "iso"));

        // for reference: URLEncoder encodes unknown characters as ?
        ec.checkThat(URLEncoder.encode("€", "ISO-8859-1"), is("%3F"));
    }

    @Test
    public void encodeValidated() {
        ec.checkThat(isocodec.encodeValidated("äöü"), is("%E4%F6%FC"));

        for (String invalid : Arrays.asList("notiso‰", "‰notiso", "not*‰¼½¾™„”“”‘’‚’/iso")) {
            try {
                isocodec.encodeValidated(invalid);
                ec.addError(new AssertionError("Should fail: " + invalid));
            } catch (IllegalArgumentException e) {
                // expected.
            }
        }
    }

    @Test
    public void decode() {
        ec.checkThat(codec.decode(null), nullValue());
        ec.checkThat(codec.decode(""), is(""));
        ec.checkThat(codec.decode("unencoded"), is("unencoded"));
        ec.checkThat(codec.decode("%C3%A4"), is("ä"));
        ec.checkThat(codec.decode("sta%25rt%C3%A4end"), is("sta%rtäend"));
        ec.checkThat(codec.decode("äöstart%C3%A4mid%C3%A4end"), is("äöstartämidäend"));
        ec.checkThat(codec.decode("%C3"), is("\ufffd"));
        ec.checkThat(codec.decode("bla%C3blu"), is("bla\ufffdblu"));
        ec.checkThat(codec.decode("start%C3%A4mid%end"), is("startämid%end"));
        ec.checkThat(codec.decode("-_.%21%2B%7E*%27%28%29"), is("-_.!+~*'()"));

        // invalid UTF-8 seq.
        ec.checkThat(codec.decode("%E1%A0%C0"), is("\ufffd"));
        // should be several "\ufffd", but determining the right number would be hard and often impossible.
        ec.checkThat(codec.decode("%E1%A0%C0%E1%A0%C0"), is("\ufffd"));

        ec.checkThat(codec.decode(StringUtils.repeat("%2B", 1024)),
                is(StringUtils.repeat("+", 1024)));

        ec.checkThat(codec.decode("an%effect"), is("an\ufffdfect")); // possibly accidential decoding

        ec.checkThat(isocodec.decode("%E4"), is("ä"));
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
