package com.composum.sling.core.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Consoles extends ConsolePage {

    private static final Logger LOG = LoggerFactory.getLogger(Consoles.class);

    public static final String CONTENT_QUERY = "/jcr:root//content[@sling:resourceType='composum/sling/console/page']";

    public static class ConsoleFilter implements ResourceFilter {

        private final List<String> selectors;

        public ConsoleFilter(String... selectors) {
            this.selectors = Arrays.asList(selectors);
        }

        @Override
        public boolean accept(Resource resource) {
            ValueMap values = resource.adaptTo(ValueMap.class);
            String[] categories = values.get("categories", new String[0]);
            for (String category : categories) {
                if (selectors.contains(category)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isRestriction() {
            return true;
        }

        @Override
        public void toString(StringBuilder builder) {
            builder.append("console(").append(StringUtils.join(selectors, ',')).append(")");
        }
    }

    public Consoles(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Consoles(BeanContext context) {
        super(context);
    }

    public Consoles() {
        super();
    }

    public String getCurrentUser() {
        Session session = getSession();
        String userId = session.getUserID();
        return userId;
    }

    public String getWorkspaceName() {
        return getSession().getWorkspace().getName();
    }

    public class Console {

        private final String label;
        private final String name;
        private final String path;

        public Console(String label, String name, String path) {
            this.label = label;
            this.name = name;
            this.path = path;
        }

        public String getLabel() {
            return label;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }
    }

    public List<Console> getConsoles() {
        List<Console> consoles = new ArrayList<>();
        ResourceResolver resolver = getResolver();
        Iterator<Resource> consoleContentResources = resolver.findResources(CONTENT_QUERY, Query.XPATH);
        if (consoleContentResources != null) {
            while (consoleContentResources.hasNext()) {
                CoreConfiguration configuration = getSling().getService(CoreConfiguration.class);
                String[] categories = configuration.getConsoleCategories();
                ResourceFilter consoleFilter = new ConsoleFilter(categories);
                Resource consoleContent = consoleContentResources.next();
                for (Resource console : consoleContent.getChildren()) {
                    if (consoleFilter.accept(console)) {
                        ResourceHandle handle = ResourceHandle.use(console);
                        consoles.add(new Console(handle.getTitle(), handle.getName(), handle.getPath()));
                    }
                }
            }
        }
        return consoles;
    }
}
