package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;

import static com.composum.sling.core.util.ResourceUtil.PROP_DESCRIPTION;

public class ConfigItem extends ConsoleSlingBean implements Comparable<ConfigItem> {

    public static final String PROP_ORDER = "order";
    public static final int ORDER_DEFAULT = 50;

    private transient String data;

    public ConfigItem(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConfigItem(BeanContext context) {
        super(context);
    }

    public ConfigItem() {
        super();
    }

    public int getOrder() {
        return getProperty(PROP_ORDER, ORDER_DEFAULT);
    }

    public String getId() {
        return getPath().replace('/', '-');
    }

    @Override
    public String getTitle() {
        String title = super.getTitle();
        return StringUtils.isNotBlank(title) ? title : getName();
    }

    public String getDescription() {
        return getProperty(PROP_DESCRIPTION, "");
    }

    @Override
    public int compareTo(@Nonnull ConfigItem other) {
        int result = getOrder() - other.getOrder();
        if (result == 0) {
            result = getTitle().compareTo(other.getTitle());
        }
        return result;
    }
}
