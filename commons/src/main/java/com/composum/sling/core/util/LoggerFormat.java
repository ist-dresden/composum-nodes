package com.composum.sling.core.util;

import org.slf4j.helpers.MessageFormatter;

import org.jetbrains.annotations.NotNull;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * a simple mapper to use a logging formatter as value format
 */
public class LoggerFormat extends Format {

    public final String format;

    public LoggerFormat(@NotNull final String format) {
        this.format = format;
    }

    @Override
    public StringBuffer format(final Object obj,
                               @NotNull final StringBuffer toAppendTo, @NotNull final FieldPosition pos) {
        if (obj != null) {
            toAppendTo.append(MessageFormatter.format(format, obj).getMessage());
        }
        return toAppendTo;
    }

    @Override
    public Object parseObject(final String source, @NotNull final ParsePosition pos) {
        throw new UnsupportedOperationException("output formatter only");
    }

    public static String format(String message, Object... values) {
        return MessageFormatter.arrayFormat(message, values).getMessage();
    }
}
