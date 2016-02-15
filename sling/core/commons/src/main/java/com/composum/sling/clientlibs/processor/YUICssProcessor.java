package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.core.util.ResourceUtil;
import com.yahoo.platform.yui.compressor.CssCompressor;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
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
        label = "Clientlib CSS Processor (YUI)",
        description = "Delivers CSS content bundled and minimized.",
        immediate = true,
        metatype = true
)
@Service
public class YUICssProcessor implements CssProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(YUICssProcessor.class);

    public static final String DEBUG = "css.debug";
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
            label = "Mimimize",
            description = "compress with VUI compressor (if not 'debug' set)",
            boolValue = true
    )
    protected boolean minimize;

    public static final String LINEBREAK = "css.lineBreak";
    @Property(
            name = LINEBREAK,
            label = "Line Break",
            description = "length of compressed source lines (if not 'debug' set)",
            intValue = 0
    )
    protected int lineBreak;

    public static final String DEFAULT_TEMPLATE = "  <link rel=\"stylesheet\" href=\"{0}\" />";
    public static final String TEMPLATE = "css.template";
    @Property(
            name = TEMPLATE,
            label = "Template",
            description = "the HTML template for clientlib rendering",
            value = DEFAULT_TEMPLATE
    )
    protected String template;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties, Writer writer)
            throws IOException {
        renderClientlibLinks(clientlib, properties, writer, template);
    }

    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties, Writer writer,
                                     String template)
            throws IOException {
        List<Clientlib.Link> links = clientlib.getLinks(debug, properties);
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
            context.hint(ResourceUtil.PROP_MIME_TYPE, "text/css");
            if (minimize) {
                final InputStreamReader sourceReader = new InputStreamReader(source, DEFAULT_CHARSET);
                final PipedOutputStream outputStream = new PipedOutputStream();
                result = new PipedInputStream(outputStream);
                final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                final CssCompressor compressor = new CssCompressor(sourceReader);
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            compressor.compress(writer, lineBreak);
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

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        debug = PropertiesUtil.toBoolean(properties.get(DEBUG), false);
        minimize = PropertiesUtil.toBoolean(properties.get(MINIMIZE), true);
        lineBreak = PropertiesUtil.toInteger(properties.get(LINEBREAK), 0);
        template = PropertiesUtil.toString(properties.get(TEMPLATE), DEFAULT_TEMPLATE);
    }
}
