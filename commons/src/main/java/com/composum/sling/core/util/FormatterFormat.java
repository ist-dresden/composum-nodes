package com.composum.sling.core.util;

import org.jetbrains.annotations.NotNull;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Formatter;
import java.util.Locale;

/**
 * a simple mapper to use a string formatter as value format
 */
public class FormatterFormat extends Format {

    public final Locale locale;
    public final String format;

    public FormatterFormat(@NotNull final String format, @NotNull final Locale locale) {
        this.locale = locale;
        this.format = format;
    }

    @Override
    public StringBuffer format(final Object obj,
                               @NotNull final StringBuffer toAppendTo, @NotNull final FieldPosition pos) {
        if (obj != null) {
            Formatter formatter = new Formatter(toAppendTo, locale);
            formatter.format(format, obj);
        }
        return toAppendTo;
    }

    @Override
    public Object parseObject(final String source, @NotNull final ParsePosition pos) {
        throw new UnsupportedOperationException("output formatter only");
    }
}
