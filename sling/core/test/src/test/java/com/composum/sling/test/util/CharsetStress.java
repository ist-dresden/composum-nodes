package com.composum.sling.test.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a couple of teststrings to detect character escaping problems. This file encodes all chars numerically to
 * make sure we are independent of the encoding of this file.
 */
public class CharsetStress {

    /**
     * Returns a test string usable to detect character encoding problems, including some HTML and XML constructs
     * usable to detect missing HTML encoding. Contains some chars which do not belong to ISO-8859-1. (81 characters)
     * <p>
     * äöüÄ\"'ÖÜñóáéíóú¬áßàèìùòâêîôû &<&>xml; &euro; @%‰ ¼½¾ «™©®» „$”“€”‘£’‚¥’ <b>!</b>
     */
    public static String getUTF8CharsetStress() {
        try {
            return new String(new byte[]{-61, -92, -61, -74, -61, -68, -61, -124, 92, 34, 39, -61, -106, -61, -100,
                    -61, -79, -61, -77, -61, -95, -61, -87, -61, -83, -61, -77, -61, -70, -62, -84, -61, -95, -61, -97,
                    -61, -96, -61, -88, -61, -84, -61, -71, -61, -78, -61, -94, -61, -86, -61, -82, -61, -76, -61, -69,
                    32, 38, 60, 38, 62, 120, 109, 108, 59, 32, 38, 101, 117, 114, 111, 59, 32, 64, 37, -30, -128, -80,
                    32, -62, -68, -62, -67, -62, -66, 32, -62, -85, -30, -124, -94, -62, -87, -62, -82, -62, -69, 32,
                    -30, -128, -98, 36, -30, -128, -99, -30, -128, -100, -30, -126, -84, -30, -128, -99, -30, -128, -104,
                    -62, -93, -30, -128, -103, -30, -128, -102, -62, -91, -30, -128, -103, 32, 60, 98, 62, 33, 60, 47,
                    98, 62}, "UTF8");
        } catch (final UnsupportedEncodingException e) { // can't happen.
            throw new RuntimeException(e);
        }
    }

    /**
     * Contains 11 characters from {@link #getUTF8CharsetStress()} containeed in UTF-8 but not in ISO-8859-1:
     * <code>‰™„”“€”‘’‚’</code>
     */
    public static String getUTF8NotISO8859d1() {
        final String s1 = getUTF8CharsetStress();
        final String s2 = getFullISO8859d1Charset();
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s1.length(); ++i) {
            final char c = s1.charAt(i);
            if (0 > s2.indexOf(c)) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Contains 11 characters from {@link #getUTF8CharsetStress()} containeed in UTF-8 but not in ISO-8859-15:
     * <code>‰¼½¾™„”“”‘’‚’</code>
     */
    public static String getUTF8NotISO8859d15() {
        final String s1 = getUTF8CharsetStress();
        final String s2 = getFullISO8859d15Charset();
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s1.length(); ++i) {
            final char c = s1.charAt(i);
            if (0 > s2.indexOf(c)) {
                buf.append(c);
            }
        }
        return buf.toString();
    }


    /**
     * All 191 characters ISO-8859-1 from 32 until 255, that is, all non-control chars:<code>
     * !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz
     * {|}~ ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ
     * </code>
     */
    public static String getFullISO8859d1Charset() {
        try {
            final byte[] b = bytes32to255();
            return new String(b, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) { // can't happen.
            throw new RuntimeException(e);
        }
    }

    /**
     * All 191 characters ISO-8859-15 from 32 until 255, that is, all non-control chars:<code>
     * !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz
     * {|}~ ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ
     * </code>
     */
    public static String getFullISO8859d15Charset() {
        try {
            final byte[] b = bytes32to255();
            return new String(b, "ISO-8859-15");
        } catch (final UnsupportedEncodingException e) { // can't happen.
            throw new RuntimeException(e);
        }
    }

    private static byte[] bytes32to255() {
        final List<Byte> bytes = new ArrayList<Byte>();
        for (int i = 32; i < 127; ++i) {
            bytes.add((byte) i);
        }
        for (int i = 160; i < 256; ++i) {
            bytes.add((byte) i);
        }
        final byte[] b = new byte[bytes.size()];
        for (int i = 0; i < b.length; ++i) {
            b[i] = bytes.get(i);
        }
        return b;
    }

}
