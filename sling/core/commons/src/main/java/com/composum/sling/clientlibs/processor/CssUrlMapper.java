package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.service.ClientlibProcessor;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple processor to map URLs embedded in CSS files.
 */
public class CssUrlMapper implements ClientlibProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CssUrlMapper.class);

    public static final Pattern URL_PATTERN = Pattern.compile("(url\\s*\\(\\s*['\"]?)([^'\")]+)([\"']?\\s*\\))");

    @Override
    public InputStream processContent(Clientlib clientlib, final InputStream source, final ProcessorContext context)
            throws IOException {
        final PipedOutputStream outputStream = new PipedOutputStream();
        InputStream result = new PipedInputStream(outputStream);
        context.execute(new Runnable() {
            @Override
            public void run() {
                final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                try {
                    try {
                        String css = IOUtils.toString(source, DEFAULT_CHARSET);
                        map(css, writer, context);
                        writer.flush();
                        writer.close();
                    } catch (IOException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                } finally {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        return result;
    }

    public void map(String css, Writer writer, ProcessorContext context) {

        try {
            Matcher matcher = URL_PATTERN.matcher(css);
            int pos = 0;
            while (matcher.find(pos)) {
                writer.write(css, pos, matcher.start());
                writer.write(matcher.group(1));
                writer.write(map(context, matcher.group(2)));
                writer.write(matcher.group(3));
                pos = matcher.end();
            }
            writer.write(css, pos, css.length() - pos);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String map(ProcessorContext context, String url) {
        return LinkUtil.getUrl(context.getRequest(), url);
    }
}
