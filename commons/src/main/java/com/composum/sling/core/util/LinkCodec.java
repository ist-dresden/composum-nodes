package com.composum.sling.core.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * Replaced by {@link UrlCodec} with its various codecs for URL parts.
 *
 * @deprecated use {@link UrlCodec} codecs / {@link LinkUtil} methods
 */
// FIXME(hps,19.06.20) remove this
@Deprecated
public class LinkCodec extends URLCodec {

    private static final Logger LOG = LoggerFactory.getLogger(LinkCodec.class);

    protected static final BitSet SLING_PATH_SET = new BitSet(256);
    protected static final BitSet SLING_URL_SET = new BitSet(256);

    static {
        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            SLING_PATH_SET.set(i);
            SLING_URL_SET.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            SLING_PATH_SET.set(i);
            SLING_URL_SET.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            SLING_PATH_SET.set(i);
            SLING_URL_SET.set(i);
        }
        // special chars
        SLING_PATH_SET.set('-');
        SLING_URL_SET.set('-');
        SLING_PATH_SET.set('_');
        SLING_URL_SET.set('_');
        SLING_PATH_SET.set('.');
        SLING_URL_SET.set('.');
        SLING_PATH_SET.set('*');
        SLING_URL_SET.set('*');
        SLING_PATH_SET.set('/');
        SLING_URL_SET.set('/');
        SLING_URL_SET.set('@');
        SLING_URL_SET.set(':');
        SLING_URL_SET.set(';');
        SLING_URL_SET.set('?');
        SLING_URL_SET.set('&');
        SLING_URL_SET.set('=');
        SLING_URL_SET.set('#');
    }

    public LinkCodec() {
        super(StandardCharsets.UTF_8.name());
    }

    @Override
    public byte[] encode(byte[] bytes) {
        return encodeUrl(SLING_PATH_SET, bytes);
    }

    @Override
    public String encode(String value) {
        try {
            return super.encode(value);
        } catch (EncoderException ex) {
            LOG.error(ex.toString());
            return value;
        }
    }

    @Override
    public String decode(String value) {
        try {
            // FIXME(hps,19.06.20) + handling is broken.
            value = value.replaceAll("\\+", "%2B"); // keep '+' as is
            return super.decode(value);
        } catch (DecoderException ex) {
            LOG.error(ex.toString());
            return value;
        }
    }

    public String encodeUrl(String url) {
        if (url == null) {
            return null;
        }
        try {
            return StringUtils.newStringUsAscii(encodeUrl(SLING_URL_SET, url.getBytes(charset)));
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex.toString());
            return url;
        }
    }
}
