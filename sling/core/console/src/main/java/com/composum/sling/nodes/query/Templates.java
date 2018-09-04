package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
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

import static com.composum.sling.nodes.query.Template.PROP_GROUP;

public class Templates extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Templates.class);

    public static final String DEFAULT_GROUP = "general";

    public static final String TEMPLATE_RESOURCE_TYPE = "composum/nodes/console/query/template";
    public static final String TEMPLATES_RESOURCE_TYPE = "composum/nodes/console/query/templates";

    public static final String CONTENT_QUERY_BASE = "/jcr:root";
    public static final String CONTENT_QUERY_RULE = "/*[@sling:resourceType='" + TEMPLATES_RESOURCE_TYPE + "']";

    private transient Map<String, List<Template>> templates;

    public Templates(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Templates(BeanContext context) {
        super(context);
    }

    public Templates() {
        super();
    }

    public Set<Map.Entry<String, List<Template>>> getGroups() {
        return getTemplates().entrySet();
    }

    public Map<String, List<Template>> getTemplates() {
        if (templates == null) {
            templates = new TreeMap<>();
            ResourceResolver resolver = getResolver();
            for (String path : resolver.getSearchPath()) {
                findTemplates(templates, CONTENT_QUERY_BASE + path + CONTENT_QUERY_RULE);
            }
            for (List<Template> group : templates.values()) {
                Collections.sort(group);
            }
        }
        return templates;
    }

    protected void findTemplates(Map<String, List<Template>> consoles, String query) {
        ResourceResolver resolver = getResolver();

        @SuppressWarnings("deprecation")
        Iterator<Resource> templateResources = resolver.findResources(query, Query.XPATH);
        if (templateResources != null) {

            while (templateResources.hasNext()) {

                Resource templateContent = templateResources.next();
                for (Resource template : templateContent.getChildren()) {
                    if (template.isResourceType(TEMPLATE_RESOURCE_TYPE)) {
                        ResourceHandle handle = ResourceHandle.use(template);
                        String groupName = handle.getProperty(PROP_GROUP, DEFAULT_GROUP);
                        List<Template> group = templates.get(groupName);
                        if (group == null) {
                            group = new ArrayList<>();
                            templates.put(groupName, group);
                        }
                        group.add(new Template(context, handle));
                    }
                }
            }
        }
    }
}
