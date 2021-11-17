package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Codecs for the various URL parts.
 * Unlike {@link org.apache.commons.codec.net.URLCodec} this is focused on Strings
 * and thus the decoder can leave unknown characters untouched: "ä%C3%A4"
 * is decoded to "ää" instead of "?ä" as {@link org.apache.commons.codec.net.URLCodec#decode(String)} would do.
 */
public class UrlCodec {

    private static final Logger LOG = LoggerFactory.getLogger(UrlCodec.class);

    /**
     * The characters which can always appear in any URL without being encoded: the <a href="https://tools.ietf.org/html/rfc3986#section-2.3">"unreserved"
     * chars.</a> Unfortunately there are different recommendations about encoding $!*'(), so we exclude them.
     * Possibly we could include the "extra" chars !*'(), . We exlude ~ since it was declared unsafe in <a href="https://tools.ietf.org/html/rfc1738>RFC 1738</a>.
     */
    protected static final String PART_URL_SAFECHARS = "-0-9a-zA-Z._+";

    /**
     * Codec quoting everything other than the chars which are safe in every part of the URL.
     */
    public static final UrlCodec URLSAFE = new UrlCodec(PART_URL_SAFECHARS, StandardCharsets.UTF_8);

    /**
     * Codec for the <a href="https://tools.ietf.org/html/rfc3986#section-3.3">path part</a> of an URL.
     */
    public static final UrlCodec PATH = new UrlCodec(PART_URL_SAFECHARS + "/:@" +
            "!$&'()*+,=" // the sub-delims  https://tools.ietf.org/html/rfc3986#section-2.2
            // We omit ; which is admissible in JCR resources, but is removed from the suffix in
            // SlingPathInfo if not quoted.
            , StandardCharsets.UTF_8);

    /**
     * Codec for the <a href="https://tools.ietf.org/html/rfc3986#section-3.2.1">authority</a> of an URL.
     */
    public static final UrlCodec AUTHORITY = new UrlCodec(PART_URL_SAFECHARS +
            "!$&'()*+,;=" // sub-delims
            , StandardCharsets.UTF_8);

    /**
     * Codec for the <a href="https://tools.ietf.org/html/rfc3986#section-3.4">query part</a> of an URL.
     */
    public static final UrlCodec QUERYPART = new UrlCodec(PART_URL_SAFECHARS + "/:@"
            + "!$'()*,;" // the sub-delims, we exclude & and = since they are meta-characters here, and & of course
            // also + since that encodes space and would have to be handled differently, and & of course
            , StandardCharsets.UTF_8) {

        @Override
        protected String charsToEncode(String admissibleCharacters) {
            return admissibleCharacters.replaceAll("\\+", " ");
        }

        @Override
        protected void encodePostprocess(StringBuffer out) {
            for (int i = 0; i < out.length(); ++i) {
                if (out.charAt(i) == ' ') {
                    out.setCharAt(i, '+');
                }
            }
        }

        @Override
        protected String decodePreprocess(String encoded) {
            return StringUtils.replaceChars(encoded, '+', ' ');
        }
    };

    /**
     * Codec for the <a href="https://tools.ietf.org/html/rfc3986#section-3.5">fragment part</a> of an URL.
     */
    public static final UrlCodec FRAGMENT = new UrlCodec(PART_URL_SAFECHARS + "!$&'()*+,;=" +
            "/?:@", StandardCharsets.UTF_8);

    /**
     * Codec for opaque URLs that are not parsed. Contains all unreserved, reserved and extra characters
     */
    public static final UrlCodec OPAQUE = new UrlCodec(PART_URL_SAFECHARS + "$.!*'()," + ";/?:@=&", StandardCharsets.UTF_8);

    /**
     * Matches one or several percent encoded bytes.
     */
    protected static final Pattern PAT_ENCODED_CHARACTERS = Pattern.compile("(%[0-9a-fA-F][0-9a-fA-F])+");

    /**
     * Matches a percent sign followed by something that's not a hexadecimally encoded byte.
     */
    protected static final Pattern PAT_INVALID_ENCODED_CHARACTER = Pattern.compile("%(?![0-9a-fA-F][0-9a-fA-F]).{0,2}");

    /**
     * {@value #INVALID_CHARACTER_MARKER} is inserted whenever something could not be decoded,
     * or sometimes when it's encoded - see {@link #encode(String)}.
     */
    protected static final String INVALID_CHARACTER_MARKER = "\ufffd";

    protected static final String HEXDIGITS = "0123456789ABCDEF";

    protected final Charset charset;

    protected final String admissibleCharacters;

    /**
     * Matches one or more characters not in the {@link #admissibleCharacters}.
     */
    protected final Pattern charsToEncodeRegex;
    /**
     * Matches an arbitrarily long sequence of admissible chars and percent encodings.
     */
    protected final Pattern validationRegex;

    protected transient String invalidCharacterMarkerForEncoding;

    /**
     * Initializes the Codec with a range of admissible characters.
     *
     * @param admissibleCharacters all characters that remain untouched when encoding, can contain ranges like a-z in simple regex character classes. (Thus, - has to be first or last character if it needs to be included. Obviously, the quoting character '%' always has to be admissible.
     * @param charset              the charset needed for the decoder.
     * @throws IllegalArgumentException if the admissibleCharacters don't contain '%'
     * @throws PatternSyntaxException   if the admissibleCharacters are not a well formed character class
     */
    public UrlCodec(@NotNull String admissibleCharacters, @NotNull Charset charset) throws IllegalArgumentException, PatternSyntaxException {
        this.charset = Objects.requireNonNull(charset);
        this.admissibleCharacters = Objects.requireNonNull(admissibleCharacters);
        this.charsToEncodeRegex = Pattern.compile("([^" + charsToEncode(admissibleCharacters) + "])+");
        if (!charsToEncodeRegex.matcher("%").matches()) {
            throw new IllegalArgumentException("Quoting character '%' cannot be admissible.");
        }
        this.validationRegex = Pattern.compile("([" + admissibleCharacters + "]|%[0-9a-fA-F][0-9a-fA-F])*");
    }

    /**
     * Hook to calculate the set of characters to encode from the admissibleCharacters
     */
    protected String charsToEncode(String admissibleCharacters) {
        return admissibleCharacters;
    }

    /**
     * Encodes all characters which are not admissible to percent-encodings wrt. the given charset.
     * If characters are not in the charset, they will silently be encoded as a replacement character,
     * which is either {@value #INVALID_CHARACTER_MARKER} or '?' if one of these is admissible, or the encoding
     * of {@value #INVALID_CHARACTER_MARKER} for the charset (which might be an encoded '?').
     */
    @Nullable
    public String encode(@Nullable String encoded) {
        return encode(encoded, false);
    }

    /**
     * Encodes all characters which are not admissible to percent-encodings wrt. the given charset.
     * If characters are not in the charset, we will throw an {@link IllegalArgumentException}.
     *
     * @throws IllegalArgumentException if a character cannot be encoded
     */
    @Nullable
    public String encodeValidated(@Nullable String encoded) throws IllegalArgumentException {
        return encode(encoded, true);
    }

    /**
     * Decodes a percent encoded characters in the string, never throwing exceptions: if an undecodeable
     * character is encountered it's replaced with the replacement character {@value #INVALID_CHARACTER_MARKER}.
     * The only exception we make here is that a % sign without a hexadecimal number is passed through unchanged,
     * so that this can be used to preventively decode strings that might be encoded - which is not 100% safe, though, since there might been something looking like a % encoded character: e.g. "an%effect" will be decoded to "an\ufffdfect".
     */
    @Nullable
    public String decode(@Nullable String encoded) {
        return decode(encoded, false);
    }

    @Nullable
    protected String encode(@Nullable String encoded, boolean doThrow) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }
        Matcher matcher = charsToEncodeRegex.matcher(encoded);
        ByteBuffer bytes = ByteBuffer.allocate(100);
        CharsetEncoder charsetEncoder = charset.newEncoder();
        StringBuffer out = new StringBuffer();
        while (matcher.find()) { // found some not admissible characters we need to encode
            matcher.appendReplacement(out, "");

            CharSequence match = encoded.subSequence(matcher.start(), matcher.end());
            CharBuffer matchBuffer;
            boolean overflow, error = true;
            do {
                bytes.clear();
                charsetEncoder.reset();
                matchBuffer = CharBuffer.wrap(match);
                CoderResult result1 = charsetEncoder.encode(matchBuffer, bytes, true);
                CoderResult result2 = charsetEncoder.flush(bytes);
                overflow = result1.isOverflow() || result2.isOverflow();
                error = result1.isError() || result2.isError();

                if (overflow) { // enlarge byte buffer and try again
                    bytes = ByteBuffer.allocate((int) Math.max(2 * bytes.capacity(),
                            match.length() * charsetEncoder.maxBytesPerChar() * 1.2
                    ));
                }
            } while (overflow);

            // percent encode the bytes encoded from the not admissible characters
            bytes.flip().rewind();
            writePercentEncoded(bytes, out);

            if (error) {
                LOG.debug("Could not encode {} to {}", matcher.group(), charset.name());
                if (doThrow) {
                    throw new IllegalArgumentException("Could not encode " + matcher.group());
                } else {
                    out.append(StringUtils.repeat(getInvalidCharacterMarkerForEncoding(),
                            matcher.end() - matcher.start() - matchBuffer.position()));
                }
            }
        }
        matcher.appendTail(out);
        encodePostprocess(out);
        return out.toString();
    }

    /**
     * Hook for finalizing encoding
     */
    protected void encodePostprocess(StringBuffer out) {
    }

    protected void writePercentEncoded(ByteBuffer bytes, StringBuffer out) {
        while (bytes.hasRemaining()) {
            int b = (bytes.get() + 0x100) & 0xff;
            out.append('%')
                    .append(HEXDIGITS.charAt(b / 0x10))
                    .append(HEXDIGITS.charAt(b % 0x10));
        }
    }

    /**
     * To mark characters that could not properly be encoded, we use {@value #INVALID_CHARACTER_MARKER} or ? if
     * one of these is admissible, or {@value #INVALID_CHARACTER_MARKER} encoded if that belongs to the charset, or ? encoded if
     * it's not.
     */
    protected String getInvalidCharacterMarkerForEncoding() {
        if (invalidCharacterMarkerForEncoding == null) {
            if (!charsToEncodeRegex.matcher(INVALID_CHARACTER_MARKER).matches()) {
                invalidCharacterMarkerForEncoding = INVALID_CHARACTER_MARKER;
            } else if (!charsToEncodeRegex.matcher("?").matches()) {
                invalidCharacterMarkerForEncoding = "?";
            } else {
                ByteBuffer byteBuffer = charset.encode(INVALID_CHARACTER_MARKER);
                StringBuffer buf = new StringBuffer();
                writePercentEncoded(byteBuffer, buf);
                invalidCharacterMarkerForEncoding = buf.toString();
            }
        }
        return invalidCharacterMarkerForEncoding;
    }

    /**
     * Decodes percent encoded characters in the string but throws an {@link IllegalArgumentException} if the input string is invalid:
     * if it contains an unencoded quoting character % recognizable because it is not followed by a 2 digit hexadecimal number or it does not encode a character in the charset.
     *
     * @throws IllegalArgumentException if encoded is not a validly encoded String
     */
    @Nullable
    public String decodeValidated(@Nullable String encoded) throws IllegalArgumentException {
        return decode(encoded, true);
    }

    @Nullable
    protected String decode(@Nullable String encoded, boolean doThrow) throws IllegalArgumentException {
        encoded = decodePreprocess(encoded);
        if (encoded == null || encoded.isEmpty() || !encoded.contains("%")) {
            return encoded;
        }
        if (doThrow) {
            Matcher fail = PAT_INVALID_ENCODED_CHARACTER.matcher(encoded);
            if (fail.find()) {
                throw new IllegalArgumentException("Invalid encoded character " + fail.group());
            }
        }
        Matcher m = PAT_ENCODED_CHARACTERS.matcher(encoded);
        CharBuffer out = CharBuffer.allocate(encoded.length() + 100);
        ByteBuffer bytes = ByteBuffer.allocate(100);
        CharsetDecoder charsetDecoder = charset.newDecoder();
        int appended = 0;
        try {
            while (m.find()) {
                out.append(encoded, appended, m.start());
                appended = m.end();
                if (bytes.capacity() < (m.end() - m.start()) / 3) {
                    bytes = ByteBuffer.allocate(m.end() - m.start());
                }
                bytes.clear();
                for (int i = m.start() + 1; i < m.end(); i += 3) {
                    bytes.put((byte) (16 * unhex(encoded.charAt(i)) + unhex(encoded.charAt(i + 1))));
                }
                charsetDecoder.reset();
                bytes.flip();
                CoderResult result = charsetDecoder.decode(bytes, out, true);
                checkResult(encoded, doThrow, out, result);
                result = charsetDecoder.flush(out);
                checkResult(encoded, doThrow, out, result);
            }
            out.append(encoded, appended, encoded.length());
        } catch (BufferOverflowException e) { // impossible
            LOG.error("Bug: Buffer overflow in decoding {}", encoded, e);
            if (doThrow) {
                throw e;
            } else {
                return out.flip().toString() + INVALID_CHARACTER_MARKER;
            }
        }
        return out.flip().toString();
    }

    /**
     * Hook to preprocess something about to be decoded.
     */
    protected String decodePreprocess(String encoded) {
        return encoded;
    }

    protected void checkResult(@NotNull String encoded, boolean doThrow, CharBuffer out, CoderResult result) throws IllegalArgumentException {
        if (result.isError()) {
            if (doThrow) {
                try {
                    result.throwException();
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                out.put(INVALID_CHARACTER_MARKER);
            }
        }
        if (result.isOverflow()) {
            LOG.error("Bug: overflow when decoding {}", encoded);
        }
    }

    protected byte unhex(char c) {
        if (c >= '0' && c <= '9') return (byte) (c - '0');
        if (c >= 'a' && c <= 'f') return (byte) (10 + c - 'a');
        if (c >= 'A' && c <= 'F') return (byte) (10 + c - 'A');
        throw new IllegalArgumentException("Invalid hex char " + c);
    }

    /**
     * Verifies that the given String is encoded: all characters are admissible and % is always followed by a hexadecimal number.
     */
    public boolean isValid(@Nullable String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return true;
        }
        if (!validationRegex.matcher(encoded).matches()) {
            if (LOG.isDebugEnabled()) {
                Matcher m = validationRegex.matcher(encoded);
                if (m.lookingAt()) { // happens always
                    String invalidChars = StringUtils.abbreviate(encoded.substring(m.end()), 4);
                    LOG.debug("Inadmissible character(s) at {} in input {}", invalidChars, encoded);
                }
            }
            return false;
        }
        if (!encoded.contains("%")) {
            return true;
        }
        Matcher matcher = PAT_INVALID_ENCODED_CHARACTER.matcher(encoded);
        if (matcher.find()) {
            LOG.debug("Invalidly encoded character {} in input {}", matcher.group(), encoded);
            return false;
        }
        try { // check whether there are characters in there that do not belong to our charset
            decode(encoded, true);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UrlCodec{" +
                "charset=" + charset +
                ", admissibleCharacters='" + admissibleCharacters + '\'' +
                '}';
    }
}
