package com.composum.sling.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
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
     * Matches one or several encoded characters.
     */
    public static final Pattern ENCODED_CHARACTERS = Pattern.compile("(%([0-9a-fA-F][0-9a-fA-F]))+");
    public static final Pattern INVALID_ENCODED_CHARACTER = Pattern.compile("%(?![0-9a-fA-F][0-9a-fA-F]).{0,2}");
    public static final String INVALID_CHARACTER_MARKER = "\ufffd";

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
        this.invalidCharRegex = Pattern.compile("[^" + admissibleCharacters + "]");
        if (invalidCharRegex.matcher("%").matches()) {
            throw new IllegalArgumentException("Quoting character '%' is not admissible.");
        }
    }

    @Nullable
    public String encode(@Nullable String encoded) {
        throw new UnsupportedOperationException("Not implemented yet."); // FIXME hps 17.06.20 not implemented
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
        if (encoded == null || !encoded.contains("%")) {
            return encoded;
        }
        if (doThrow) {
            Matcher fail = INVALID_ENCODED_CHARACTER.matcher(encoded);
            if (fail.find()) {
                throw new IllegalArgumentException("Invalid encoded character " + fail.group());
            }
        }
        Matcher m = ENCODED_CHARACTERS.matcher(encoded);
        CharBuffer out = CharBuffer.allocate(encoded.length());
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
                for (int i = m.start() + 1; i < m.end(); i += 3) {
                    bytes.put((byte) (16 * unhex(encoded.charAt(i)) + unhex(encoded.charAt(i + 1))));
                }
                charsetDecoder.reset();
                bytes.flip();
                CoderResult result = charsetDecoder.decode(bytes, out, true);
                checkResult(encoded, doThrow, out, result);
                result = charsetDecoder.flush(out);
                checkResult(encoded, doThrow, out, result);
                bytes.clear();
            }
            out.append(encoded, appended, encoded.length());
        } catch (BufferOverflowException e) {
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
