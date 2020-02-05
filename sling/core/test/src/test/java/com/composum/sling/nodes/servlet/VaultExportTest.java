package com.composum.sling.nodes.servlet;

import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.test.util.JcrTestUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/** Test how to use vault as an easier to maintain version of the SourceModel. */
public class VaultExportTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected Resource something;
    protected ResourceResolver resolver;

    @Before
    public void setup() throws IOException, ParseException, RepositoryException {
        resolver = context.resourceResolver();
        Session session = resolver.adaptTo(Session.class);
        InputStreamReader cndReader =
                new InputStreamReader(getClass().getResourceAsStream("/nodes/testingNodetypes.cnd"));
        NodeType[] nodeTypes = CndImporter.registerNodeTypes(cndReader, session);
        resolver.commit();
        ec.checkThat(nodeTypes.length, Matchers.greaterThan(0));

        something = context.load(false).json("/nodes/vaulttest.json", "/something");
        ModifiableValueMap vm = something.getChild("propertytest").adaptTo(ModifiableValueMap.class);
        vm.put("binary", getClass().getResourceAsStream("/nodes/something.bin"));
        vm.put("binary with-weird na_me", getClass().getResourceAsStream("/nodes/something.bin"));
        something.getChild("testbild.png/jcr:content").adaptTo(ModifiableValueMap.class).put(ResourceUtil.PROP_DATA,
                getClass().getResourceAsStream("/nodes/something.bin"));
        JcrTestUtils.printResourceRecursivelyAsJson(something);
        context.resourceResolver().commit();
    }

    @Test
    public void checkSetup() {
        // empty
    }

}
