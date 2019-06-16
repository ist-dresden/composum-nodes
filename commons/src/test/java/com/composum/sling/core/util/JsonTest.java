package com.composum.sling.core.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by rw on 18.05.15.
 */
public class JsonTest {

    public static JsonWriter createJsonWriter(Writer writer) {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(true);
        jsonWriter.setIndent("    ");
        return jsonWriter;
    }

    /**
     * Writes an object into a string and reinstantiates another object from this created JSON string;
     * the first JSON string created is than compared to a string created from the new instance.
     * @param object the test instance
     * @param gson the Gson transformer for JSON mapping
     */
    public static void testWriteReadWriteEquals(Object object, Gson gson) {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = createJsonWriter(writer);
        try {
            gson.toJson(object, object.getClass(), jsonWriter);
            String jsonValue = writer.toString();
            System.out.println("template:");
            System.out.println(jsonValue);
            System.out.println();
            StringReader reader = new StringReader(jsonValue);
            JsonReader jsonReader = new JsonReader(reader);
            Object newObject = gson.fromJson(jsonReader, object.getClass());
            writer = new StringWriter();
            jsonWriter = createJsonWriter(writer);
            gson.toJson(newObject, newObject.getClass(), jsonWriter);
            assertEquals(jsonValue, writer.toString());
            assertTrue("Sanity check failed: the JSON representation is suspiciously short - is this broken? " + jsonValue, jsonValue.length() > 10);
        } finally {
            System.out.println("new object:");
            System.out.println(writer.toString());
        }
    }
}
