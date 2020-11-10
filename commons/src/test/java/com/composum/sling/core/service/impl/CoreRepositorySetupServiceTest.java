package com.composum.sling.core.service.impl;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class CoreRepositorySetupServiceTest {

    public static final String JAVA_RESOURCE_BASE = "/com/composum/sling/core/setup/";

    public static class TestService extends CoreRepositorySetupService {

        public String groupPath;

        @Override
        public void addAcl(@Nonnull final Session session, @Nonnull final String path,
                           @Nonnull final String principal, boolean allow,
                           @Nonnull final String[] privileges,
                           @Nonnull final Map restrictions) {
            System.out.println("addAcl(" + path + "," + principal + ","
                    + allow + "," + Arrays.toString(privileges) + "," + restrictions + ")");
        }

        @Override
        protected Node makeNodeAvailable(@Nonnull final Session session,
                                         @Nonnull final String path, @Nonnull final String primaryType) {
            return null;
        }

        @Override
        protected Authorizable makeGroupAvailable(@Nonnull final Session session,
                                                  @Nonnull final String id, @Nonnull final String intermediatePath) {
            groupPath = intermediatePath;
            return null;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testAclFromJsonWithoutValues() throws Exception {
        TestService service = new TestService();
        try (
                InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + "acl.json");
                Reader streamReader = new InputStreamReader(stream, UTF_8)
        ) {
            service.addJsonAcl(null, streamReader, null);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testAclFromJsonWithValues() throws Exception {
        TestService service = new TestService();
        try (
                InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + "acl.json");
                Reader streamReader = new InputStreamReader(stream, UTF_8)
        ) {
            service.addJsonAcl(null, streamReader, new HashMap<String, Object>() {{
                put("base", "my/groups");
            }});
            assertEquals("my/groups", service.groupPath);
        }
    }
}