package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibConfiguration;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Since there is currently (as of mid-2017) no Java javascript minifier library that works sufficiently well, this does nothing
 * and relies on minified siblings for each javascript, generated e.g. with minify-maven-plugin. (Yui is not sufficient.)
 */
@Component(
        label = "Clientlib Default Javascript Processor",
        description = "Delivers Javascript content bundled.",
        immediate = true
)
@Service
public class DefaultJavascriptProcessor extends AbstractClientlibRenderer implements JavascriptProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJavascriptProcessor.class);

    @Reference
    private ClientlibConfiguration clientlibConfig;

    @Override
    protected String getLinkTemplate() {
        return clientlibConfig.getJavascriptTemplate();
    }

    @Override
    public InputStream processContent(final InputStream source, ProcessorContext context) {
        context.hint(ResourceUtil.PROP_MIME_TYPE, "application/javascript");
        return source;
    }

}
