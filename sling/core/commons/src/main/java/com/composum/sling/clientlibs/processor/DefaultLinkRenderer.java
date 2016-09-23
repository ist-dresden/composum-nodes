package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibKey;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Component(
        label = "Clientlib Link Processor",
        description = "Renders collections of link tags.",
        immediate = true
)
@Service
public class DefaultLinkRenderer implements LinkRenderer {

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context)
            throws IOException {
        renderClientlibLinks(clientlib, properties, writer, context, clientlibConfig.getLinkTemplate());
    }

    public void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                                     Writer writer, RendererContext context,
                                     String template)
            throws IOException {
        List<ClientlibLink> links = clientlib.getLinks(context, true);
        for (int i = 0; i < links.size(); ) {
            ClientlibLink link = links.get(i);
            String rel = link.properties.get(ClientlibKey.PROP_REL);
            ;
            writer.append(MessageFormat.format(template, link.getUrl(context), rel != null ? rel : ""));
            if (++i < links.size()) {
                writer.append('\n');
            }
        }
    }
}
