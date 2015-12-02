package com.composum.sling.core.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consoles extends ConsolePage {

    private static final Logger LOG = LoggerFactory.getLogger(Consoles.class);

    public static final String CONSOLE_RESOURCE_TYPE_PATTERN = "^composum/sling/console/.*$";

    public static final String PROP_PRECONDITION = "precondition";

    public static final String PRECONDITION_CLASS_AVAILABILITY = "class";

    public static final ResourceFilter CONSOLES_FILTER = new ConsoleFilter();

    public static final Map<String, PreconditionFilter> PRECONDITION_FILTERS;

    static {
        PRECONDITION_FILTERS = new HashMap<>();
        PRECONDITION_FILTERS.put(PRECONDITION_CLASS_AVAILABILITY, new ClassAvailabilityFilter());
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

    //
    // console modules
    //

    public static class ConsoleFilter extends ResourceFilter.ResourceTypeFilter {

        public ConsoleFilter() {
            super(new StringFilter.WhiteList(CONSOLE_RESOURCE_TYPE_PATTERN));
        }

        @Override
        public boolean accept(Resource resource) {
            boolean accepted = super.accept(resource);
            if (accepted) {
                ResourceHandle handle = ResourceHandle.use(resource);
                String precondition = handle.getProperty(PROP_PRECONDITION);
                if (StringUtils.isNotBlank(precondition)) {
                    String[] rule = StringUtils.split(precondition, ":");
                    PreconditionFilter filter = PRECONDITION_FILTERS.get(rule[0]);
                    if (filter != null) {
                        accepted = filter.accept(resource, rule.length > 0 ? rule[1] : null);
                    }
                }
            }
            return accepted;
        }
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
        Resource consoleContent = getResolver().getResource("/libs/composum/sling/console/content");
        if (consoleContent != null) {
            for (Resource console : consoleContent.getChildren()) {
                if (CONSOLES_FILTER.accept(console)) {
                    ResourceHandle handle = ResourceHandle.use(console);
                    consoles.add(new Console(handle.getTitle(), handle.getName(), handle.getPath()));
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
