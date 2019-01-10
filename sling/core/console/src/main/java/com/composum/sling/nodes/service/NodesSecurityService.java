package com.composum.sling.nodes.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.value.StringValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component(
        label = "Composum Nodes Security Service"
)
@Service
public class NodesSecurityService implements SecurityService {

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final String jsonFilePath)
            throws RepositoryException, IOException {
        Node jsonFileNode = session.getNode(jsonFilePath);
        if (jsonFileNode != null) {
            Property property = jsonFileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA);
            try (InputStream stream = property.getBinary().getStream();
                 Reader streamReader = new InputStreamReader(stream, UTF_8)) {
                addJsonAcl(session, streamReader);
            }
        } else {
            throw new IOException("configuration file node not found (" + jsonFilePath + ")");
        }
    }

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final Reader reader)
            throws RepositoryException, IOException {
        try (JsonReader jsonReader = new JsonReader(reader)) {
            if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
                jsonReader.beginArray();
                while (jsonReader.peek() != JsonToken.END_ARRAY) {
                    addAclObject(session, jsonReader);
                }
                jsonReader.endArray();
            } else {
                addAclObject(session, jsonReader);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addAclObject(@Nonnull final Session session, @Nonnull final JsonReader reader)
            throws RepositoryException {
        final Gson gson = new Gson();
        final Map map = gson.fromJson(reader, Map.class);
        final String path = (String) map.get("path");
        if (StringUtils.isNotBlank(path)) {
            final List<Map> acl = (List<Map>) map.get("acl");
            if (acl != null) {
                addAclList(session, path, acl);
            } else {
                removeAcl(session, path, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addAclList(@Nonnull final Session session, @Nonnull final String path,
                              @Nonnull final List<Map> list)
            throws RepositoryException {
        for (Map map : list) {
            String principal = (String) map.get("principal");
            if (StringUtils.isNotBlank(principal)) {
                final List<Map> acl = (List<Map>) map.get("acl");
                if (acl != null) {
                    for (Map rule : acl) {
                        boolean allow = (Boolean) rule.get("allow");
                        Object object = rule.get("privileges");
                        String[] privileges;
                        if (object instanceof List) {
                            List<String> privList = (List<String>) object;
                            privileges = privList.toArray(new String[0]);
                        } else {
                            privileges = new String[]{(String) object};
                        }
                        Map<String, String> restrictions = (Map<String, String>) rule.get("restrictions");
                        addAcl(session, path, principal, allow, privileges,
                                restrictions != null ? restrictions : Collections.EMPTY_MAP);
                    }
                } else {
                    removeAcl(session, path, principal);
                }
            }
        }
    }

    @Override
    public void addAcl(@Nonnull final Session session, @Nonnull final String path,
                       @Nonnull final String principalName, boolean allow,
                       @Nonnull final String[] privilegeKeys,
                       @Nonnull final Map restrictionKeys)
            throws RepositoryException {
        final AccessControlManager acManager = session.getAccessControlManager();
        final PrincipalManager principalManager = ((JackrabbitSession) session).getPrincipalManager();
        final JackrabbitAccessControlList policies = AccessControlUtils.getAccessControlList(acManager, path);
        final Principal principal = principalManager.getPrincipal(principalName);
        final Privilege[] privileges = AccessControlUtils.privilegesFromNames(acManager, privilegeKeys);
        final Map<String, Value> restrictions = new HashMap<>();
        for (final Object key : restrictionKeys.keySet()) {
            restrictions.put((String) key, new StringValue((String) restrictionKeys.get(key)));
        }
        policies.addEntry(principal, privileges, allow, restrictions);
        acManager.setPolicy(path, policies);
    }

    @Override
    public void removeAcl(@Nonnull final Session session, @Nonnull final String path, @Nullable final String principal)
            throws RepositoryException {
        final AccessControlManager acManager = session.getAccessControlManager();
        final JackrabbitAccessControlList policy = AccessControlUtils.getAccessControlList(acManager, path);
        for (final AccessControlEntry entry : policy.getAccessControlEntries()) {
            final JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
            if (principal == null || principal.equals(jrEntry.getPrincipal().getName())) {
                policy.removeAccessControlEntry(entry);
            }
        }
        acManager.setPolicy(path, policy);
        if (policy.isEmpty()) {
            acManager.removePolicy(path, policy);
        }
    }
}
