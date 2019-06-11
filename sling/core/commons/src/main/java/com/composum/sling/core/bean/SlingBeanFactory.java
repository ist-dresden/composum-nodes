package com.composum.sling.core.bean;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingBean;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;

/**
 * a factory service interface to produce a bean (model)
 */
public interface SlingBeanFactory {

    /**
     * @return a new bean (model) for the given resource an the requested type
     */
    @Nonnull
    SlingBean createBean(@Nonnull BeanContext context, @Nonnull Resource resource,
                         @Nonnull Class<? extends SlingBean> type)
            throws InstantiationException;
}
