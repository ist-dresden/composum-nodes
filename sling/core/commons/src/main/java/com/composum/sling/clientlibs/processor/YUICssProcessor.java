package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.core.util.ResourceUtil;
import com.yahoo.platform.yui.compressor.CssCompressor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Clientlib CSS Processor (YUI)"
        }
)
public class YUICssProcessor extends AbstractClientlibRenderer implements CssProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUICssProcessor.class);

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getConfig().cssTemplate();
    }

    @Override
    public InputStream processContent(final InputStream source, ProcessorContext context)
            throws IOException {
        InputStream result = source;
        if (source != null) {
            context.hint(ResourceUtil.PROP_MIME_TYPE, "text/css");
            if (context.useMinifiedFiles() && clientlibConfig.getConfig().cssMinimize()) {
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                context.execute(() -> {
                    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                         InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET)) {
                        final CssCompressor compressor = new CssCompressor(sourceReader);
                        compressor.compress(writer, clientlibConfig.getConfig().cssLineBreak());
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
