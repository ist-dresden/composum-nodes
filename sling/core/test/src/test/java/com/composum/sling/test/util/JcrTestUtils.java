package com.composum.sling.test.util;

import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.resource.Resource;

import java.io.StringWriter;

/**
 * Some utility methods for JCR.
 */
public class JcrTestUtils {

    /** Prints a resource and its subresources as JSON, depth effectively unlimited. */
    public static void printResourceRecursivelyAsJson(Resource resource) throws Exception {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(true);
        jsonWriter.setIndent("    ");
        JsonUtil.exportJson(jsonWriter, resource, MappingRules.getDefaultMappingRules(), 99);
        System.out.println(writer);
    }

    /** Uses the varargs mechanism to easily construct an array - shorter than e.g. new String[]{objects...}. */
    public static <T> T[] array(T... objects) {
        return objects;
    }

}
