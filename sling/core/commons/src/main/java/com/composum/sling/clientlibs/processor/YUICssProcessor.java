package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibLink;
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
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Component(
        label = "Clientlib CSS Processor (YUI)",
        description = "Delivers CSS content bundled and minimized.",
        immediate = true
)
@Service
public class YUICssProcessor implements CssProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUICssProcessor.class);

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context)
            throws IOException {
        renderClientlibLinks(clientlib, properties, writer, context, clientlibConfig.getCssTemplate());
    }

    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context,
                                     String template)
            throws IOException {
        List<ClientlibLink> links = clientlib.getLinks(context, clientlibConfig.getDebug());
        for (int i = 0; i < links.size(); ) {
            ClientlibLink link = links.get(i);
            writer.append(MessageFormat.format(template, link.getUrl(context)));
            if (++i < links.size()) {
                writer.append('\n');
            }
        }
    }

    @Override
    public InputStream processContent(Clientlib clientlib, final InputStream source, ProcessorContext context)
            throws IOException {
        InputStream result = source;
        if (source != null) {
            context.hint(ResourceUtil.PROP_MIME_TYPE, "text/css");
            if (context.useMinifiedFiles() && clientlibConfig.getCssMinimize()) {
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                             InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET)) {
                            final CssCompressor compressor = new CssCompressor(sourceReader);
                            compressor.compress(writer, clientlibConfig.getCssLineBreak());
                            writer.flush();
                        } catch (IOException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                });
            }
        }
        return result;
    }
}
