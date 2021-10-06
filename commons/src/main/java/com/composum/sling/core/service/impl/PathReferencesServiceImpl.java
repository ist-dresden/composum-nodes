package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.PathReferencesService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    protected static class HitIteratorImpl implements HitIterator {

        protected class IteratorHit implements Hit {

            protected class HitProperty implements Property {

                protected class HitValue implements Value {

                    @Nonnull
                    protected final String text;
                    protected final int index;

                    private transient List<String> paths;
                    private transient Boolean absolute;
                    private transient Boolean relative;
                    private transient Boolean childPath;
                    private transient Boolean richText;

                    public HitValue(@Nonnull final String text, int index) {
                        this.text = text;
                        this.index = index;
                    }

                    @Override
                    public String toString() {
                        return getText();
                    }

                    public int getIndex() {
                        return index;
                    }

                    @Nonnull
                    public String apply(@Nonnull final String newPath) {
                        return isRichText() ? applyText(newPath) : applyVal(newPath);
                    }

                    @Nonnull
                    protected String applyVal(@Nonnull final String newPath) {
                        final String basePath = HitIteratorImpl.this.options.getBasePath();
                        final String relPath = relPath(basePath, newPath);
                        if (isAbsolute()) {
                            return getText().replaceAll("^" + HitIteratorImpl.this.absPath, absPath(basePath, relPath));
                        } else {
                            return getText().replaceAll("^" + HitIteratorImpl.this.relPath, relPath);
                        }
                    }

                    @Nonnull
                    protected String applyText(@Nonnull final String newPath) {
                        final String basePath = HitIteratorImpl.this.options.getBasePath();
                        final String relPath = relPath(basePath, newPath);
                        if (isAbsolute()) {
                            return getText().replaceAll("^=\"" + HitIteratorImpl.this.absPath, "=\"" + absPath(basePath, relPath));
                        } else {
                            return getText().replaceAll("^=\"" + HitIteratorImpl.this.relPath, "=\"" + relPath);
                        }
                    }

                    @Override
                    @Nonnull
                    public String getText() {
                        return text;
                    }

                    @Override
                    @Nonnull
                    public List<String> getPaths() {
                        if (paths == null) {
                            paths = new ArrayList<>();
                            boolean includeChildren = HitIteratorImpl.this.options.isIncludeChildren()
                                    || HitIteratorImpl.this.options.isChildrenOnly();
                            if (HitIteratorImpl.this.options.isUseAbsolutePath()) {
                                collectOccurrences(paths, getText(), HitIteratorImpl.this.getAbsPath(), includeChildren);
                            }
                            if (HitIteratorImpl.this.options.isUseRelativePath()) {
                                collectOccurrences(paths, getText(), HitIteratorImpl.this.getRelPath(), includeChildren);
                            }
                        }
                        return paths;
                    }

                    @Override
                    public boolean isAbsolute() {
                        if (absolute == null) {
                            absolute = false;
                            for (String path : getPaths()) {
                                if (path.startsWith(HitIteratorImpl.this.getAbsPath())) {
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
                            for (String path : getPaths()) {
                                if (path.startsWith(HitIteratorImpl.this.getRelPath())) {
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
                            for (String path : getPaths()) {
                                if (!path.equals(HitIteratorImpl.this.getAbsPath())
                                        && !path.equals(HitIteratorImpl.this.getRelPath())) {
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
                protected final boolean multi;
                private transient Boolean richText;

                public HitProperty(@Nonnull final String name) {
                    this.name = name;
                    multi = true;
                }

                public HitProperty(@Nonnull final String name, @Nonnull String value) {
                    this.name = name;
                    multi = false;
                    addValue(value, 0);
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
                public String getText() {
                    final Value value = getValue();
                    return value != null ? value.getText() : "";
                }

                @Override
                @Nonnull
                public List<Value> getValues() {
                    return value;
                }

                protected void addValue(@Nonnull final String value, int index) {
                    this.value.add(new HitValue(value, index));
                }

                @Override
                public boolean isMulti() {
                    return multi;
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

            private transient Map<String, Property> propertyMap;

            public IteratorHit(Resource resource) {
                this.resource = resource;
            }

            @Override
            @Nonnull
            public Resource getResource() {
                return resource;
            }

            @Nonnull
            protected Map<String, Property> getPropertyMap() {
                if (propertyMap == null) {
                    propertyMap = new HashMap<>();
                    final ValueMap resourceValues = resource.getValueMap();
                    final String propertyName = HitIteratorImpl.this.options.getPropertyName();
                    if (StringUtils.isBlank(propertyName)) {
                        for (Map.Entry<String, Object> entry : resourceValues.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                String string = (String) value;
                                if (isMatchingValue(string)) {
                                    propertyMap.put(key, new HitProperty(key, string));
                                }
                            } else if (value instanceof String[]) {
                                HitProperty property = extractMatchingValues(key, (String[]) value);
                                if (!property.isEmpty()) {
                                    propertyMap.put(key, property);
                                }
                            }
                        }
                    } else {
                        final Object value = resourceValues.get(propertyName);
                        if (value instanceof String) {
                            if (isMatchingValue((String) value)) {
                                propertyMap.put(propertyName, new HitProperty(propertyName, (String) value));
                            }
                        } else if (value instanceof String[]) {
                            HitProperty property = extractMatchingValues(propertyName, (String[]) value);
                            if (!property.isEmpty()) {
                                propertyMap.put(propertyName, property);
                            }
                        }
                    }
                }
                return propertyMap;
            }

            @Nonnull
            protected HitProperty extractMatchingValues(@Nonnull final String key, @Nonnull final String[] value) {
                HitProperty property = new HitProperty(key);
                for (int i = 0; i < value.length; i++) {
                    if (isMatchingValue(value[i])) {
                        property.addValue(value[i], i);
                    }
                }
                return property;
            }

            protected boolean isMatchingValue(@Nonnull final String value) {
                final boolean searchRelative = options.isUseRelativePath() && !HitIteratorImpl.this.getRelPath().startsWith("/");
                return ((options.isUseAbsolutePath() || !searchRelative) && isMatchingValue(value, HitIteratorImpl.this.getAbsPath()))
                        || (searchRelative && isMatchingValue(value, HitIteratorImpl.this.getRelPath()));
            }

            protected boolean isMatchingValue(@Nonnull final String value, @Nonnull final String path) {
                return isMatchingVal(value, path) || (options.isFindRichText() && isMatchingText(value, path));
            }

            protected boolean isMatchingVal(@Nonnull final String value, @Nonnull final String path) {
                return options.isChildrenOnly() ? value.startsWith(path + "/") :
                        (value.equals(path) || (options.isIncludeChildren() && value.startsWith(path + "/")));
            }

            protected boolean isMatchingText(@Nonnull final String value, @Nonnull final String path) {
                return ((options.isChildrenOnly() || options.isIncludeChildren()) && value.contains("=\"" + path + "/"))
                        || (!options.isChildrenOnly() && value.contains("=\"" + path + "\""));
            }

            @Override
            @Nonnull
            public Iterable<Property> getProperties() {
                return getPropertyMap().values();
            }

            @Override
            @Nonnull
            public Set<String> getPropertyNames() {
                return getPropertyMap().keySet();
            }

            @Override
            @Nullable
            public Property getProperty(@Nonnull final String propertyName) {
                return getPropertyMap().get(propertyName);
            }

            @Override
            @Nullable
            public Property getProperty() {
                final Map<String, Property> props = getPropertyMap();
                return props.isEmpty() ? null : props.values().iterator().next();
            }

            @Override
            @Nullable
            public Property.Value getValue() {
                final Property property = getProperty();
                return property != null ? property.getValue() : null;
            }

            @Override
            public boolean isRichText() {
                final Map<String, Property> props = getPropertyMap();
                return !props.isEmpty() && props.values().iterator().next().isRichText();
            }
        }

        protected final Options options;
        protected final String queryString;
        protected final String absPath;
        protected final String relPath;
        protected final Iterator<Resource> searchResult;
        protected final Throwable throwable;

        protected IteratorHit next = null;

        public HitIteratorImpl(@Nonnull final Options options, @Nonnull final String queryString,
                               @Nonnull final String absPath, @Nonnull final String relPath,
                               @Nonnull final Iterator<Resource> searchResult) {
            this.options = options;
            this.queryString = queryString;
            this.absPath = absPath;
            this.relPath = relPath;
            this.searchResult = searchResult;
            this.throwable = null;
        }

        public HitIteratorImpl(@Nonnull final Options options, @Nonnull final String queryString,
                               @Nonnull final String absPath, @Nonnull final String relPath,
                               @Nonnull final Throwable throwable) {
            this.options = options;
            this.queryString = queryString;
            this.absPath = absPath;
            this.relPath = relPath;
            this.searchResult = Collections.emptyIterator();
            this.throwable = throwable;
        }

        @Nullable
        protected Hit loadNext() {
            while (next == null && searchResult.hasNext()) {
                final IteratorHit hit = new IteratorHit(searchResult.next());
                if (!hit.getPropertyMap().isEmpty()) {
                    next = hit;
                }
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
        public String getRelPath() {
            return relPath;
        }

        @Nonnull
        public String getAbsPath() {
            return absPath;
        }

        @Override
        @Nonnull
        public String getQueryString() {
            return queryString;
        }

        @Override
        @Nullable
        public Throwable getThrowable() {
            return throwable;
        }
    }

    @Override
    @Nonnull
    public HitIterator findReferences(@Nonnull final ResourceResolver resolver, @Nonnull final Options options,
                                      @Nonnull String searchRoot, @Nonnull String path) {
        while (searchRoot.endsWith("/")) {
            searchRoot = searchRoot.substring(0, searchRoot.length() - 1);
        }
        if (StringUtils.isBlank(searchRoot)) {
            throw new IllegalArgumentException("repository root used as search root");
        }
        path = path.replace('*', '%');
        final String basePath = options.getBasePath().replace('*', '%');
        final String relPath = relPath(basePath, path);
        final String absPath = absPath(basePath, relPath);
        final String withoutQueryChars = absPath.replaceAll("[%]", "");
        if (StringUtils.isBlank(withoutQueryChars) || "/".equals(withoutQueryChars)) {
            throw new IllegalArgumentException("path is empty or the repository root path");
        }
        final String primaryType = options.getPrimaryType();
        final String contentPath = options.getContentPath();
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
        if (StringUtils.isNotBlank(contentPath)) {
            queryBuilder.append(contentPath.startsWith("/") ? contentPath : "/" + contentPath);
        }
        queryBuilder.append("[");
        int brackets = 0;
        if (StringUtils.isNotBlank(resourceType)) {
            queryBuilder.append("@sling:resourceType='").append(resourceType).append("' and (");
            brackets++;
        }
        final boolean searchRelative = options.isUseRelativePath() && !relPath.startsWith("/");
        if (options.isUseTextSearch()) {
            queryBuilder.append("jcr:contains(").append(propertyName.startsWith("@") ? propertyName : ".")
                    .append(",'").append(searchRelative ? relPath : absPath).append("')");
        } else {
            if (options.isUseAbsolutePath() || !searchRelative) {
                addPathExpression(options, queryBuilder, propertyName, absPath);
            }
            if (searchRelative) {
                if (options.isUseAbsolutePath()) {
                    queryBuilder.append(" or ");
                }
                addPathExpression(options, queryBuilder, propertyName, relPath);
            }
        }
        while (--brackets >= 0) {
            queryBuilder.append(")");
        }
        queryBuilder.append("]");
        final String queryString = queryBuilder.toString();
        try {
            //noinspection deprecation
            Iterator<Resource> result = resolver.findResources(queryString, Query.XPATH);
            return new HitIteratorImpl(options, queryString, absPath, relPath, result);
        } catch (Exception ex) {
            return new HitIteratorImpl(options, queryString, absPath, relPath, ex);
        }
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

    @Override
    public void changeReferences(@Nonnull final ResourceResolver resolver,
                                 @Nonnull final Hit hit, @Nonnull final String newPath) {
        final ModifiableValueMap modifiable = hit.getResource().adaptTo(ModifiableValueMap.class);
        if (modifiable != null) {
            for (final Hit.Property property : hit.getProperties()) {
                final String name = property.getName();
                final Object current = modifiable.get(name);
                if (current instanceof String) {
                    final Hit.Property.Value value = property.getValue();
                    if (value != null) {
                        modifiable.put(name, value.apply(newPath));
                    }
                } else if (current instanceof String[]) {
                    final String[] values = (String[]) current;
                    final String[] modified = Arrays.copyOf(values, values.length);
                    for (Hit.Property.Value value : property.getValues()) {
                        final int index = value.getIndex();
                        if (index < modified.length) {
                            modified[index] = value.apply(newPath);
                        }
                    }
                    modifiable.put(name, modified);
                }
            }
        }
    }

    protected static void collectOccurrences(@Nonnull final List<String> collection, @Nonnull final String text,
                                             @Nonnull final String path, boolean includeChildren) {
        collectOccurrences(collection, text, propertyValuePattern(path, includeChildren));
        collectOccurrences(collection, text, richTextPattern(path, includeChildren));
    }

    protected static void collectOccurrences(@Nonnull final List<String> collection, @Nonnull final String text,
                                             @Nonnull final Pattern pattern) {
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

    @Nonnull
    protected static String relPath(@Nonnull final String basePath, @Nonnull String path) {
        if (path.equals(basePath) || path.startsWith(basePath + "/")) {
            path = path.substring(basePath.length());
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        return path;
    }

    @Nonnull
    protected static String absPath(@Nonnull final String basePath, @Nonnull final String relPath) {
        return relPath.startsWith("/") ? relPath : (basePath + "/" + relPath);
    }
}
