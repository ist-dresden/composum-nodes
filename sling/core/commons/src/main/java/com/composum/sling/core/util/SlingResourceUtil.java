package com.composum.sling.core.util;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * A set of utility functions related to the handling of Sling Resources, without going down to JCR specifics.
 * This is a replacement for the JCR usage ridden {@link PropertyUtil} that contains all "pure" Sling functions that do not
 * require the Resources to be JCR resources.
 */
public class SlingResourceUtil {

    /**
     * Returns the relative path that leads from parent to child.
     * TODO: perhaps extend this to creating relative paths also when child isn't in subtree?
     *
     * @param parent the parent
     * @param child  a path of a child of parent
     * @return the path from which the child can be read from parent - e.g. with {@link org.apache.sling.api.resource.Resource#getChild(String)}. If parent and child are the same, this is empty.
     * @throws IllegalArgumentException if the child isn't a child of parent.
     */
    public static String relativePath(@Nonnull String parent, @Nonnull String child) {
        String result = null;
        if (parent.equals(child)) {
            result = "";
        } else if (child.startsWith(parent + '/')) {
            result = child.substring(parent.length() + 1);
        } else {
            String parentNormalized = ResourceUtil.normalize(parent);
            String childNormalized = ResourceUtil.normalize(child);
            if (parentNormalized == null || childNormalized == null)
                throw new IllegalArgumentException("Invalid path: parent=" + parent + " , child=" + child);
            if (!parentNormalized.endsWith("/")) parentNormalized = parentNormalized + "/";
            if (childNormalized.startsWith(parentNormalized)) {
                result = childNormalized.substring(parentNormalized.length());
            }
        }
        if (result == null)
            throw new IllegalArgumentException("Path of supposed child is not really a child path of parent: parent=" + parent + " , child=" + child);
        return result;
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

    /**
     * Returns the path of a resource, or null if it is null. For use e.g. in logging statements.
     * Caution when using UUIDs - they do not work on all resolvers and break on imports/exports.
     */
    @Nullable
    public static String getPath(@Nullable Resource resource) {
        return resource != null ? resource.getPath() : null;
    }

    /**
     * Adds a mixin if it isn't there already.
     *
     * @return true if we needed to add the mixin.
     */
    @CheckForNull
    public static boolean addMixin(@Nonnull Resource resource, @Nonnull String mixin) {
        if (!ResourceUtil.isResourceType(resource, mixin)) {
            ModifiableValueMap vm = resource.adaptTo(ModifiableValueMap.class);
            String[] mixins = vm.get(ResourceUtil.PROP_MIXINTYPES, new String[0]);
            List<String> newMixins = new ArrayList<String>(Arrays.asList(mixins));
            newMixins.add(mixin);
            vm.put(ResourceUtil.PROP_MIXINTYPES, newMixins.toArray(new String[newMixins.size()]));
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
