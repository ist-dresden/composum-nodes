package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.HttpUtil;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Consoles implements HttpUtil.CachableInstance {

    private static final Logger LOG = LoggerFactory.getLogger(Consoles.class);

    public static final String CATEGORIES = "categories";

    public static final String ORDER = "order";
    public static final int ORDER_DEFAULT = 50;

    public static final String PN_CONSOLE_ID = "consoleId";
    public static final String PN_PARENT_ID = "parentId";
    public static final String PN_SLING_REDIRECT = "sling:redirect";
    public static final String PN_DYN_REDIRECT = "dynamicRedirect";
    public static final String PN_PATH_CONDITION = "pathCondition";
    public static final String PN_PERM_SUPPORT = "permissionsSupport";
    public static final String PN_DESCRIPTION = "description";
    public static final String PN_TARGET = "target";
    public static final String PN_MENU = "menu";
    public static final String PN_PRECONDITION = "precondition";
    public static final String PN_CONTENT_SRC = "contentSrc";

    public static final String CONTENT_QUERY_BASE = "/jcr:root";
    public static final String CONTENT_QUERY_RULE = "/content[@sling:resourceType='composum/nodes/console/page']";

    public static final String SA_INSTANCE = Consoles.class.getName() + "#instance";

    @NotNull
    public static Consoles getInstance(@NotNull final BeanContext context) {
        final SlingHttpServletRequest request = Objects.requireNonNull(context.getRequest());
        return HttpUtil.getInstance(request, SA_INSTANCE, new HttpUtil.InstanceFactory<Consoles>() {

            @Override
            public Class<Consoles> getType() {
                return Consoles.class;
            }

            @Override
            public Consoles newInstance(SlingHttpServletRequest request) {
                return new Consoles(context);
            }
        });
    }

    public class ConsoleFilter extends ResourceFilter.AbstractResourceFilter {

        private final BeanContext context;
        private final List<String> selectors;

        public ConsoleFilter(@NotNull final BeanContext context, String... selectors) {
            this.context = context;
            this.selectors = Arrays.asList(selectors);
        }

        @Override
        public boolean accept(@Nullable final Resource resource) {
            if (resource != null) {
                final ValueMap values = resource.getValueMap();
                final String[] categories = values.get(CATEGORIES, new String[0]);
                for (final String category : categories) {
                    if (selectors.contains(category)) {
                        final String[] precondition = values.get(PN_PRECONDITION, new String[0]);
                        for (final String condition : precondition) {
                            final Condition filter = Condition.DEFAULT.getCondition(condition);
                            if (filter != null) {
                                if (!filter.accept(context, resource)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isRestriction() {
            return true;
        }

        @Override
        public void toString(@NotNull final StringBuilder builder) {
            builder.append("console(").append(StringUtils.join(selectors, ',')).append(")");
        }
    }

    public class Console implements Comparable<Console>, Serializable {

        private final String id;
        private final String name;
        private final String path;
        private final String title;
        private final String slingRedirect;
        private final String pathCondition;
        private final boolean dynamicRedirect;
        private final boolean permissionsSupport;
        private final String description;
        private final String target;
        private final String contentSrc;
        private final int order;

        private Console parent = null;
        private final String parentId;
        private final boolean isMenu;
        private final boolean isDeclaredMenu;
        private final Map<String, Console> menuItems;

        public Console(@NotNull final ConsoleFilter filter, @NotNull final ResourceHandle handle) {
            this(filter, handle, handle.getProperty(PN_MENU, Boolean.FALSE));
        }

        public Console(@NotNull final ConsoleFilter filter, @NotNull final ResourceHandle handle, boolean isMenu) {
            id = handle.getProperty(PN_CONSOLE_ID, String.class);
            name = handle.getName();
            path = handle.getPath();
            title = handle.getTitle();
            slingRedirect = handle.getProperty(PN_SLING_REDIRECT, String.class);
            pathCondition = handle.getProperty(PN_PATH_CONDITION, String.class);
            dynamicRedirect = handle.getProperty(PN_DYN_REDIRECT, Boolean.FALSE);
            permissionsSupport = handle.getProperty(PN_PERM_SUPPORT, Boolean.FALSE);
            description = handle.getProperty(PN_DESCRIPTION, "");
            target = handle.getProperty(PN_TARGET, "");
            contentSrc = handle.getProperty(PN_CONTENT_SRC, "");
            order = handle.getProperty(ORDER, ORDER_DEFAULT);
            parentId = handle.getProperty(PN_PARENT_ID, String.class);
            this.isMenu = isMenu;
            this.isDeclaredMenu = handle.getProperty(PN_MENU, Boolean.FALSE);
            menuItems = new TreeMap<>();
            if (isMenu) {
                buildMenu(filter, handle);
            }
        }

        public boolean isDynamicRedirect() {
            return dynamicRedirect;
        }

        public boolean supportsPermissions() {
            return permissionsSupport;
        }

        @NotNull
        public String getLabel() {
            return title;
        }

        @NotNull
        public String getId() {
            return StringUtils.isNotBlank(id) ? id : (parent != null ? (parent.getId() + "-" + getName()) : getName());
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getPath() {
            return path;
        }

        @NotNull
        public String getDescription() {
            return StringUtils.isNotBlank(description)
                    ? (description.startsWith("/") ? description : coreConfig.getComposumBase() + description) : "";
        }

        @NotNull
        public String getContentSrc() {
            return StringUtils.isNotBlank(contentSrc)
                    ? (contentSrc.startsWith("/") ? contentSrc : coreConfig.getComposumBase() + contentSrc) : "";
        }

        @NotNull
        protected String embedSuffix(@NotNull final SlingHttpServletRequest request, @NotNull String value) {
            if (StringUtils.isNotBlank(value)) {
                final String suffix = XSS.filter(request.getRequestPathInfo().getSuffix());
                if (StringUtils.isNotBlank(suffix)) {
                    value = StringUtils.replace(value, "${path}", suffix); // plain
                    // placeholder ${path} is maybe URL-encoded at that place since {} aren't valid in URL.
                    value = StringUtils.replace(value, "$%7Bpath%7D", suffix); // encoded
                }
            }
            return value;
        }

        @NotNull
        public String getRedirectUrl(@NotNull final SlingHttpServletRequest request) {
            final String url = StringUtils.isNotBlank(slingRedirect) ? slingRedirect : getPath();
            return isDynamicRedirect() ? url : (StringUtils.isNotBlank(url)
                    ? embedSuffix(request, LinkUtil.getUnmappedUrl(request, url)) : "");
        }

        @NotNull
        public String getUrl(@NotNull final SlingHttpServletRequest request) {
            final String url = isDynamicRedirect() ? ""
                    : embedSuffix(request, LinkUtil.getUnmappedUrl(request, getPath()));
            return StringUtils.isNotBlank(url) ? url : "#";
        }

        @NotNull
        public String getLinkAttributes(@NotNull final SlingHttpServletRequest request) {
            final StringBuilder builder = new StringBuilder();
            String value;
            if (StringUtils.isNotBlank(target)) {
                builder.append(" target=\"").append(target).append("\"");
            }
            if (isDynamicRedirect() && StringUtils.isNotBlank(value = getRedirectUrl(request))) {
                builder.append(" data-redirect=\"").append(value).append("\"");
                if (StringUtils.isNotBlank(pathCondition)) {
                    builder.append(" data-path-condition=\"").append(pathCondition).append("\"");
                }
            }
            return builder.toString();
        }

        @Override
        public int compareTo(Console other) {
            final CompareToBuilder builder = new CompareToBuilder();
            builder.append(order, other.order);
            builder.append(getPath(), other.getPath());
            return builder.toComparison();
        }

        @Nullable
        public String getParentId() {
            return parentId;
        }

        @Nullable
        public Console getParent() {
            return parent;
        }

        public boolean isMenu() {
            return isMenu;
        }

        public boolean isDeclaredMenu() {
            return isDeclaredMenu;
        }

        public boolean isValidMenu() {
            return !menuItems.isEmpty();
        }

        @NotNull
        public Collection<ConsoleModel> getMenuItems(@NotNull final BeanContext context) {
            final List<ConsoleModel> result = new ArrayList<>();
            for (final Console console : menuItems.values()) {
                if (!console.isDeclaredMenu() || console.isValidMenu()) {
                    result.add(new ConsoleModel(context, console));
                }
            }
            return result;
        }

        protected void addConsole(@NotNull final Console console) {
            menuItems.put(String.format("%04d", console.order) + "#" + console.getName(), console);
            console.parent = this;
        }

        protected void buildMenu(@NotNull final ConsoleFilter filter, @NotNull final ResourceHandle handle) {
            for (final Resource child : handle.getChildren()) {
                if (filter.accept(child)) {
                    final Console console = new Console(filter, ResourceHandle.use(child), true);
                    consoleSet.put(console.getName(), console);
                    addConsole(console);
                }
            }
        }

        @NotNull
        public String toString(@NotNull final BeanContext context) {
            final StringWriter buffer = new StringWriter();
            final JsonWriter writer = new JsonWriter(buffer);
            try {
                final Console parent = getParent();
                writer.beginObject();
                if (parent != null) {
                    writer.name("parent").value(parent.getId());
                }
                writer.name("id").value(getId());
                writer.name("url").value(getUrl(context.getRequest()));
                writer.endObject();
                writer.flush();
            } catch (IOException ioex) {
                LOG.error(ioex.toString());
            }
            return buffer.toString();
        }
    }

    private final BeanContext beanContext;
    private final CoreConfiguration coreConfig;
    private final TreeMap<String, Console> consoleSet;
    private final Set<Console> toplevel;
    private final long created;

    public Consoles(@NotNull final BeanContext context) {
        beanContext = context;
        coreConfig = beanContext.getService(CoreConfiguration.class);
        consoleSet = new TreeMap<>();
        ResourceResolver resolver = context.getResolver();
        for (String path : resolver.getSearchPath()) {
            findConsoles(context, CONTENT_QUERY_BASE + path + CONTENT_QUERY_RULE);
        }
        for (final Map.Entry<String, Console> entry : consoleSet.entrySet()) {
            final Console console = entry.getValue();
            final String parentId = console.getParentId();
            final Console parent;
            if (StringUtils.isNotBlank(parentId) && (parent = getConsole(parentId)) != null) {
                parent.addConsole(console);
            }
        }
        toplevel = new TreeSet<>();
        for (final Map.Entry<String, Console> entry : consoleSet.entrySet()) {
            final Console console = entry.getValue();
            if (console.getParent() == null && (!console.isMenu() || console.isValidMenu())) {
                toplevel.add(console);
            }
        }
        for (final Map.Entry<String, Console> entry : new TreeMap<>(consoleSet).entrySet()) {
            final String key = entry.getKey();
            final Console console = entry.getValue();
            if (console.isMenu()) {
                final String id = console.getId();
                if (!key.equals(id)) {
                    consoleSet.remove(key);
                    consoleSet.put(id, console);
                }
            }
        }
        created = System.currentTimeMillis();
    }

    @Override
    public long getCreated() {
        return created;
    }

    @NotNull
    public Collection<ConsoleModel> getConsoles(@NotNull final BeanContext context) {
        final List<ConsoleModel> result = new ArrayList<>();
        for (final Console console : toplevel) {
            result.add(new ConsoleModel(context, console));
        }
        return result;
    }

    @Nullable
    public ConsoleModel getConsole(@NotNull final BeanContext context, @NotNull final String name) {
        final Console console = getConsole(name);
        return console != null ? new ConsoleModel(context, console) : null;
    }

    @Nullable
    public Console getConsole(@NotNull final String name) {
        return consoleSet.get(name);
    }

    protected void findConsoles(@NotNull final BeanContext context, @NotNull final String query) {
        final SlingHttpServletRequest request = Objects.requireNonNull(context.getRequest());
        final NodesConfiguration configuration = Objects.requireNonNull(context.getService(NodesConfiguration.class));
        final String[] categories = configuration.getConsoleCategories();
        final ConsoleFilter consoleFilter = new ConsoleFilter(context, categories);
        final ResourceResolver resolver = context.getResolver();
        @SuppressWarnings("deprecation") final Iterator<Resource> consoleContentResources = resolver.findResources(query, Query.XPATH);
        while (consoleContentResources.hasNext()) {
            final Resource consoleContent = consoleContentResources.next();
            for (final Resource resource : consoleContent.getChildren()) {
                if (consoleFilter.accept(resource)) {
                    final Console console = new Console(consoleFilter, ResourceHandle.use(resource));
                    consoleSet.putIfAbsent(console.getId(), console);
                }
            }
        }
    }

    @NotNull
    public String toString(@NotNull final BeanContext context) {
        final StringWriter buffer = new StringWriter();
        final JsonWriter writer = new JsonWriter(buffer);
        try {
            writer.beginObject();
            for (Map.Entry<String, Console> entry : consoleSet.entrySet()) {
                writer.name(entry.getKey()).jsonValue(entry.getValue().toString(context));
            }
            writer.endObject();
            writer.flush();
        } catch (IOException ioex) {
            LOG.error(ioex.toString());
        }
        return buffer.toString();
    }
}
