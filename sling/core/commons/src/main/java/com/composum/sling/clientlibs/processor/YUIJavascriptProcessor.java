package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.core.util.ResourceUtil;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.osgi.service.component.ComponentContext;
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
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

@Component(
        label = "Clientlib Javascript Processor (YUI)",
        description = "Delivers Javascript content bundled and minimized.",
        immediate = true,
        metatype = true
)
@Service
public class YUIJavascriptProcessor implements JavascriptProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUIJavascriptProcessor.class);

    public static final String DEBUG = "javascript.debug";
    @Property(
            name = DEBUG,
            label = "Debug",
            description = "let files unchanged and unbundled if set to 'true'",
            boolValue = false
    )
    protected boolean debug;

    public static final String MINIMIZE = "javascript.minimize";
    @Property(
            name = MINIMIZE,
            label = "Minimize",
            description = "compress with VUI compressor (if not 'debug' set)",
            boolValue = false
    )
    protected boolean minimize;

    public static final String MUNGE = "javascript.munge";
    @Property(
            name = MUNGE,
            label = "Munge",
            description = "munge javascript source code (if not 'debug' set)",
            boolValue = false
    )
    protected boolean munge;

    public static final String OPTIMIZE = "javascript.optimize";
    @Property(
            name = OPTIMIZE,
            label = "Optimize",
            description = "optimize javascript source code (if not 'debug' set",
            boolValue = false
    )
    protected boolean optimize;

    public static final String LINEBREAK = "javascript.lineBreak";
    @Property(
            name = LINEBREAK,
            label = "Line Break",
            description = "length of compressed source lines (if not 'debug' set)",
            intValue = 500
    )
    protected int lineBreak;

    public static final String DEFAULT_TEMPLATE = "  <script type=\"text/javascript\" src=\"{0}\"></script>";
    public static final String TEMPLATE = "javascript.template";
    @Property(
            name = TEMPLATE,
            label = "Template",
            description = "the HTML template for clientlib rendering",
            value = DEFAULT_TEMPLATE
    )
    protected String template;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context)
            throws IOException {
        renderClientlibLinks(clientlib, properties, writer, context, template);
    }

    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context,
                                     String template)
            throws IOException {
        List<Clientlib.Link> links = clientlib.getLinks(debug, false, false, properties, context);
        for (int i = 0; i < links.size(); ) {
            Clientlib.Link link = links.get(i);
            writer.append(MessageFormat.format(template, link.url));
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
            context.hint(ResourceUtil.PROP_MIME_TYPE, "application/javascript");
            if (minimize) {
                final InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET);
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                final Reporter errorReporter = new Reporter(clientlib);
                final JavaScriptCompressor compressor = new JavaScriptCompressor(sourceReader, errorReporter);
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            compressor.compress(writer, lineBreak, munge, false, true, !optimize);
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

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        debug = PropertiesUtil.toBoolean(properties.get(DEBUG), false);
        minimize = PropertiesUtil.toBoolean(properties.get(MINIMIZE), false);
        munge = PropertiesUtil.toBoolean(properties.get(MUNGE), false);
        optimize = PropertiesUtil.toBoolean(properties.get(OPTIMIZE), false);
        lineBreak = PropertiesUtil.toInteger(properties.get(LINEBREAK), 500);
        template = PropertiesUtil.toString(properties.get(TEMPLATE), DEFAULT_TEMPLATE);
    }
}
