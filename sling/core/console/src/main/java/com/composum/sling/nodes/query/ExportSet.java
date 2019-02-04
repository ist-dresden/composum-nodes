package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportSet extends ConfigSet<ExportCfg> {

    private static final Logger LOG = LoggerFactory.getLogger(ExportSet.class);

    public static final String EXPORT_SET_RESOURCE_TYPE = "composum/nodes/browser/query/export/set";

    public static final ResourceFilter EXPORT_SET_ITEM_FILTER =
            new ResourceFilter.ResourceTypeFilter(new StringFilter.WhiteList("^composum/nodes/browser/query/export$"));

    public ExportSet(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ExportSet(BeanContext context) {
        super(context);
    }

    public ExportSet() {
        super();
    }

    protected String getSetResourceType() {
        return EXPORT_SET_RESOURCE_TYPE;
    }

    protected ResourceFilter getItemFilter() {
        return EXPORT_SET_ITEM_FILTER;
    }

    protected ExportCfg createItem(Resource resource) {
        return new ExportCfg(context, resource);
    }
}
