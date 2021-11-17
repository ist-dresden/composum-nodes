package com.composum.sling.nodes.console;

import com.composum.sling.core.AbstractServletBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.LinkMapper;
import org.apache.sling.api.resource.Resource;

public abstract class ConsoleServletBean extends AbstractServletBean {

    public ConsoleServletBean(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsoleServletBean(BeanContext context) {
        super(context);
    }

    public ConsoleServletBean() {
        super();
    }

    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
        context.getRequest().setAttribute(LinkMapper.LINK_MAPPER_REQUEST_ATTRIBUTE, LinkMapper.CONTEXT);
    }
}
