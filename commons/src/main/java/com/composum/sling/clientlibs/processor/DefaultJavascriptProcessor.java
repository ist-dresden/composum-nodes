package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.core.util.ResourceUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Since there is currently (as of mid-2017) no Java javascript minifier library that works sufficiently well, this does nothing
 * and relies on minified siblings for each javascript, generated e.g. with minify-maven-plugin. (Yui is not sufficient.)
 */
@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Clientlib Default Javascript Processor"
        }
)
public class DefaultJavascriptProcessor extends AbstractClientlibRenderer implements JavascriptProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJavascriptProcessor.class);

    @Reference
    protected ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getConfig().javascriptTemplate();
    }

    @Override
    public InputStream processContent(final InputStream source, ProcessorContext context) {
        context.hint(ResourceUtil.PROP_MIME_TYPE, "application/javascript");
        return source;
    }

}
