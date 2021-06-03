package com.composum.sling.core.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class CoreRepositorySetupServiceTest {

    public static final String JAVA_RESOURCE_BASE = "/com/composum/sling/core/setup/";

    public static class TestService extends CoreRepositorySetupService {

        public String groupPath;

        @Override
        public void addAcRule(@Nonnull final Session session, @Nonnull final String path,
                              @Nonnull final String principal, boolean allow,
                              @Nonnull final String[] privileges,
                              @Nonnull final Map<String, Object> restrictions) {
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

        @Override
        protected void makeMemberAvailable(@Nonnull final Session session, @Nonnull final String memberId,
                                           @Nonnull final List<String> groupIds, @Nullable final String groupPath) {
            for (String groupId : groupIds) {
                if (StringUtils.isNotBlank(groupPath)) {
                    makeGroupAvailable(session, groupId, groupPath);
                }
            }
        }
    }

    @Test
    public void testAclFromJsonWithoutValues() throws Exception {
        TestService service = new TestService();
        try (
                InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + "acl.json");
                Reader streamReader = stream != null ? new InputStreamReader(stream, UTF_8) : null
        ) {
            if (streamReader != null) {
                service.addJsonAcl(Mockito.mock(Session.class), streamReader, null);
            }
        }
    }

    @Test
    public void testAclFromJsonWithValues() throws Exception {
        TestService service = new TestService();
        try (
                InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + "acl.json");
                Reader streamReader = stream != null ? new InputStreamReader(stream, UTF_8) : null
        ) {
            if (streamReader != null) {
                service.addJsonAcl(Mockito.mock(Session.class), streamReader, new HashMap<String, Object>() {{
                    put("base", "my/groups");
                }});
            }
            assertEquals("my/groups", service.groupPath);
        }
    }
}
