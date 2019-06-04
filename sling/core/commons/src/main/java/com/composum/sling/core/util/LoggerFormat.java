package com.composum.sling.core.util;

import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nonnull;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * a simple mapper to use a logging formatter as value format
 */
public class LoggerFormat extends Format {

    public final String format;

    public LoggerFormat(@Nonnull final String format) {
        this.format = format;
    }

    @Override
    public StringBuffer format(final Object obj,
                               @Nonnull final StringBuffer toAppendTo, @Nonnull final FieldPosition pos) {
        if (obj != null) {
            toAppendTo.append(MessageFormatter.format(format, obj).getMessage());
        }
        return toAppendTo;
    }

    @Override
    public Object parseObject(final String source, @Nonnull final ParsePosition pos) {
        throw new UnsupportedOperationException("output formatter only");
    }

    public static String format(String message, Object... values) {
        return MessageFormatter.arrayFormat(message, values).getMessage();
    }
}
