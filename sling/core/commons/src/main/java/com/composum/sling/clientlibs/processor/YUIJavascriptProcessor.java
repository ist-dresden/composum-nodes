package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.core.util.ResourceUtil;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
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
        label = "Clientlib Javascript Processor (YUI)",
        description = "Delivers Javascript content bundled and minimized.",
        immediate = true
)
@Service
public class YUIJavascriptProcessor implements JavascriptProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUIJavascriptProcessor.class);

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context)
            throws IOException {
        renderClientlibLinks(clientlib, properties, writer, context, clientlibConfig.getJavascriptTemplate());
    }

    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context,
                                     String template)
            throws IOException {
        List<ClientlibLink> links = clientlib.getLinks(context, clientlibConfig.getJavascriptDebug());
        for (int i = 0; i < links.size(); ) {
            ClientlibLink link = links.get(i);
            writer.append(MessageFormat.format(template, link.getUrl(context)));
            if (++i < links.size()) {
                writer.append('\n');
            }
        }
    }

    @Override
    public InputStream processContent(final Clientlib clientlib, final InputStream source, ProcessorContext context)
            throws IOException {
        InputStream result = source;
        if (source != null) {
            context.hint(ResourceUtil.PROP_MIME_TYPE, "application/javascript");
            if (clientlibConfig.getJavascriptMinimize()) {
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                             InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET)) {
                            final Reporter errorReporter = new Reporter(clientlib);
                            final JavaScriptCompressor compressor =
                                    new JavaScriptCompressor(sourceReader, errorReporter);
                            compressor.compress(
                                    writer,
                                    clientlibConfig.getJavascriptLineBreak(),
                                    clientlibConfig.getJavascriptMunge(),
                                    false,
                                    true,
                                    !clientlibConfig.getJavascriptOptimize());
                            writer.flush();
                            writer.close();
                        } catch (IOException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                });
            }
        }
        return result;
    }

    protected class Reporter implements ErrorReporter {

        protected final Clientlib clientlib;

        public Reporter(Clientlib clientlib) {
            this.clientlib = clientlib;
        }

        public String getMessage(String message, int line, String lineSource, int lineOffset) {
            return clientlib.getPath() + ": " + message + " - line: " + line + " '"
                    + lineSource + "' (" + lineOffset + ")";
        }

        @Override
        public void warning(String message, String sourceName, int line,
                            String lineSource, int lineOffset) {
            LOG.warn(getMessage(message, line, lineSource, lineOffset));
        }

        @Override
        public void error(String message, String sourceName, int line,
                          String lineSource, int lineOffset) {
            LOG.error(getMessage(message, line, lineSource, lineOffset));
        }

        @Override
        public EvaluatorException runtimeError(String message, String sourceName, int line,
                                               String lineSource, int lineOffset) {
            return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
        }
    }
}
