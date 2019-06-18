package com.composum.sling.core.util;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A set of utility functions related to the handling of Sling Resources, without going down to JCR specifics.
 * This is a replacement for the JCR usage ridden {@link PropertyUtil} that contains all "pure" Sling functions that do not
 * require the Resources to be JCR resources.
 */
public class SlingResourceUtil {

    /**
     * Returns the shortest relative path that leads from a node to another node.
     * Postcondition: <code>ResourceUtil.normalize(node + "/" + result), is(ResourceUtil.normalize(other))</code>.
     *
     * @param node the parent
     * @param other  a path of a child of parent
     * @return the path from which other can be read from node - e.g. with {@link org.apache.sling.api.resource.Resource#getChild(String)}. If node and other are the same, this is empty.
     * @throws IllegalArgumentException in those cases where there is no sensible answer: one of the paths is empty or one absolute and one relative path
     */
    public static String relativePath(@Nonnull String node, @Nonnull String other) {
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
            if (!node.endsWith("/"))
                node = node + "/";
            if (other.startsWith(node)) {
                return other.substring(node.length());
            } else {
                String longestPrefix = StringUtils.getCommonPrefix(node, other);
                if (!longestPrefix.endsWith("/"))
                    longestPrefix = StringUtils.defaultString(ResourceUtil.getParent(longestPrefix));
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
        if (parent == null || descendant == null) return false;
        if (parent.equals(descendant) || parent.equals("/")) return true;
        if (descendant.startsWith(parent + '/')) return true;
        String parentNormalized = ResourceUtil.normalize(parent);
        String descendantNormalized = ResourceUtil.normalize(descendant);
        return parentNormalized.equals(descendantNormalized) || parentNormalized.equals("/")
                || descendantNormalized.startsWith(parentNormalized + '/');
    }

    /** Returns the path of a resource, or null if it is null. For use e.g. in logging statements. */
    @Nullable
    public static String getPath(@Nullable Resource resource) {
        return resource != null ? resource.getPath() : null;
    }

    /** Returns the list of paths of a number of resources. For use e.g. in logging statements. */
    @Nonnull
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
    public static boolean addMixin(@Nonnull Resource resource, @Nonnull String mixin) {
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
     * @return an iterable running through the descendants, not null
     */
    @Nonnull
    public static Iterable<Resource> descendants(@Nullable final Resource resource) {
        // this is awful because we are stuck at Java 7 for now. With Java 8 Streams this is way easier.
        if (resource == null) return Collections.emptyList();
        return new Iterable<Resource>() {
            @Nonnull
            @SuppressWarnings("unchecked")
            @Override
            public Iterator<Resource> iterator() {
                Transformer descendantsTransformer = new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        if (input instanceof Resource) {
                            return IteratorUtils.chainedIterator(
                                    // we wrap the resource into Object[] so that it doesn't get its children read again
                                    IteratorUtils.singletonIterator(new Object[]{input}),
                                    ((Resource) input).listChildren());
                        }
                        return input;
                    }
                };
                return IteratorUtils.transformedIterator(IteratorUtils.objectGraphIterator(resource, descendantsTransformer),
                        new Transformer() {
                            @Override
                            public Object transform(Object input) {
                                return ((Object[]) input)[0];
                            }
                        });
            }

        };
    }

}
