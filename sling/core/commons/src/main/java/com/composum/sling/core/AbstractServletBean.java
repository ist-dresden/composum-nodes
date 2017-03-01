package com.composum.sling.core;

import com.composum.sling.core.util.ConsoleUtil;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the abstract model base class for Composum Console components addressed by a forward from
 * a console servlet with the models resource path in the requests suffix
 */
public class AbstractServletBean extends AbstractSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractServletBean.class);

    public AbstractServletBean(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public AbstractServletBean(BeanContext context) {
        super(context);
    }

    public AbstractServletBean() {
        super();
    }

    /**
     * extract the resource referenced to display in the browsers view as the components resource
     */
    @Override
    public void initialize(BeanContext context) {
        Resource resource = ConsoleUtil.getConsoleResource(context);
        initialize(context, ResourceHandle.use(resource));
    }
}
