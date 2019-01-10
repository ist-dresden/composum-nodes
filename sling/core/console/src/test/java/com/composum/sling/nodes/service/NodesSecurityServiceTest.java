package com.composum.sling.nodes.service;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NodesSecurityServiceTest {

    public static final String JAVA_RESOURCE_BASE = "/com/composum/nodes/security/";

    public static class TestService extends NodesSecurityService {

        @Override
        public void addAcl(@Nonnull final Session session, @Nonnull final String path,
                           @Nonnull final String principal, boolean allow,
                           @Nonnull final String[] privileges,
                           @Nonnull final Map restrictions) {
            System.out.println("addAcl(" + path + "," + principal + ","
                    + allow + "," + Arrays.toString(privileges) + "," + restrictions + ")");
        }
    }

    @Test
    public void testAclFromJson() throws Exception {
        TestService service = new TestService();
        try (
                InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + "acl.json");
                Reader streamReader = new InputStreamReader(stream, UTF_8)
        ) {
            //noinspection ConstantConditions
            service.addJsonAcl(null, streamReader);
        }
    }
}
