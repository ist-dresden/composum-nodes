package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibKey;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

@Component(
        label = "Clientlib Link Processor",
        description = "Renders collections of link tags.",
        immediate = true,
        metatype = true
)
@Service
public class DefaultLinkRenderer implements LinkRenderer {

    public static final String DEFAULT_TEMPLATE = "  <link rel=\"{1}\" href=\"{0}\" />";
    public static final String TEMPLATE = "link.template";
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
        List<ClientlibLink> links = clientlib.getLinks(context, true);
        for (int i = 0; i < links.size(); ) {
            ClientlibLink link = links.get(i);
            String rel = link.properties.get(ClientlibKey.PROP_REL);;
            writer.append(MessageFormat.format(template, link.getUrl(context), rel != null ? rel : ""));
            if (++i < links.size()) {
                writer.append('\n');
            }
        }
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        template = PropertiesUtil.toString(properties.get(TEMPLATE), DEFAULT_TEMPLATE);
    }
}
