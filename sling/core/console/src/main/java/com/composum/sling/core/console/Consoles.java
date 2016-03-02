package com.composum.sling.core.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Consoles extends ConsolePage {

    public static final String CATEGORIES = "categories";

    public static final String ORDER = "order";
    public static final int ORDER_DEFAULT = 50;

    public static final String PROP_PRECONDITION = "precondition";

    public static final String PRECONDITION_CLASS_AVAILABILITY = "class";

    public static final ResourceFilter CONSOLES_FILTER = new ConsoleFilter();

    public static final Map<String, PreconditionFilter> PRECONDITION_FILTERS;

    static {
        PRECONDITION_FILTERS = new HashMap<>();
        PRECONDITION_FILTERS.put(PRECONDITION_CLASS_AVAILABILITY, new ClassAvailabilityFilter());
    }

    public static final String CONTENT_QUERY_BASE = "/jcr:root";
    public static final String CONTENT_QUERY_RULE = "//content[@sling:resourceType='composum/sling/console/page']";
    public static final String CONTENT_QUERY_LIBS = CONTENT_QUERY_BASE + "/libs" + CONTENT_QUERY_RULE;
    public static final String CONTENT_QUERY_APPS = CONTENT_QUERY_BASE + "/apps" + CONTENT_QUERY_RULE;

    public static class ConsoleFilter implements ResourceFilter {

        private final List<String> selectors;

        public ConsoleFilter(String... selectors) {
            this.selectors = Arrays.asList(selectors);
        }

        @Override
        public boolean accept(Resource resource) {
            ValueMap values = resource.adaptTo(ValueMap.class);
            String[] categories = values.get(CATEGORIES, new String[0]);
            for (String category : categories) {
                if (selectors.contains(category)) {
                        String precondition = values.get(PROP_PRECONDITION, "");
                        if (StringUtils.isNotBlank(precondition)) {
                            String[] rule = StringUtils.split(precondition, ":");
                            PreconditionFilter filter = PRECONDITION_FILTERS.get(rule[0]);
                            if (filter != null) {
                                return filter.accept(resource, rule.length > 0 ? rule[1] : null);
                            }
                        }
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

    public class Console implements Comparable<Console> {

        private final String label;
        private final String name;
        private final String path;
        private final int order;

        public Console(String label, String name, String path, int order) {
            this.label = label;
            this.name = name;
            this.path = path;
            this.order = order;
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

        @Override
        public int compareTo(Console other) {
            return order - other.order;
        }
    }

    private transient List<Console> consoles;

    public Consoles(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Consoles(BeanContext context) {
        super(context);
    }

    public Consoles() {
        super();
    }

    //
    // workspace, user and profile
    //

    public String getCurrentUser() {
        Session session = getSession();
        String userId = session.getUserID();
        return userId;
    }

    public String getWorkspaceName() {
        return getSession().getWorkspace().getName();
    }

    public List<Console> getConsoles() {
        if (consoles == null) {
            consoles = new ArrayList<>();
            findConsoles(consoles, CONTENT_QUERY_APPS);
            findConsoles(consoles, CONTENT_QUERY_LIBS);
            Collections.sort(consoles);
        }
        return consoles;
    }

    protected void findConsoles(List<Console> consoles, String query) {
        ResourceResolver resolver = getResolver();

        Iterator<Resource> consoleContentResources = resolver.findResources(query, Query.XPATH);
        if (consoleContentResources != null) {

            CoreConfiguration configuration = getSling().getService(CoreConfiguration.class);
            String[] categories = configuration.getConsoleCategories();
            ResourceFilter consoleFilter = new ConsoleFilter(categories);

            while (consoleContentResources.hasNext()) {

                Resource consoleContent = consoleContentResources.next();
                for (Resource console : consoleContent.getChildren()) {
                    if (consoleFilter.accept(console)) {
                        ResourceHandle handle = ResourceHandle.use(console);
                        consoles.add(new Console(handle.getTitle(), handle.getName(),
                                handle.getPath(), handle.getProperty(ORDER, ORDER_DEFAULT)));
                    }
                }
            }
        }
        return consoles;
    }

    //
    // console module preconditions
    //

    /**
     * the special filter to check the preconditions of a console module
     */
    public interface PreconditionFilter {

        /**
         * check the configured precondition for the concole resource
         */
        boolean accept(Resource resource, String precondition);
    }

    /**
     * check the availability of a class as a precondition for a console module
     */
    public static class ClassAvailabilityFilter implements PreconditionFilter {

        @Override
        public boolean accept(Resource resource, String className) {
            boolean classAvailable = false;
            try {
                getClass().forName(className);
                classAvailable = true;
            } catch (Exception ex) {
                // ok, not available
            }
            return classAvailable;
        }
    }
}
