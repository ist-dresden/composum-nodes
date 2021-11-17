package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.HttpUtil;
import com.composum.sling.core.util.SerializableValueMap;
import com.composum.sling.core.util.ValueEmbeddingReader;
import com.composum.sling.nodes.console.Condition;
import org.apache.commons.io.IOUtils;
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BrowserViews implements HttpUtil.CachableInstance {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserViews.class);

    public static final String SA_INSTANCE = BrowserViews.class.getName() + "#instance";

    public static final String PN_ID = "id";
    public static final String PN_RANK = "rank";
    public static final int DEFAULT_RANK = 50;

    public static final String NN_PATTERN = "pattern";
    public static final String NN_TABS = "tabs";
    public static final String NN_TOOLBAR = "toolbar";
    public static final String NN_CONTENT = "content";

    public static final String PN_CONDITION = "condition";
    public static final String PN_ENABLED = "enabled";
    public static final String PN_KEY = "key";
    public static final String PN_ICON = "icon";
    public static final String PN_CSS = "css";
    public static final String PN_GROUP = "group";
    public static final String PN_LABEL = "label";
    public static final String PN_TITLE = "title";
    public static final String PN_HREF = "href";
    public static final String PN_TARGET = "target";
    public static final String PN_PLACEHOLDER = "placeholder";
    public static final String PN_DESCRIPTION = "description";
    public static final String PN_RESOURCE_TYPE = "resourceType";

    public static final String PN_TYPE = "type";
    public static final String TYPE_BUTTON = "button";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_TEXT = "text";

    public static final String QUERY_BASE = "/jcr:root";
    public static final String QUERY_RULE = "/*[@sling:resourceType='composum/nodes/browser/view']";

    @NotNull
    public static BrowserViews getInstance(@NotNull final SlingHttpServletRequest request) {
        return HttpUtil.getInstance(request, SA_INSTANCE, new HttpUtil.InstanceFactory<BrowserViews>() {

            @Override
            public Class<BrowserViews> getType() {
                return BrowserViews.class;
            }

            @Override
            public BrowserViews newInstance(SlingHttpServletRequest request) {
                return new BrowserViews(request);
            }
        });
    }

    public class ResourceContext {

        @NotNull
        protected final Map<String, Object> properties;

        public ResourceContext(@Nullable final Resource resource) {
            if (resource != null) {
                properties = new HashMap<>(resource.getValueMap());
                properties.put("name", resource.getName());
                properties.put("path", resource.getPath());
            } else {
                properties = Collections.emptyMap();
            }
        }

        public String adjustValue(@NotNull final String value) {
            try {
                return IOUtils.toString(new ValueEmbeddingReader(new StringReader(value), properties));
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
            return value;
        }
    }

    public class View implements Comparable<View>, Serializable {

        public class Tab {

            protected final String name;
            protected final ValueMap properties;
            protected final Condition condition;

            public Tab(@NotNull final Resource resource) {
                name = resource.getName();
                properties = new SerializableValueMap(resource.getValueMap());
                condition = new Condition.And(properties.get(PN_CONDITION, new String[0]));
            }

            public boolean matches(@NotNull final BeanContext context, @NotNull final Resource resource) {
                return condition.accept(context, resource);
            }

            @NotNull
            public String getResourceType() {
                return properties.get(PN_RESOURCE_TYPE, "");
            }

            @NotNull
            public String getId() {
                return properties.get(PN_ID, name);
            }

            @NotNull
            public String getKey() {
                return properties.get(PN_KEY, "");
            }

            @NotNull
            public String getIcon() {
                return properties.get(PN_ICON, "");
            }

            @NotNull
            public String getCss() {
                return properties.get(PN_CSS, "");
            }

            @NotNull
            public String getGroup() {
                return properties.get(PN_GROUP, "");
            }

            @NotNull
            public String getLabel() {
                return properties.get(PN_LABEL, "");
            }

            @NotNull
            public String getTitle() {
                return properties.get(PN_TITLE, "");
            }
        }

        public class Toolbar implements Serializable {

            public class Element implements Serializable {

                protected final String name;
                protected final ValueMap properties;
                protected final Condition condition;
                protected final Condition enabled;
                protected final Boolean isEnabled;

                public Element(@NotNull final Resource resource) {
                    name = resource.getName();
                    properties = new SerializableValueMap(resource.getValueMap());
                    condition = new Condition.And(properties.get(PN_CONDITION, new String[0]));
                    enabled = new Condition.And(properties.get(PN_ENABLED, new String[0]));
                    isEnabled = null;
                }

                public Element(@NotNull final Element template,
                               @NotNull final BeanContext context, @NotNull final Resource resource) {
                    name = template.name;
                    properties = template.properties;
                    condition = template.condition;
                    enabled = template.enabled;
                    isEnabled = enabled(context, resource);
                }

                public boolean matches(@NotNull final BeanContext context, @NotNull final Resource resource) {
                    return condition.accept(context, resource);
                }

                public boolean enabled(@NotNull final BeanContext context, @NotNull final Resource resource) {
                    return enabled.accept(context, resource);
                }

                public boolean isEnabled() {
                    return isEnabled == null || isEnabled;
                }

                public String getType() {
                    return properties.get(PN_TYPE, TYPE_BUTTON);
                }

                public String getKey() {
                    return properties.get(PN_KEY, name);
                }

                @NotNull
                public String getIcon() {
                    return Toolbar.this.resourceContext.adjustValue(properties.get(PN_ICON, ""));
                }

                @NotNull
                public String getCss() {
                    return properties.get(PN_CSS, "");
                }

                @NotNull
                public String getLabel() {
                    return Toolbar.this.resourceContext.adjustValue(properties.get(PN_LABEL, ""));
                }

                @NotNull
                public String getTitle() {
                    return Toolbar.this.resourceContext.adjustValue(properties.get(PN_TITLE, ""));
                }

                @NotNull
                public String getPlaceholder() {
                    return Toolbar.this.resourceContext.adjustValue(properties.get(PN_PLACEHOLDER, ""));
                }

                @NotNull
                public String getHref() {
                    return Toolbar.this.resourceContext.adjustValue(properties.get(PN_HREF, ""));
                }

                @NotNull
                public String getTarget() {
                    return properties.get(PN_TARGET, "");
                }
            }

            public class Group implements Serializable {

                protected final ValueMap properties;
                protected final Condition condition;

                protected final List<Element> elements = new ArrayList<>();

                public Group(@NotNull final Resource resource) {
                    properties = new SerializableValueMap(resource.getValueMap());
                    condition = new Condition.And(properties.get(PN_CONDITION, new String[0]));
                    for (Resource child : resource.getChildren()) {
                        elements.add(new Element(child));
                    }
                    if (elements.isEmpty()) {
                        elements.add(new Element(resource));
                    }
                }

                protected Group(@NotNull final Group template,
                                @NotNull final BeanContext context, @NotNull final Resource resource) {
                    properties = template.properties;
                    condition = template.condition;
                    for (Element element : template.elements) {
                        if (element.matches(context, resource)) {
                            elements.add(new Element(element, context, resource));
                        }
                    }
                }

                public boolean matches(@NotNull final BeanContext context, @NotNull final Resource resource) {
                    return condition.accept(context, resource);
                }

                public Collection<Element> getElements() {
                    return elements;
                }
            }

            protected final ValueMap properties;
            protected final ResourceContext resourceContext;

            protected final List<Group> groups = new ArrayList<>();

            public Toolbar(@NotNull final Resource resource) {
                properties = new SerializableValueMap(resource.getValueMap());
                resourceContext = new ResourceContext(null);
                for (Resource child : resource.getChildren()) {
                    groups.add(new Group(child));
                }
            }

            protected Toolbar(@NotNull final Toolbar template,
                              @NotNull final BeanContext context, @NotNull final Resource resource) {
                properties = template.properties;
                resourceContext = new ResourceContext(resource);
                for (Group group : template.groups) {
                    if (group.matches(context, resource)) {
                        groups.add(new Group(group, context, resource));
                    }
                }
            }

            public Collection<Group> getGroups() {
                return groups;
            }
        }

        public class Content implements Serializable {

            protected final ValueMap properties;

            public Content(@NotNull final Resource resource) {
                properties = new SerializableValueMap(resource.getValueMap());
            }

            @NotNull
            public String getResourceType() {
                return properties.get(PN_RESOURCE_TYPE, "");
            }
        }

        protected final ValueMap properties;
        protected final Condition condition;
        protected final List<Tab> tabs;
        protected String tabResourceType = null;
        protected Toolbar toolbar = null;
        protected Content content = null;

        public View(ResourceHandle resource) {
            properties = new SerializableValueMap(resource.getValueMap());
            condition = Condition.And.fromResource(resource.getChild(NN_PATTERN));
            tabs = new ArrayList<>();
            final Resource tabs = resource.getChild(NN_TABS);
            if (tabs != null) {
                for (final Resource tabRes : tabs.getChildren()) {
                    final Tab tab = new Tab(tabRes);
                    this.tabs.add(tab);
                    Resource child;
                    if ((child = tabRes.getChild(NN_TOOLBAR)) != null && toolbar == null) {
                        toolbar = new Toolbar(child);
                        tabResourceType = tab.getResourceType();
                    }
                    if ((child = tabRes.getChild(NN_CONTENT)) != null && content == null) {
                        content = new Content(child);
                        tabResourceType = tab.getResourceType();
                    }
                }
            }
        }

        @Override
        public int compareTo(@NotNull View other) {
            return Integer.compare(getRank(), other.getRank());
        }

        public String getId() {
            return properties.get(PN_ID, "");
        }

        public int getRank() {
            return properties.get(PN_RANK, DEFAULT_RANK);
        }

        public boolean matches(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return condition == null || condition.accept(context, resource);
        }

        @NotNull
        public String getViewResourceType() {
            return properties.get(PN_RESOURCE_TYPE, "");
        }

        @NotNull
        public String getTabResourceType() {
            return tabResourceType;
        }

        @NotNull
        public List<Tab> getTabs(@NotNull final BeanContext context, @NotNull final Resource resource) {
            final List<Tab> result = new ArrayList<>();
            for (final Tab tab : tabs) {
                if (tab.matches(context, resource)) {
                    result.add(tab);
                }
            }
            return result;
        }

        @NotNull
        public Toolbar getToolbar(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return new Toolbar(toolbar, context, resource);
        }

        @NotNull
        public Content getContent(@NotNull final BeanContext context, @NotNull final Resource resource) {
            return content;
        }
    }

    @Nullable
    public static View getView(@NotNull final BeanContext context, @NotNull final Resource resource) {
        final BrowserViews instance = getInstance(context.getRequest());
        for (final View view : instance.browserViews) {
            if (view.matches(context, resource)) {
                return view;
            }
        }
        return null;
    }

    protected final List<View> browserViews;
    protected final long created;

    protected BrowserViews(@NotNull final SlingHttpServletRequest request) {
        final ResourceResolver resolver = request.getResourceResolver();
        final Map<String, View> viewMap = new HashMap<>();
        for (final String path : resolver.getSearchPath()) {
            findBrowserViews(resolver, viewMap, QUERY_BASE + path + QUERY_RULE);
        }
        browserViews = new ArrayList<>(viewMap.values());
        Collections.sort(browserViews);
        created = System.currentTimeMillis();
    }

    @Override
    public long getCreated() {
        return created;
    }

    protected void findBrowserViews(@NotNull final ResourceResolver resolver,
                                    @NotNull final Map<String, View> viewMap, @NotNull final String query) {
        @SuppressWarnings("deprecation") final Iterator<Resource> browserViewContentResources = resolver.findResources(query, Query.XPATH);
        while (browserViewContentResources.hasNext()) {
            final Resource browserViewRes = browserViewContentResources.next();
            final View browserView = new View(ResourceHandle.use(browserViewRes));
            if (!viewMap.containsKey(browserView.getId())) {
                viewMap.put(browserView.getId(), browserView);
            }
        }
    }
}
