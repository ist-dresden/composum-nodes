package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.PathReferencesService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Path References Service"
        }
)
public class PathReferencesServiceImpl implements PathReferencesService {

    protected class HitIterator implements Iterator<Hit> {

        protected class IteratorHit implements Hit {

            protected class HitProperty implements Property {

                protected class HitValue implements Value {

                    @Nonnull
                    protected final String text;

                    private transient List<String> path;
                    private transient Boolean absolute;
                    private transient Boolean relative;
                    private transient Boolean childPath;
                    private transient Boolean richText;

                    public HitValue(@Nonnull final String text) {
                        this.text = text;
                    }

                    @Override
                    public String toString() {
                        return getText();
                    }

                    @Override
                    @Nonnull
                    public String getText() {
                        return text;
                    }

                    @Override
                    @Nonnull
                    public List<String> getPath() {
                        if (path == null) {
                            path = new ArrayList<>();
                            boolean includeChildren = HitIterator.this.options.isIncludeChildren()
                                    || HitIterator.this.options.isChildrenOnly();
                            if (HitIterator.this.options.isUseAbsolutePath()) {
                                collectOccurences(path, getText(), HitIterator.this.getAbsPath(), includeChildren);
                            }
                            if (HitIterator.this.options.isUseRelativePath()) {
                                collectOccurences(path, getText(), HitIterator.this.getPath(), includeChildren);
                            }
                        }
                        return path;
                    }

                    @Override
                    public boolean isAbsolute() {
                        if (absolute == null) {
                            absolute = false;
                            for (String path : getPath()) {
                                if (path.startsWith(HitIterator.this.getAbsPath())) {
                                    absolute = true;
                                    break;
                                }
                            }
                        }
                        return absolute;
                    }

                    @Override
                    public boolean isRelative() {
                        if (relative == null) {
                            relative = false;
                            for (String path : getPath()) {
                                if (path.startsWith(HitIterator.this.getPath())) {
                                    relative = true;
                                    break;
                                }
                            }
                        }
                        return relative;
                    }

                    @Override
                    public boolean isChildPath() {
                        if (childPath == null) {
                            childPath = false;
                            for (String path : getPath()) {
                                if (!path.equals(HitIterator.this.getAbsPath())
                                        && !path.equals(HitIterator.this.getPath())) {
                                    childPath = true;
                                    break;
                                }
                            }
                        }
                        return childPath;
                    }

                    @Override
                    public boolean isRichText() {
                        if (richText == null) {
                            richText = getText().contains("=\"");
                        }
                        return richText;
                    }
                }

                protected final String name;
                protected final List<Value> value = new ArrayList<>();
                private transient Boolean richText;

                public HitProperty(@Nonnull final String name) {
                    this.name = name;
                }

                public HitProperty(@Nonnull final String name, @Nonnull String value) {
                    this.name = name;
                    addValue(value);
                }

                public boolean isEmpty() {
                    return value.isEmpty();
                }

                @Override
                @Nonnull
                public String getName() {
                    return name;
                }

                @Override
                @Nullable
                public Value getValue() {
                    return value.isEmpty() ? null : value.get(0);
                }

                @Override
                @Nonnull
                public List<Value> getValues() {
                    return value;
                }

                public void addValue(@Nonnull final String value) {
                    this.value.add(new HitValue(value));
                }

                @Override
                public boolean isRichText() {
                    if (richText == null) {
                        richText = false;
                        for (Value val : value) {
                            if (richText = val.isRichText()) {
                                break;
                            }
                        }
                    }
                    return richText;
                }
            }

            protected Resource resource;

            private transient Map<String, Property> properties;

            public IteratorHit(Resource resource) {
                this.resource = resource;
            }

            @Override
            @Nonnull
            public Resource getResource() {
                return resource;
            }

            @Nonnull
            protected Map<String, Property> getProperties() {
                if (properties == null) {
                    properties = new HashMap<>();
                    final ValueMap resourceValues = resource.getValueMap();
                    final String propertyName = HitIterator.this.options.getPropertyName();
                    if (StringUtils.isBlank(propertyName)) {
                        for (Map.Entry<String, Object> entry : resourceValues.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                String string = (String) value;
                                if (isMatchingValue(string)) {
                                    properties.put(key, new HitProperty(key, string));
                                }
                            } else if (value instanceof String[]) {
                                HitProperty property = extractMatchingValues(key, (String[]) value);
                                if (!property.isEmpty()) {
                                    properties.put(key, property);
                                }
                            }
                        }
                    } else {
                        final Object value = resourceValues.get(propertyName);
                        if (value instanceof String) {
                            if (isMatchingValue((String) value)) {
                                properties.put(propertyName, new HitProperty(propertyName, (String) value));
                            }
                        } else if (value instanceof String[]) {
                            HitProperty property = extractMatchingValues(propertyName, (String[]) value);
                            if (!property.isEmpty()) {
                                properties.put(propertyName, property);
                            }
                        }
                    }
                }
                return properties;
            }

            @Nonnull
            protected HitProperty extractMatchingValues(@Nonnull final String key, @Nonnull final String[] value) {
                HitProperty property = new HitProperty(key);
                for (String string : value) {
                    if (isMatchingValue(string)) {
                        property.addValue(string);
                    }
                }
                return property;
            }

            protected boolean isMatchingValue(@Nonnull final String value) {
                return (options.isUseAbsolutePath() && (value.startsWith(HitIterator.this.absPath)
                        || (options.isFindRichText() && value.contains("=\"" + HitIterator.this.absPath))))
                        || (options.isUseRelativePath() && (value.startsWith(HitIterator.this.path)
                        || (options.isFindRichText() && value.contains("=\"" + HitIterator.this.path))));
            }

            @Override
            @Nonnull
            public Set<String> getPropertyNames() {
                return getProperties().keySet();
            }

            @Override
            @Nullable
            public Property getProperty(@Nonnull final String propertyName) {
                return getProperties().get(propertyName);
            }

            @Override
            @Nullable
            public Property getProperty() {
                Map<String, Property> properties = getProperties();
                return properties.isEmpty() ? null : properties.values().iterator().next();
            }

            @Override
            @Nullable
            public Property.Value getValue() {
                Property property = getProperty();
                return property != null ? property.getValue() : null;
            }

            @Override
            public boolean isRichText() {
                Map<String, Property> properties = getProperties();
                return !properties.isEmpty() && properties.values().iterator().next().isRichText();
            }
        }

        protected final Options options;
        protected final String queryString;
        protected final String absPath;
        protected final String path;
        protected final Iterator<Resource> searchResult;

        protected IteratorHit next = null;

        public HitIterator(@Nonnull final Options options, @Nonnull final String queryString,
                           @Nonnull final String absPth, @Nonnull final String path,
                           @Nonnull final Iterator<Resource> searchResult) {
            this.options = options;
            this.queryString = queryString;
            this.absPath = absPth;
            this.path = path;
            this.searchResult = searchResult;
        }

        @Nullable
        protected Hit loadNext() {
            while (next == null && searchResult.hasNext()) {
                next = new IteratorHit(searchResult.next());
            }
            return next;
        }

        @Override
        public boolean hasNext() {
            return loadNext() != null;
        }

        @Override
        @Nullable
        public Hit next() {
            Hit result = loadNext();
            next = null;
            return result;
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        @Nonnull
        public String getAbsPath() {
            return absPath;
        }

        @Nonnull
        public String getQueryString() {
            return queryString;
        }
    }

    @Override
    @Nonnull
    public Iterator<Hit> findReferences(@Nonnull final ResourceResolver resolver, @Nonnull final Options options,
                                        @Nonnull String searchRoot, @Nonnull String path) {
        while (searchRoot.endsWith("/")) {
            searchRoot = searchRoot.substring(0, searchRoot.length() - 1);
        }
        if (StringUtils.isBlank(searchRoot)) {
            throw new IllegalArgumentException("repository root used as search root");
        }
        path = path.replace('*', '%');
        final String basePath = options.getBasePath().replace('*', '%');
        if (path.equals(basePath) || path.startsWith(basePath + "/")) {
            path = path.substring(basePath.length());
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        final String absPath = path.startsWith("/") ? path : (basePath + "/" + path);
        final String withoutQueryChars = absPath.replaceAll("[%]", "");
        if (StringUtils.isBlank(withoutQueryChars) || "/".equals(withoutQueryChars)) {
            throw new IllegalArgumentException("path is empty or the repository root path");
        }
        final String primaryType = options.getPrimaryType();
        final String resourceName = options.getResourceName();
        final String resourceType = options.getResourceType();
        String propertyName = options.getPropertyName();
        if (StringUtils.isBlank(propertyName)) {
            propertyName = "*";
        } else {
            propertyName = "@" + propertyName;
        }
        final StringBuilder queryBuilder = new StringBuilder("/jcr:root");
        queryBuilder.append(searchRoot).append("//");
        if (StringUtils.isNotBlank(primaryType)) {
            queryBuilder.append("element(").append(StringUtils.isNotBlank(resourceName) ? resourceName : "*")
                    .append(",").append(primaryType).append(")");
        } else {
            queryBuilder.append(StringUtils.isNotBlank(resourceName) ? resourceName : "*");
        }
        queryBuilder.append("[");
        int brackets = 0;
        if (StringUtils.isNotBlank(resourceType)) {
            queryBuilder.append("@sling:resourceType='").append(resourceType).append("' and (");
            brackets++;
        }
        if (options.isUseAbsolutePath()) {
            addPathExpression(options, queryBuilder, propertyName, absPath);
        }
        if (options.isUseRelativePath() && !path.startsWith("/")) {
            if (options.isUseAbsolutePath()) {
                queryBuilder.append(" or ");
            }
            addPathExpression(options, queryBuilder, propertyName, path);
        }
        while (--brackets >= 0) {
            queryBuilder.append(")");
        }
        queryBuilder.append("]");
        final String queryString = queryBuilder.toString();
        //noinspection deprecation
        return new HitIterator(options, queryString, absPath, path, resolver.findResources(queryString, Query.XPATH));
    }

    protected void addPathExpression(@Nonnull final Options options, @Nonnull final StringBuilder queryBuilder,
                                     @Nonnull final String propertyName, @Nonnull final String path) {
        if (!options.isChildrenOnly()) {
            if (!path.contains("%")) {
                queryBuilder.append("./").append(propertyName).append("='").append(path).append("'");
            } else {
                queryBuilder.append("jcr:like(./").append(propertyName).append(",'").append(path).append("')");
            }
            if (options.isFindRichText()) {
                queryBuilder.append(" or jcr:like(./").append(propertyName).append(",'%=\"").append(path).append("\"%')");
            }
        }
        if (options.isIncludeChildren() || options.isChildrenOnly()) {
            if (!options.isChildrenOnly()) {
                queryBuilder.append(" or ");
            }
            queryBuilder.append("jcr:like(./").append(propertyName).append(",'").append(path).append("/%')");
            if (options.isFindRichText()) {
                queryBuilder.append(" or jcr:like(./").append(propertyName).append(",'%=\"").append(path).append("/%')");
            }
        }
    }

    protected static void collectOccurences(@Nonnull final List<String> collection, @Nonnull final String text,
                                            @Nonnull final String path, boolean includeChildren) {
        collectOccurences(collection, text, path, propertyValuePattern(path, includeChildren));
        collectOccurences(collection, text, path, richTextPattern(path, includeChildren));
    }

    protected static void collectOccurences(@Nonnull final List<String> collection, @Nonnull final String text,
                                            @Nonnull final String path, @Nonnull final Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int pos = 0;
        while (matcher.find(pos)) {
            collection.add(matcher.group("path"));
            pos = matcher.end();
        }
    }

    protected static Pattern richTextPattern(@Nonnull final String path, boolean includeChildren) {
        return Pattern.compile("(=[\"'])(?<path>" + path + (includeChildren ? "(/[^\"']+)?" : "") + ")([\"'])");
    }

    protected static Pattern propertyValuePattern(@Nonnull final String path, boolean includeChildren) {
        return Pattern.compile("^(?<path>" + path + (includeChildren ? "(/.+)?" : "") + ")$");
    }
}
