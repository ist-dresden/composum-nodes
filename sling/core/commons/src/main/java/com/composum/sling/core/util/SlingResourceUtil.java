package com.composum.sling.core.util;

import javax.annotation.Nonnull;

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
}
