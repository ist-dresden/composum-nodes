package com.composum.sling.core.console;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolePage extends AbstractSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolePage.class);

    public ConsolePage(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsolePage(BeanContext context) {
        super(context);
    }

    public ConsolePage() {
        super();
    }

    public String getURL (String path) {
        return LinkUtil.getUrl(getRequest(), path);
    }
}
