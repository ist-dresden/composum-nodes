package com.composum.sling.test.util;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a couple of teststrings to detect character escaping problems. This file encodes all chars numerically to
 * make sure we are independent of the encoding of this file. Caution: this currently covers only relatively common
 * stuff - it does not yet lead into the deeper realms of UTF like substitutes, right to left, Kanji, characters
 * outside of 16 bit.
 */
public class CharsetStress {

    /**
     * Returns a test string usable to detect character encoding problems, including some HTML and XML constructs
     * usable to detect missing HTML encoding. Contains some chars which do not belong to ISO-8859-1. (81 characters)
     * <p>
     * äöüÄ\"'ÖÜñóáéíóú¬áßàèìùòâêîôû &<&>xml; &euro; @%‰ ¼½¾ «™©®» „$”“€”‘£’‚¥’ <b>!</b>·
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
                    98, 62, -62, -73}, "UTF8");
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

    /**
     * Arabic for "hallo". <p>مرحبا</p>
     */
    public static String getArabic() {
        return fromBytes("[-39, -123, -40, -79, -40, -83, -40, -88, -40, -89]");
    }

    /**
     * Russian for "hallo". <p>Привет</p>
     */
    public static String getRussian() {
        return fromBytes("[-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126]");
    }

    /**
     * Chinesisch for "hallo". <p>你好</p>
     */
    public static String getChinese() {
        return fromBytes("[-28, -67, -96, -27, -91, -67]");
    }

    /**
     * Japanisch for "hallo". <p>こんにちは</p>
     */
    public static String getJapanese() {
        return fromBytes("[-29, -127, -109, -29, -126, -109, -29, -127, -85, -29, -127, -95, -29, -127, -81]");
    }

    /**
     * A string saying hello in various languages with more challenging charsets.
     * <p>¡Hola! היי! Γεια! Привет مرحبا 你好 こんにちは</p>
     */
    public static String getVariousLanguages() {
        return fromBytes("[-62, -95, 72, 111, 108, 97, 33, 32, -41, -108, -41, -103, -41, -103, 33, 32, -50, -109, -50, -75, -50, -71, -50, -79, 33]") + " " + getRussian() + " " + getArabic() + " " + getChinese() + " " + getJapanese();
    }

    /**
     * Characters that are meta-characters in various contexts: URLs / HTTP, JCR, ...
     * <p>:_?&#@%[](){}<$>!'"*+,;=-/|\</p>
     */
    public static String getMetachars() {
        return fromBytes("[58, 95, 63, 38, 35, 64, 37, 91, 93, 40, 41, 123, 125, 60, 36, 62, 33, 39, 34, 42, 43, 44, 59, 61, 45, 47, 124, 92]");
    }

    /**
     * Returns a textual representation of the bytes of a string to compare to,
     * in case there is unexplainable trouble with source file charsets.
     */
    @Nonnull
    public static String bytes(@Nullable String string) {
        if (string == null) {
            return "<null>";
        }
        StringBuilder buf = new StringBuilder("[");
        for (byte b : string.getBytes(StandardCharsets.UTF_8)) {
            if (buf.length() > 1) {
                buf.append(", ");
            }
            buf.append((int) b);
        }
        return buf.append("]").toString();
    }

    /**
     * Inverse of {@link #bytes(String)}.
     */
    @Nullable
    public static String fromBytes(@Nonnull String bytesString) {
        if ("<null>".equals(bytesString)) {
            return null;
        }
        if (!StringUtils.startsWith(bytesString, "[") || !StringUtils.endsWith(bytesString, "]")) {
            throw new IllegalArgumentException("Wrong format of " + bytesString);
        }
        ByteBuffer buf = ByteBuffer.allocate(StringUtils.countMatches(bytesString, ",") + 1);
        for (String b : bytesString.substring(1, bytesString.length() - 1).split("\\s*,\\s*")) {
            buf.put(Byte.parseByte(b));
        }
        return new String(buf.array(), StandardCharsets.UTF_8);
    }

    /**
     * Removes all chars from a string that are not permitted as XML characters. Actually, we also remove characters
     * which authors should avoid, and restrict ourselves to 16bit.<p>
     * Permitted: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] , but suggested to avoid:
     * [#x7F-#x84], [#x86-#x9F], [#xFDD0-#xFDEF]
     *
     * @see "https://www.w3.org/TR/xml/#NT-Char"
     */
    public static String xmlPermissible(@Nullable String string) {
        String res = string;
        if (res != null && !res.isEmpty()) {
            res = res.replaceAll("[^\\t\\n\\r\\x20-\\uD7FF\\uE000-\\uFFFD]+", "");
            res = res.replaceAll("[\\x7F-\\x84\\x86-\\x9F\\uFDD0-\\uFDEF]+", "");
        }
        return res;
    }

    /**
     * NameStartChar from https://www.w3.org/TR/xml/#NT-Char .
     * NameStartChar	   ::=   	":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
     */
    protected static final String NAMESTARTCHAR = ":A-Z_a-z\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD";

    /**
     * NameStartChar from https://www.w3.org/TR/xml/#NT-Char . NameChar	   ::=   	NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
     */
    protected static final String NAMECHAR = "-" + NAMESTARTCHAR + ".0-9\\xB7\\u0300-\\u036F\\u203F-\\u2040";

    /**
     * Removes all chars from a string that are not permitted as XML names.
     */
    public static String xmlNodenamePermissible(@Nullable String string) {
        String res = xmlPermissible(string);
        if (res != null && !res.isEmpty()) {
            res = res.replaceAll("[^" + NAMECHAR + "]+", "");
        }
        return res;
    }

    /**
     * Removes all characters from a string that are not permitted as JCR nodename.
     * If you want it to be encoded, pipe it through {@link org.apache.jackrabbit.util.ISO9075#encode(String)}
     * {@link #xmlPermissible(String)} except '/' | ':' | '[' | ']' | '|' | '*'
     * We also do not admit ; which is a separator, and &amp; &lt; &gt; &quot; since these are quoted by the XSS filter.
     *
     * @see "https://docs.adobe.com/docs/en/spec/jcr/2.0/3_Repository_Model.html"
     */
    public static String jcrNodename(@Nullable String string) {
        String res = xmlPermissible(string);
        if (res != null && !res.isEmpty()) {
            res = res.replaceAll("[/:\\[\\]|*]+", "")
                    .replaceAll("[;<>\"&]+", ""); // remove additional problematic chars
        }
        return res;
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
