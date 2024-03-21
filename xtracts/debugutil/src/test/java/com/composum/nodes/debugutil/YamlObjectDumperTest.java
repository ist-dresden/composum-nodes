package com.composum.nodes.debugutil;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class YamlObjectDumperTest {

    private YamlObjectDumper dumper;

    @Before
    public void setUp() {
        dumper = new YamlObjectDumper();
        dumper.printLineNumbers = false;
    }

    @Test
    public void dumpsNullAsNullString() {
        String result = dumper.dump(null, 1);
        assertEquals("null", result);
    }

    @Test
    public void dumpsStringWithEscapedQuotes() {
        String result = dumper.dump("test\"quote", 1);
        assertEquals("\"test\\\"quote\"", result);
    }

    @Test
    public void dumpsNumberAsIs() {
        String result = dumper.dump(123, 1);
        assertEquals("123", result);
    }

    @Test
    public void dumpsBooleanAsIs() {
        String result = dumper.dump(true, 1);
        assertEquals("true", result);
    }

    @Test
    public void dumpsIterableAsYamlList() {
        String result = dumper.dump(Arrays.asList("item1", "item2"), 2);
        assertEquals("- \"item1\"\n- \"item2\"", result);
    }

    @Test
    public void dumpsMapAsYamlMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        String result = dumper.dump(map, 2);
        assertEquals("key1: \"value1\"\n" +
                "key2: \"value2\"", result);
    }

    @Test
    public void dumpsObjectWithPublicMethods() {
        String result = dumper.dump(new ObjectWithPublicMethods(), 2);
        assertEquals("method1: \"value1\"\n" +
                "method2: \"value2\"", result);
    }

    @Test
    public void stopsRecursionAtMaxDepth() {
        String result = dumper.dump(Arrays.asList(Arrays.asList("item")), 0);
        assertEquals("- ...", result);
    }

    @Test
    public void marksSeenObjects() {
        Object seenObject = new ObjectWithPublicMethods();
        String result = dumper.dump(Arrays.asList(seenObject, seenObject), 2);
        assertEquals("- \n" +
                "  method1: \"value1\"\n" +
                "  method2: \"value2\"\n" +
                "- \"object was already seen at line 1\"", result);
    }

    @Test
    public void mixedTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("0bar", Arrays.asList("item1", "item2"));
        map.put("2foo", 17);
        map.put("3baz", new ObjectWithPublicMethods());
        map.put("1qux", Arrays.asList(new ObjectWithPublicMethods()));
        Map<String, Object> map2 = new HashMap<>();
        map2.put("one", 1);
        map2.put("two", "two");
        map.put("4quux", map2);

        String result = dumper.dump(map, 2);
        assertEquals("" +
                "0bar: \n" +
                "  - \"item1\"\n" +
                "  - \"item2\"\n" +
                "1qux: \n" +
                "  - \n" +
                "    method1: \"value1\"\n" +
                "    method2: \"value2\"\n" +
                "2foo: 17\n" +
                "3baz: \n" +
                "  method1: \"value1\"\n" +
                "  method2: \"value2\"\n" +
                "4quux: \n" +
                "  one: 1\n" +
                "  two: \"two\"", result);
    }

    @Test
    public void classTest() {
        String result = dumper.dump(YamlObjectDumperTest.class, 2);
        assertEquals("\"class com.composum.nodes.debugutil.YamlObjectDumperTest\"", result);
    }

    public static class ObjectWithPublicMethods {
        public String method1() {
            return "value1";
        }

        public String method2() {
            return "value2";
        }
    }
}
