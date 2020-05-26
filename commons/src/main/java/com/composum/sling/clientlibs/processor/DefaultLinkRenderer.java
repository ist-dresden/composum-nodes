package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Clientlib Link Processor"
        }
)
public class DefaultLinkRenderer extends AbstractClientlibRenderer implements LinkRenderer {

    @Reference
    protected ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getConfig().template_link_general();
    }

}
