package com.composum.sling.core.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

public class Consoles extends ConsolePage {

    private static final Logger LOG = LoggerFactory.getLogger(Consoles.class);

    public static final ResourceFilter CONSOLES_FILTER = new ResourceFilter.ResourceTypeFilter(
            new StringFilter.WhiteList("^composum/sling/console/.*$")
    );

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
}
