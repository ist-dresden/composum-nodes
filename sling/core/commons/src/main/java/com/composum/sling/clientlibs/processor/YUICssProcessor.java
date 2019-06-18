package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.core.util.ResourceUtil;
import com.yahoo.platform.yui.compressor.CssCompressor;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Component(
        label = "Clientlib CSS Processor (YUI)",
        description = "Delivers CSS content bundled and minimized.",
        immediate = true
)
@Service
public class YUICssProcessor extends AbstractClientlibRenderer implements CssProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUICssProcessor.class);

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getCssTemplate();
    }

    @Override
    public InputStream processContent(final InputStream source, ProcessorContext context)
            throws IOException {
        InputStream result = source;
        if (source != null) {
            context.hint(ResourceUtil.PROP_MIME_TYPE, "text/css");
            if (context.useMinifiedFiles() && clientlibConfig.getCssMinimize()) {
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                context.execute(() -> {
                    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                         InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET)) {
                        final CssCompressor compressor = new CssCompressor(sourceReader);
                        compressor.compress(writer, clientlibConfig.getCssLineBreak());
                        writer.flush();
                    } catch (IOException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                });
            }
        }
        return result;
    }
}
