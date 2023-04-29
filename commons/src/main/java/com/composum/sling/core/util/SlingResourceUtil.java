package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * A set of utility functions related to the handling of Sling Resources, without going down to JCR specifics.
 * This is a replacement for the JCR usage ridden {@link PropertyUtil} that contains all "pure" Sling functions that do not
 * require the Resources to be JCR resources.
 */
public class SlingResourceUtil {

    /**
     * Returns the shortest relative path that leads from a node to another node.
     * Postcondition: {@code ResourceUtil.normalize(node + "/" + result), is(ResourceUtil.normalize(other))}.
     * In most cases  {@link #appendPaths(String, String)}(node, result) will return {other}. )}
     * <p>
     * Examples: relativePath("/foo", "/foo/bar") = "bar" ; relativePath("/foo/bar", "/foo") = ".." ;
     * relativePath("foo", "foo") = "../bar" .
     *
     * @param node  the parent
     * @param other a path of a child of parent / parent itself
     * @return the (relative) path from which other can be read from node - e.g. with
     * {@link org.apache.sling.api.resource.Resource#getChild(String)}. If node and other are the same, this is empty.
     * @throws IllegalArgumentException in those cases where there is no sensible answer: one of the paths is empty or one absolute and one relative path
     */
    public static String relativePath(@NotNull String node, @NotNull String other) {
        node = ResourceUtil.normalize(node);
        other = ResourceUtil.normalize(other);
        //noinspection IfStatementWithTooManyBranches,OverlyComplexBooleanExpression
        if (StringUtils.isBlank(node) || StringUtils.isBlank(other)
                || (StringUtils.startsWith(node, "/") && !StringUtils.startsWith(other, "/"))
                || (!StringUtils.startsWith(node, "/") && StringUtils.startsWith(other, "/"))
        ) { // no sensible answer here
            throw new IllegalArgumentException("Invalid path: node=" + node + " , other=" + other);
        } else if (node.equals(other)) {
            return "";
        } else if (other.startsWith(node + '/')) {
            return other.substring(node.length() + 1);
        } else {
            if (!node.endsWith("/")) {
                node = node + "/";
            }
            if (other.startsWith(node)) {
                return other.substring(node.length());
            } else {
                String longestPrefix = StringUtils.getCommonPrefix(node, other);
                if (!longestPrefix.endsWith("/")) {
                    longestPrefix = StringUtils.defaultString(ResourceUtil.getParent(longestPrefix));
                }
                String nodeRestpath = other.substring(longestPrefix.length());
                String otherRestpath = node.substring(longestPrefix.length());
                return StringUtils.repeat("../", StringUtils.countMatches(otherRestpath, "/")) + nodeRestpath;
            }
        }
    }

    /**
     * Checks whether {descendant} is the same path as parent node or a path of a descendant of the parent node. (We don't check
     * whether the resources exist - just check the paths.
     *
     * @param parent     the parent or null
     * @param descendant the descendant or null
     * @return true if descendant is a descendant of parent , false if any is null.
     */
    public static boolean isSameOrDescendant(@Nullable String parent, @Nullable String descendant) {
        if (parent == null || descendant == null) {
            return false;
        }
        if (parent.equals(descendant) || parent.equals("/")) {
            return true;
        }
        if (descendant.startsWith(parent + '/') && !descendant.contains("..")) {
            return true;
        }
        String parentNormalized = ResourceUtil.normalize(parent);
        String descendantNormalized = ResourceUtil.normalize(descendant);
        return parentNormalized.equals(descendantNormalized) || parentNormalized.equals("/")
                || descendantNormalized.startsWith(parentNormalized + '/');
    }

    /**
     * Checks whether {descendant} is the same path as parent node or a path of a descendant of the parent node.
     *
     * @param parent     the parent or null
     * @param descendant the descendant or null
     * @return true if descendant is a descendant of parent , false if any is null.
     */
    public static boolean isSameOrDescendant(@Nullable Resource parent, @Nullable Resource descendant) {
        if (parent == null || descendant == null) {
            return false;
        }
        return isSameOrDescendant(parent.getPath(), descendant.getPath());
    }

    /**
     * Returns the path of a resource, or null if it is null. For use e.g. in logging statements.
     */
    @Nullable
    public static String getPath(@Nullable Resource resource) {
        return resource != null ? resource.getPath() : null;
    }

    /**
     * Returns the list of paths of a number of resources. For use e.g. in logging statements.
     */
    @NotNull
    public static List<String> getPaths(@Nullable List<Resource> resources) {
        List<String> paths = new ArrayList<>();
        if (resources != null) {
            for (Resource resource : resources) {
                paths.add(getPath(resource));
            }
        }
        return paths;
    }

    /**
     * Adds a mixin if it isn't there already.
     *
     * @return true if we needed to add the mixin.
     */
    public static boolean addMixin(@NotNull Resource resource, @NotNull String mixin) {
        if (!ResourceUtil.isResourceType(resource, mixin)) {
            ModifiableValueMap vm = resource.adaptTo(ModifiableValueMap.class);
            String[] mixins = vm.get(ResourceUtil.PROP_MIXINTYPES, new String[0]);
            List<String> newMixins = new ArrayList<>(Arrays.asList(mixins));
            newMixins.add(mixin);
            vm.put(ResourceUtil.PROP_MIXINTYPES, newMixins.toArray(new String[0]));
            return true;
        }
        return false;
    }

    /**
     * Returns an iterator that goes through all descendants of a resource, parents come before their children.
     *
     * @param resource a resource or null
     * @return an iterable running through the resource and it's descendants, not null
     */
    @NotNull
    public static Iterable<Resource> descendants(@Nullable final Resource resource) {
        return () -> descendantsStream(resource).iterator();
    }

    /**
     * Returns a stream that goes through all descendants of a resource, parents come before their children.
     *
     * @param resource a resource or null
     * @return a stream running through the resource and it's the descendants, not null
     */
    @NotNull
    public static Stream<Resource> descendantsStream(@Nullable Resource resource) {
        return descendantsStream(resource, null);
    }

    /**
     * Returns a stream that goes through all descendants of a resource until a filter is hit, parents come before
     * their children.
     *
     * @param resource   a resource or null
     * @param leafFilter if this returns true, this assumes the resource is a leaf and does not return it's descendants.
     * @return a stream running through the resource and it's the descendants, not null
     */
    @NotNull
    public static Stream<Resource> descendantsStream(@Nullable Resource resource,
                                                     @Nullable Function<Resource, Boolean> leafFilter) {
        if (resource == null) {
            return Stream.empty();
        }
        if (leafFilter != null && Boolean.TRUE.equals(leafFilter.apply(resource))) {
            return Stream.of(resource);
        }
        return Stream.concat(Stream.of(resource),
                StreamSupport.stream(resource.getChildren().spliterator(), false)
                        .flatMap((r) -> SlingResourceUtil.descendantsStream(r, leafFilter)));
    }

    /**
     * Returns a stream of the resource and all its parents.
     */
    @NotNull
    public static Stream<Resource> selfAndAncestors(@Nullable Resource r) {
        if (r == null) {
            return Stream.empty();
        }
        // yes, that should be done with takeWhile, but we are restricted to Java 8 here for now.
        return Stream.iterate(r, Resource::getParent)
                .limit(StringUtils.countMatches(r.getPath(), "/"))
                .filter(Objects::nonNull);
    }

    /**
     * Appends two paths: determines the given child of a path.
     *
     * @param path      an absolute or relative path. We ignore if it ends with /. If it's null, we return null
     *                  (there is no child of no path).
     * @param childpath the relative path of the child to the path. Absolute paths are treated as relative paths: we
     *                  ignore starting and ending / . if empty or null we return path
     * @return the paths concatenated. If path is absolute, this is absolute; if path is relative, this is relative.
     */
    @Nullable
    public static String appendPaths(@Nullable String path, @Nullable String childpath) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        if (StringUtils.isBlank(childpath)) {
            return path;
        }
        childpath = StringUtils.removeStart(childpath, "/");
        childpath = StringUtils.removeEnd(childpath, "/");
        if (StringUtils.isBlank(childpath)) {
            return path;
        }
        if ("/".equals(ResourceUtil.normalize(path))) {
            return "/" + childpath;
        }
        path = StringUtils.removeEnd(path, "/");
        return path + "/" + childpath;
    }

    /**
     * Finds the longest path that is a parent of all given paths - e.g. /foo/bar for /foo/bar/a/b, /foo/bar/a/c and
     * /foo/bar/b/c .
     *
     * @param paths a collection of absolute paths or a collection of relative paths. Null / empty values are ignored.
     * @return null if collection is empty, otherwise the common parent. It might be an empty string if the paths
     * have no common parent (e.g. "a" and "b" or the illegitimate call with "/a" and "b").
     */
    @Nullable
    public static String commonParent(@Nullable Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        String result = null;
        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }
            if (result == null) {
                result = path;
            } else {
                while (!isSameOrDescendant(result, path) && result != null) {
                    result = ResourceUtil.getParent(result);
                }
                if (result == null) {
                    break;
                } // no common parents
            }
        }
        return result;
    }

    /**
     * Retrieves the first parent (including path itself) that actually exists.
     *
     * @param path an absolute path
     * @return path or the longest path parent to path that exists in resolver. Null if path
     * is empty.
     */
    @Nullable
    public static Resource getFirstExistingParent(@Nullable ResourceResolver resolver, @Nullable String path) {
        if (resolver == null) {
            return null;
        }
        String searchedPath = path;
        Resource result = StringUtils.isNotBlank(searchedPath) ? resolver.getResource(searchedPath) : null;
        while (result == null && StringUtils.isNotBlank(searchedPath)) {
            searchedPath = ResourceUtil.getParent(searchedPath);
            result = resolver.getResource(searchedPath);
        }
        return result;
    }

    /**
     * Adds one or more mixins to the resource, keeping the current ones.
     */
    public static void addMixin(@Nullable Resource resource, @Nullable String... mixins) {
        if (resource != null && mixins != null && mixins.length > 0) {
            ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
            String[] currentMixins = mvm.get(ResourceUtil.PROP_MIXINTYPES, String[].class);
            if (currentMixins == null) {
                mvm.put(ResourceUtil.PROP_MIXINTYPES, mixins);
            } else {
                List<String> allMixins = new ArrayList<>();
                allMixins.addAll(Arrays.asList(currentMixins));
                for (String mixin : mixins) {
                    if (!allMixins.contains(mixin)) {
                        allMixins.add(mixin);
                    }
                }
                mvm.put(ResourceUtil.PROP_MIXINTYPES, allMixins.toArray(new String[allMixins.size()]));
            }
        }
    }

    /**
     * Sets a property on a {@link ModifiableValueMap}. This abbreviates the procedure if it's not known whether
     * the value is null - {@link ModifiableValueMap#put(Object, Object)} throws up if it is.
     */
    public static void setProperty(@NotNull ModifiableValueMap valueMap, @NotNull String key, @Nullable Object value) {
        if (value == null) {
            valueMap.remove(key);
        } else {
            valueMap.put(key, value);
        }
    }

    /**
     * Returns the children of a resource as {@link Stream}.
     *
     * @param a resource or null
     * @return stream with the children, possibly empty.
     */
    @Nonnull
    public static Stream<Resource> getChildrenAsStream(@Nullable Resource resource) {
        if (resource == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(resource.getChildren().spliterator(), false);
    }

}
