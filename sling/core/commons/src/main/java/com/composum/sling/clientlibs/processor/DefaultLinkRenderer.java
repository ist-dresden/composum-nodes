package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(
        label = "Clientlib Link Processor",
        description = "Renders collections of link tags.",
        immediate = true
)
@Service
public class DefaultLinkRenderer extends AbstractClientlibRenderer implements LinkRenderer {

    @Reference
    protected ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getLinkTemplate();
    }

}
