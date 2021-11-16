package com.composum.sling.core.bean;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingBean;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * a factory service interface to produce a bean (model)
 */
public interface SlingBeanFactory {

    /**
     * @return a new bean (model) for the given resource an the requested type
     */
    @NotNull
    SlingBean createBean(@NotNull BeanContext context, @NotNull Resource resource,
                         @NotNull Class<? extends SlingBean> type)
            throws InstantiationException;
}
