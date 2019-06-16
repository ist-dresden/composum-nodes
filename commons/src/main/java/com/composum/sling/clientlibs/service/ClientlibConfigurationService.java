package com.composum.sling.clientlibs.service;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes (Core) Clientlib Configuration"
        }
)
@Designate(ocd = ClientlibConfiguration.Config.class)
public class ClientlibConfigurationService implements ClientlibConfiguration {

    protected Config config;

    @Override
    public Config getConfig() {
        return config;
    }

    @Modified
    @Activate
    protected void activate(final Config config) {
        this.config = config;
    }
}
