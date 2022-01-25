package com.composum.sling.nodes.console;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.LinkMapper;
import org.apache.sling.api.resource.Resource;

public abstract class ConsoleSlingBean extends AbstractSlingBean {

    public ConsoleSlingBean(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsoleSlingBean(BeanContext context) {
        super(context);
    }

    public ConsoleSlingBean() {
        super();
    }

    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
        if (context.getRequest() != null) {
            context.getRequest()
                    .setAttribute(LinkMapper.LINK_MAPPER_REQUEST_ATTRIBUTE, LinkMapper.CONTEXT);
        }
    }
}
