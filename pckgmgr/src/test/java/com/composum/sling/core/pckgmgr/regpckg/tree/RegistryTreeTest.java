package com.composum.sling.core.pckgmgr.regpckg.tree;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.google.gson.stream.JsonWriter;

public class RegistryTreeTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    private BeanContext context = new BeanContext.Map();
    @Mock
    private PackageRegistries registriesService;
    @Mock
    private PackageRegistries.Registries registries;
    @Mock
    PackageRegistry registry;
    @Mock
    SlingBindings bindings;
    @Mock
    SlingScriptHelper script;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(registriesService.getRegistries(any())).thenReturn(registries);
        when(registries.getNamespaces()).thenReturn(new HashSet<>(asList("jcr")));
        when(registries.getRegistry(any())).thenReturn(registry);
        context.setAttribute(SlingBindings.class.getName(), bindings, BeanContext.Scope.request);
        when(bindings.getSling()).thenReturn(script);
        when(script.getService(PackageRegistries.class)).thenReturn(registriesService);
    }

    String toJson(boolean merged, BeanContext context, String path) throws IOException {
        RegistryTree tree = new RegistryTree(merged);
        RegistryItem treeItem = path != null ? tree.getItem(context, path) : tree;
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        writer.setIndent("  ");
        writer.setHtmlSafe(true);
        if (context != null && !treeItem.isLoaded()) {
            treeItem.load(context);
        }
        treeItem = treeItem.compactTree();
        treeItem.toTree(writer, true, true);
        // that's a class name that changes sometimes:
        String normalizedResult = out.toString().replaceAll("PackageRegistry.MockitoMock(\\$?\\w)+", "JcrPackageRegistry");
        normalizedResult = normalizedResult.replaceAll("JcrPackageRegistryAeJvsw", "JcrPackageRegistry");
        System.out.println();
        System.out.println("####### " + treeItem.getPath() + " #######");
        System.out.println(normalizedResult);
        return normalizedResult;
    }

    @Test
    public void testTree() throws IOException {
        when(registry.packages()).thenReturn(new HashSet<>(asList(
                new PackageId("grp", "pkg1", "1.0"),
                new PackageId("grp", "pkg2", "1.0")
        )));
        boolean merged = false;
        ec.checkThat(toJson(merged, context, null), is("{\n" +
                "  \"name\": \"/\",\n" +
                "  \"path\": \"/\",\n" +
                "  \"text\": \"Packages\",\n" +
                "  \"type\": \"root\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"jcr\",\n" +
                "      \"path\": \"/@jcr\",\n" +
                "      \"text\": \"JcrPackageRegistry\",\n" +
                "      \"type\": \"registry\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr"), is("{\n" +
                "  \"name\": \"jcr\",\n" +
                "  \"path\": \"/@jcr\",\n" +
                "  \"text\": \"JcrPackageRegistry\",\n" +
                "  \"type\": \"registry\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"grp\",\n" +
                "      \"path\": \"/@jcr/grp\",\n" +
                "      \"text\": \"grp\",\n" +
                "      \"type\": \"folder\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp"),  is("{\n" +
                "  \"name\": \"grp\",\n" +
                "  \"path\": \"/@jcr/grp\",\n" +
                "  \"text\": \"grp\",\n" +
                "  \"type\": \"folder\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"pkg1\",\n" +
                "      \"path\": \"/@jcr/grp/pkg1\",\n" +
                "      \"text\": \"pkg1\",\n" +
                "      \"type\": \"package\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"pkg2\",\n" +
                "      \"path\": \"/@jcr/grp/pkg2\",\n" +
                "      \"text\": \"pkg2\",\n" +
                "      \"type\": \"package\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg1"),  is("{\n" +
                "  \"name\": \"pkg1\",\n" +
                "  \"path\": \"/@jcr/grp/pkg1\",\n" +
                "  \"text\": \"pkg1\",\n" +
                "  \"type\": \"package\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"1.0\",\n" +
                "      \"path\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "      \"text\": \"1.0\",\n" +
                "      \"type\": \"version\",\n" +
                "      \"namespace\": \"jcr\",\n" +
                "      \"namespacedPath\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "      \"packageid\": {\n" +
                "        \"name\": \"pkg1\",\n" +
                "        \"group\": \"grp\",\n" +
                "        \"version\": \"1.0\",\n" +
                "        \"downloadName\": \"pkg1-1.0.zip\",\n" +
                "        \"registry\": \"jcr\"\n" +
                "      },\n" +
                "      \"state\": {\n" +
                "        \"loaded\": true,\n" +
                "        \"installed\": false,\n" +
                "        \"current\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg1/1.0"),  is("{\n" +
                "  \"name\": \"1.0\",\n" +
                "  \"path\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "  \"text\": \"1.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg1\",\n" +
                "    \"group\": \"grp\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"downloadName\": \"pkg1-1.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
    }

    @Test
    public void testMergedTree() throws IOException {
        when(registry.packages()).thenReturn(new HashSet<>(asList(
                new PackageId("grp", "pkg1", "1.0"),
                new PackageId("grp", "pkg2", "1.0")
        )));
        boolean merged = true;
        ec.checkThat(toJson(merged, context, null), is("{\n" +
                "  \"name\": \"/\",\n" +
                "  \"path\": \"/\",\n" +
                "  \"text\": \"Packages\",\n" +
                "  \"type\": \"root\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"grp\",\n" +
                "      \"path\": \"/grp\",\n" +
                "      \"text\": \"grp\",\n" +
                "      \"type\": \"folder\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/grp/pkg1"),  is("{\n" +
                "  \"name\": \"pkg1\",\n" +
                "  \"path\": \"/grp/pkg1\",\n" +
                "  \"text\": \"pkg1\",\n" +
                "  \"type\": \"package\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"1.0\",\n" +
                "      \"path\": \"/grp/pkg1/1.0\",\n" +
                "      \"text\": \"1.0\",\n" +
                "      \"type\": \"version\",\n" +
                "      \"namespace\": \"jcr\",\n" +
                "      \"namespacedPath\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "      \"packageid\": {\n" +
                "        \"name\": \"pkg1\",\n" +
                "        \"group\": \"grp\",\n" +
                "        \"version\": \"1.0\",\n" +
                "        \"downloadName\": \"pkg1-1.0.zip\",\n" +
                "        \"registry\": \"jcr\"\n" +
                "      },\n" +
                "      \"state\": {\n" +
                "        \"loaded\": true,\n" +
                "        \"installed\": false,\n" +
                "        \"current\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/grp/pkg1/1.0"),  is("{\n" +
                "  \"name\": \"1.0\",\n" +
                "  \"path\": \"/grp/pkg1/1.0\",\n" +
                "  \"text\": \"1.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg1/1.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg1\",\n" +
                "    \"group\": \"grp\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"downloadName\": \"pkg1-1.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
        // ec.checkThat(toJson(merged, context, "/grp/pkg2"),  is(""));
        // ec.checkThat(toJson(merged, context, "/grp/pkg2/1.0"),  is(""));
    }

    /**
     * Tests the obnoxious case that the URL of a package is also a group containing more packages.
     * The children of that group will be appended to the package.
     */
    @Test
    public void testPackageAndGroup() throws IOException {
        when(registry.packages()).thenReturn(new HashSet<>(asList(
                new PackageId("grp", "pkg", "1.0"),
                new PackageId("grp/pkg", "pkg2", "2.0")
        )));
        boolean merged = false;
        ec.checkThat(toJson(merged, context, "/@jcr/grp"),  is("{\n" +
                "  \"name\": \"grp\",\n" +
                "  \"path\": \"/@jcr/grp\",\n" +
                "  \"text\": \"grp\",\n" +
                "  \"type\": \"folder\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"pkg\",\n" +
                "      \"path\": \"/@jcr/grp/pkg\",\n" +
                "      \"text\": \"pkg\",\n" +
                "      \"type\": \"package\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg"),  is("{\n" +
                "  \"name\": \"pkg\",\n" +
                "  \"path\": \"/@jcr/grp/pkg\",\n" +
                "  \"text\": \"pkg\",\n" +
                "  \"type\": \"package\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"1.0\",\n" +
                "      \"path\": \"/@jcr/grp/pkg/1.0\",\n" +
                "      \"text\": \"1.0\",\n" +
                "      \"type\": \"version\",\n" +
                "      \"namespace\": \"jcr\",\n" +
                "      \"namespacedPath\": \"/@jcr/grp/pkg/1.0\",\n" +
                "      \"packageid\": {\n" +
                "        \"name\": \"pkg\",\n" +
                "        \"group\": \"grp\",\n" +
                "        \"version\": \"1.0\",\n" +
                "        \"downloadName\": \"pkg-1.0.zip\",\n" +
                "        \"registry\": \"jcr\"\n" +
                "      },\n" +
                "      \"state\": {\n" +
                "        \"loaded\": true,\n" +
                "        \"installed\": false,\n" +
                "        \"current\": false\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"pkg2\",\n" +
                "      \"path\": \"/@jcr/grp/pkg/pkg2\",\n" +
                "      \"text\": \"pkg2\",\n" +
                "      \"type\": \"package\",\n" +
                "      \"state\": {\n" +
                "        \"loaded\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/1.0"),  is("{\n" +
                "  \"name\": \"1.0\",\n" +
                "  \"path\": \"/@jcr/grp/pkg/1.0\",\n" +
                "  \"text\": \"1.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg/1.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg\",\n" +
                "    \"group\": \"grp\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"downloadName\": \"pkg-1.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/pkg2"),  is("{\n" +
                "  \"name\": \"pkg2\",\n" +
                "  \"path\": \"/@jcr/grp/pkg/pkg2\",\n" +
                "  \"text\": \"pkg2\",\n" +
                "  \"type\": \"package\",\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"2.0\",\n" +
                "      \"path\": \"/@jcr/grp/pkg/pkg2/2.0\",\n" +
                "      \"text\": \"2.0\",\n" +
                "      \"type\": \"version\",\n" +
                "      \"namespace\": \"jcr\",\n" +
                "      \"namespacedPath\": \"/@jcr/grp/pkg/pkg2/2.0\",\n" +
                "      \"packageid\": {\n" +
                "        \"name\": \"pkg2\",\n" +
                "        \"group\": \"grp/pkg\",\n" +
                "        \"version\": \"2.0\",\n" +
                "        \"downloadName\": \"pkg2-2.0.zip\",\n" +
                "        \"registry\": \"jcr\"\n" +
                "      },\n" +
                "      \"state\": {\n" +
                "        \"loaded\": true,\n" +
                "        \"installed\": false,\n" +
                "        \"current\": false\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/pkg2/2.0"),  is("{\n" +
                "  \"name\": \"2.0\",\n" +
                "  \"path\": \"/@jcr/grp/pkg/pkg2/2.0\",\n" +
                "  \"text\": \"2.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg/pkg2/2.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg2\",\n" +
                "    \"group\": \"grp/pkg\",\n" +
                "    \"version\": \"2.0\",\n" +
                "    \"downloadName\": \"pkg2-2.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
    }

    /**
     * Tests the obnoxious case that the URL of a package is also a group containing more packages.
     * The children of that group will be appended to the package.
     */
    @Test
    public void testPackageAndGroup2() throws IOException {
        when(registry.packages()).thenReturn(new HashSet<>(asList(
                new PackageId("grp", "pkg", "1.0"),
                new PackageId("grp/pkg/sub", "pkg2", "2.0")
        )));
        boolean merged = false;
        // ec.checkThat(toJson(merged, context, "/@jcr/grp"),  is(""));
        // ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg"),  is(""));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/1.0"),  is("{\n" +
                "  \"name\": \"1.0\",\n" +
                "  \"path\": \"/@jcr/grp/pkg/1.0\",\n" +
                "  \"text\": \"1.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg/1.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg\",\n" +
                "    \"group\": \"grp\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"downloadName\": \"pkg-1.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
        // ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/sub"),  is(""));
        // ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/sub/pkg2"),  is(""));
        ec.checkThat(toJson(merged, context, "/@jcr/grp/pkg/sub/pkg2/2.0"),  is("{\n" +
                "  \"name\": \"2.0\",\n" +
                "  \"path\": \"/@jcr/grp/pkg/sub/pkg2/2.0\",\n" +
                "  \"text\": \"2.0\",\n" +
                "  \"type\": \"version\",\n" +
                "  \"namespace\": \"jcr\",\n" +
                "  \"namespacedPath\": \"/@jcr/grp/pkg/sub/pkg2/2.0\",\n" +
                "  \"packageid\": {\n" +
                "    \"name\": \"pkg2\",\n" +
                "    \"group\": \"grp/pkg/sub\",\n" +
                "    \"version\": \"2.0\",\n" +
                "    \"downloadName\": \"pkg2-2.0.zip\",\n" +
                "    \"registry\": \"jcr\"\n" +
                "  },\n" +
                "  \"state\": {\n" +
                "    \"loaded\": true,\n" +
                "    \"installed\": false,\n" +
                "    \"current\": false\n" +
                "  },\n" +
                "  \"children\": []\n" +
                "}"));
    }


    @Ignore
    @Test
    public void testPackageAndGroup3() throws IOException {
        when(registry.packages()).thenReturn(new HashSet<>(asList(
                new PackageId("grp", "pkg", "1.0"),
                new PackageId("grp/pkg", "pkg2", "2.0")
        )));
        boolean merged = true;
        ec.checkThat(toJson(merged, context, "/grp"),  is(""));
        ec.checkThat(toJson(merged, context, "/grp/pkg"),  is(""));
        ec.checkThat(toJson(merged, context, "/grp/pkg/1.0"),  is(""));
        ec.checkThat(toJson(merged, context, "/grp/pkg/pkg2"),  is(""));
        ec.checkThat(toJson(merged, context, "/grp/pkg/pkg2/2.0"),  is(""));
    }

}
