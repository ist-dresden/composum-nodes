package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
     * Matches one or several percent encoded bytes.
     */
    public static final Pattern ENCODED_CHARACTERS = Pattern.compile("(%([0-9a-fA-F][0-9a-fA-F]))+");

    /**
     * Matches a percent sign followed by something that's not a hexadecimally encoded byte.
     */
    public static final Pattern INVALID_ENCODED_CHARACTER = Pattern.compile("%(?![0-9a-fA-F][0-9a-fA-F]).{0,2}");

    /**
     * {@value #INVALID_CHARACTER_MARKER} is inserted whenever something could not be decoded.
     */
    public static final String INVALID_CHARACTER_MARKER = "\ufffd";

    protected static final String HEXDIGITS = "0123456789ABCDEF";

    protected final Charset charset;
    protected final String admissiblCharacters;
    protected final Pattern invalidCharRegex;

    /**
     * Initializes the Codec with a range of admissible characters.
     *
     * @param admissibleCharacters all characters that remain untouched when encoding, can contain ranges like a-z in simple regex character classes. (Thus, - has to be first or last character if it needs to be included. Obviously, the quoting character '%' always has to be admissible.
     * @param charset              the charset needed for the decoder.
     * @throws IllegalArgumentException if the admissibleCharacters don't contain '%'
     * @throws PatternSyntaxException   if the admissibleCharacters are not a well formed character class
     */
    public UrlCodec(@Nonnull String admissibleCharacters, @Nonnull Charset charset) throws IllegalArgumentException, PatternSyntaxException {
        this.charset = Objects.requireNonNull(charset);
        this.admissiblCharacters = Objects.requireNonNull(admissibleCharacters);
        this.invalidCharRegex = Pattern.compile("([^" + admissibleCharacters + "])+");
        if (invalidCharRegex.matcher("%").matches()) {
            throw new IllegalArgumentException("Quoting character '%' is not admissible.");
        }
    }

    /**
     * Encodes all characters which are not admissible to percent-encodings wrt. the given charset.
     * If characters are not in the charset, they will silently be encoded as '?'.
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
        Matcher m = invalidCharRegex.matcher(encoded);
        ByteBuffer bytes = ByteBuffer.allocate(100);
        CharsetEncoder charsetEncoder = charset.newEncoder();
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(out, "");
            CharSequence match = encoded.subSequence(m.start(), m.end());
            CharBuffer matchBuffer;
            boolean overflow, error = true;
            do {
                bytes.clear();
                charsetEncoder.reset();
                matchBuffer = CharBuffer.wrap(match);
                CoderResult result1 = charsetEncoder.encode(matchBuffer, bytes, true);
                CoderResult result2 = charsetEncoder.flush(bytes);
                overflow = result1.isOverflow() || result2.isOverflow();
                if (overflow) {
                    bytes = ByteBuffer.allocate((int) Math.max(2 * bytes.capacity(),
                            match.length() * charsetEncoder.maxBytesPerChar() * 1.2
                    ));
                } else {
                    error = result1.isError() || result2.isError();
                }
            } while (overflow);
            bytes.flip().rewind();
            while (bytes.hasRemaining()) {
                int b = (bytes.get() + 0x100) & 0xff;
                out.append('%')
                        .append(HEXDIGITS.charAt(b / 0x10))
                        .append(HEXDIGITS.charAt(b % 0x10));
            }
            if (error) {
                LOG.debug("Could not encode {} to {}", m.group(), charset.name());
                if (doThrow) {
                    throw new IllegalArgumentException("Could not encode " + m.group());
                } else { // TODO what to do??? This is likely not valid, but a '?' neither.
                    out.append(StringUtils.repeat(INVALID_CHARACTER_MARKER, m.end() - m.start() - matchBuffer.position()));
                }
            }
        }
        m.appendTail(out);
        return out.toString();
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
        if (encoded == null || encoded.isEmpty() || !encoded.contains("%")) {
            return encoded;
        }
        if (doThrow) {
            Matcher fail = INVALID_ENCODED_CHARACTER.matcher(encoded);
            if (fail.find()) {
                throw new IllegalArgumentException("Invalid encoded character " + fail.group());
            }
        }
        Matcher m = ENCODED_CHARACTERS.matcher(encoded);
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

    protected void checkResult(@Nonnull String encoded, boolean doThrow, CharBuffer out, CoderResult result) throws IllegalArgumentException {
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
        if (encoded == null) {
            return true;
        }
        Matcher matcher = invalidCharRegex.matcher(encoded);
        if (matcher.find()) {
            LOG.debug("Inadmissible character {} in input {}", matcher.group(), encoded);
            return false;
        }
        if (!encoded.contains("%")) {
            return true;
        }
        matcher = INVALID_ENCODED_CHARACTER.matcher(encoded);
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

}
