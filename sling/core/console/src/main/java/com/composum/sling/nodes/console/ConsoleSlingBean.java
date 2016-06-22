package com.composum.sling.nodes.console;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.LinkMapper;
import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.resource.Resource;

public class ConsoleSlingBean extends AbstractSlingBean {

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
        context.getRequest().setAttribute(LinkUtil.LINK_MAPPER, LinkMapper.CONTEXT);
    }
}
