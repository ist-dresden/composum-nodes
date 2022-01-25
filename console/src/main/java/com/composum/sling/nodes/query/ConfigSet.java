package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class ConfigSet<Item extends ConfigItem> extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigSet.class);

    public static final String SERVICE_KEY = "nodes/query/configuration";

    public static final String PROP_GROUP = "group";
    public static final String DEFAULT_GROUP = "General";

    private transient Map<String, List<Item>> items;

    public ConfigSet(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConfigSet(BeanContext context) {
        super(context);
    }

    public ConfigSet() {
        super();
    }

    protected abstract String getSetResourceType();

    protected abstract ResourceFilter getItemFilter();

    protected abstract Item createItem(Resource resource);

    public Set<Map.Entry<String, List<Item>>> getGroups() {
        return getItems().entrySet();
    }

    public Map<String, List<Item>> getItems() {
        if (items == null) {
            items = new TreeMap<>();
            ResourceResolver resolver = getResolver();
            for (String path : resolver.getSearchPath()) {
                findItems(items, "/jcr:root" + path + "/*[@sling:resourceType='" + getSetResourceType() + "']");
            }
            for (List<Item> group : items.values()) {
                Collections.sort(group);
            }
        }
        return items;
    }

    protected void findItems(Map<String, List<Item>> consoles, String query) {
        ResourceResolver resolver = getResolver();
        ResourceFilter filter = getItemFilter();

        @SuppressWarnings("deprecation")
        Iterator<Resource> templateResources = resolver.findResources(query, Query.XPATH);
        if (templateResources != null) {

            while (templateResources.hasNext()) {

                Resource templateContent = templateResources.next();
                for (Resource item : templateContent.getChildren()) {
                    if (filter.accept(item)) {
                        ResourceHandle handle = ResourceHandle.use(item);
                        String groupName = handle.getProperty(PROP_GROUP, DEFAULT_GROUP);
                        List<Item> group = items.get(groupName);
                        if (group == null) {
                            group = new ArrayList<>();
                            items.put(groupName, group);
                        }
                        group.add(createItem(handle));
                    }
                }
            }
        }
    }
}
