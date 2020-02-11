package com.composum.sling.test.util;

import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Some utility methods for JCR.
 */
public class JcrTestUtils {

    /** Prints the paths of the descendants of a resource. */
    public static void listResourcesRecursively(@Nullable Resource resource) {
        if (resource != null) {
            System.out.println(resource.getPath());
            resource.getChildren().forEach(JcrTestUtils::listResourcesRecursively);
        }
    }

    /**
     * Prints a resource and its subresources as JSON, depth effectively unlimited.
     */
    public static void printResourceRecursivelyAsJson(@Nullable Resource resource) {
        if (resource != null) {
            try {
                StringWriter writer = new StringWriter();
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setHtmlSafe(true);
                jsonWriter.setLenient(true);
                jsonWriter.setSerializeNulls(false);
                jsonWriter.setIndent("    ");
                JsonUtil.exportJson(jsonWriter, resource, MappingRules.getDefaultMappingRules(), 99);
                // ensure uninterrupted printing : wait for logmessages being printed, flush
                Thread.sleep(200);
                System.err.flush();
                System.out.flush();
                System.out.println("JCR TREE FOR " + resource.getPath());
                System.out.println(writer);
                System.out.flush();
            } catch (RepositoryException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("NO RESOURCE");
        }
    }

    /**
     * Prints a resource and its subresources as JSON, depth effectively unlimited.
     */
    public static void printResourceRecursivelyAsJson(@Nullable ResourceResolver resolver, @Nullable String path) {
        if (resolver == null) {
            System.out.println("NO RESOLVER for printing resource");
        } else if (path == null) {
            System.out.println("INVALID NULL PATH");
        } else {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                printResourceRecursivelyAsJson(resource);
            } else {
                System.out.println("NO RESOURCE at " + path);
            }
        }
    }

    /**
     * Uses the varargs mechanism to easily construct an array - shorter than e.g. new String[]{objects...}.
     */
    @SafeVarargs
    @Nonnull
    public static <T> T[] array(@Nonnull T... objects) {
        return objects;
    }

    @Nonnull
    public static List<Resource> ancestorsAndSelf(@Nullable Resource r) {
        List<Resource> list = new ArrayList<>();
        while (r != null) {
            list.add(r);
            r = r.getParent();
        }
        return list;
    }

}
