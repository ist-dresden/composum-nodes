package com.composum.sling.core.pckgmgr.jcrpckg.tree;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.StringWriter;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.stream.JsonWriter;

import junit.framework.TestCase;

public class TreeItemTest extends TestCase {

    static final Answer THROWING_ANSWER = new Answer() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void testFolderItem() throws Exception {
        FolderItem item = new FolderItem("/some/where", "folder");
        String json = toJson(item::toJson);
        System.out.println(json);
        assertEquals("{\n" +
                "  \"id\": \"/some/where\",\n" +
                "  \"path\": \"/some/where\",\n" +
                "  \"name\": \"folder\",\n" +
                "  \"text\": \"folder\",\n" +
                "  \"type\": \"folder\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": false\n" +
                "  }\n" +
                "}".trim(), json.trim());
    }

    @Test
    public void testTreeNode() throws Exception {
        TreeNode item = new TreeNode("/my/group");
        item.addPackage(makeJcrPackage("my/group", "pckg", "1.2"));
        item.addPackage(makeJcrPackage("is/ignored", "pckgign", "17"));
        String json = toJson(item::toJson);
        System.out.println(json);
        assertEquals("{\n" +
                "  \"id\": \"/my/group\",\n" +
                "  \"path\": \"/my/group\",\n" +
                "  \"name\": \"group\",\n" +
                "  \"text\": \"group\",\n" +
                "  \"type\": \"folder\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": false\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"definition\": {\n" +
                "        \"group\": \"my/group\",\n" +
                "        \"name\": \"pckg\",\n" +
                "        \"version\": \"1.2\",\n" +
                "        \"jcr:description\": \"descr pckg\",\n" +
                "        \"includeVersions\": false\n" +
                "      },\n" +
                "      \"id\": \"/my/group/pckg-1.2.zip\",\n" +
                "      \"path\": \"/my/group/pckg-1.2.zip\",\n" +
                "      \"name\": \"pckg-1.2.zip\",\n" +
                "      \"text\": \"pckg-1.2.zip\",\n" +
                "      \"type\": \"package\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": true\n" +
                "      },\n" +
                "      \"file\": \"pckg-1.2.zip\",\n" +
                "      \"packageid\": {\n" +
                "        \"name\": \"pckg\",\n" +
                "        \"group\": \"my/group\",\n" +
                "        \"version\": \"1.2\",\n" +
                "        \"downloadName\": \"pckg-1.2.zip\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}".trim(), json.trim());
    }

    private static JcrPackage makeJcrPackage(String group, String name, String version) throws Exception {
        JcrPackage jcrPackage = mock(JcrPackage.class, THROWING_ANSWER);
        JcrPackageDefinition def = mock(JcrPackageDefinition.class, THROWING_ANSWER);
        doReturn(def).when(jcrPackage).getDefinition();
        doReturn(group).when(def).get(JcrPackageDefinition.PN_GROUP);
        doReturn(name).when(def).get(JcrPackageDefinition.PN_NAME);
        doReturn(version).when(def).get(JcrPackageDefinition.PN_VERSION);
        doReturn("descr " + name).when(def).get(JcrPackageDefinition.PN_DESCRIPTION);
        doReturn(null).when(def).getCalendar(anyString());
        doReturn(false).when(def).getBoolean(anyString());
        return jcrPackage;
    }

    private <T> String toJson(ThrowingConsumer<JsonWriter> writeJson) throws Exception {
        StringWriter out = new StringWriter();
        try (JsonWriter writer = new JsonWriter(out)) {
            writer.setIndent("  ");
            writeJson.accept(writer);
        }
        return out.toString();
    }

    @FunctionalInterface
    private static interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

}
