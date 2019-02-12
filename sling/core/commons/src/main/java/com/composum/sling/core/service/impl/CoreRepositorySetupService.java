package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.RepositorySetupService;
import com.composum.sling.core.util.ValueEmbeddingReader;
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
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component(
        label = "Composum Nodes Security Service"
)
@Service
public class CoreRepositorySetupService implements RepositorySetupService {

    private static final Logger LOG = LoggerFactory.getLogger(CoreRepositorySetupService.class);

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final String jsonFilePath,
                           @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        Node jsonFileNode = session.getNode(jsonFilePath);
        if (jsonFileNode != null) {
            Property property = jsonFileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA);
            try (InputStream stream = property.getBinary().getStream();
                 Reader streamReader = new InputStreamReader(stream, UTF_8)) {
                addJsonAcl(session, streamReader, values);
            }
        } else {
            throw new IOException("configuration file node not found (" + jsonFilePath + ")");
        }
    }

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final Reader reader,
                           @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        try (JsonReader jsonReader = new JsonReader(
                values != null ? new ValueEmbeddingReader(reader, values) : reader)) {
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

    @Override
    public void removeJsonAcl(@Nonnull final Session session, @Nonnull final String jsonFilePath,
                              @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        Node jsonFileNode = session.getNode(jsonFilePath);
        if (jsonFileNode != null) {
            Property property = jsonFileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA);
            try (InputStream stream = property.getBinary().getStream();
                 Reader streamReader = new InputStreamReader(stream, UTF_8)) {
                removeJsonAcl(session, streamReader, values);
            }
        } else {
            throw new IOException("configuration file node not found (" + jsonFilePath + ")");
        }
    }

    @Override
    public void removeJsonAcl(@Nonnull final Session session, @Nonnull final Reader reader,
                              @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        try (JsonReader jsonReader = new JsonReader(
                values != null ? new ValueEmbeddingReader(reader, values) : reader)) {
            if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
                jsonReader.beginArray();
                while (jsonReader.peek() != JsonToken.END_ARRAY) {
                    removeAclObject(session, jsonReader);
                }
                jsonReader.endArray();
            } else {
                removeAclObject(session, jsonReader);
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
            String primaryType = (String) map.get(JcrConstants.JCR_PRIMARYTYPE);
            if (StringUtils.isNotBlank(primaryType)) {
                makeNodeAvailable(session, path, primaryType);
            }
            final List<Map> acl = (List<Map>) map.get("acl");
            if (acl != null) {
                addAclList(session, path, acl);
            } else {
                removeAcl(session, path, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void removeAclObject(@Nonnull final Session session, @Nonnull final JsonReader reader)
            throws RepositoryException {
        final Gson gson = new Gson();
        final Map map = gson.fromJson(reader, Map.class);
        final String path = (String) map.get("path");
        if (StringUtils.isNotBlank(path)) {
            final List<Map> acl = (List<Map>) map.get("acl");
            if (acl != null) {
                removeAclList(session, path, acl);
            } else {
                removeAcl(session, path, null);
            }
            String primaryType = (String) map.get(JcrConstants.JCR_PRIMARYTYPE);
            if (StringUtils.isNotBlank(primaryType)) {
                removeNode(session, path);
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
                String groupPath = (String) map.get(GROUP_PATH);
                if (StringUtils.isNotBlank(groupPath)) {
                    makeGroupAvailable(session, principal, groupPath);
                }
                List<String> memberOf = (List<String>) map.get(MEMBER_OF);
                if (memberOf != null) {
                    makeMemberAvailable(session, principal, memberOf);
                }
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

    @SuppressWarnings("unchecked")
    protected void removeAclList(@Nonnull final Session session, @Nonnull final String path,
                                 @Nonnull final List<Map> list)
            throws RepositoryException {
        for (Map map : list) {
            String principal = (String) map.get("principal");
            if (StringUtils.isNotBlank(principal)) {
                final List<Map> acl = (List<Map>) map.get("acl");
                removeAcl(session, path, principal);
                List<String> memberOf = (List<String>) map.get(MEMBER_OF);
                if (memberOf != null) {
                    removeMember(session, principal, memberOf);
                }
                String groupPath = (String) map.get(GROUP_PATH);
                if (StringUtils.isNotBlank(groupPath)) {
                    removeGroup(session, principal);
                }
            }
        }
    }

    // ACL

    protected void addAcl(@Nonnull final Session session, @Nonnull final String path,
                          @Nonnull final String principalName, boolean allow,
                          @Nonnull final String[] privilegeKeys,
                          @Nonnull final Map restrictionKeys)
            throws RepositoryException {
        try {
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
            LOG.info("addAcl({},{})", principalName, Arrays.toString(privilegeKeys));
            acManager.setPolicy(path, policies);
        } catch (RepositoryException e) {
            LOG.error("Error in addAcl({},{},{},{}, {}) : {}", new Object[]{path, principalName, allow, Arrays.asList(privilegeKeys), restrictionKeys, e.toString()});
            throw e;
        }
    }

    protected void removeAcl(@Nonnull final Session session, @Nonnull final String path, @Nullable final String principal)
            throws RepositoryException {
        try {
            final AccessControlManager acManager = session.getAccessControlManager();
            JackrabbitAccessControlList policy = null;
            try {
                policy = AccessControlUtils.getAccessControlList(acManager, path);
            } catch (RepositoryException ignore) {
            }
            if (policy != null) {
                for (final AccessControlEntry entry : policy.getAccessControlEntries()) {
                    final JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                    if (principal == null || principal.equals(jrEntry.getPrincipal().getName())) {
                        LOG.info("removeAcl({},{})", entry.getPrincipal().getName(), Arrays.toString(entry.getPrivileges()));
                        policy.removeAccessControlEntry(entry);
                    }
                }
                acManager.setPolicy(path, policy);
                if (policy.isEmpty()) {
                    acManager.removePolicy(path, policy);
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error in removeAcl({},{}) : {}", new Object[]{path, principal, e.toString()});
            throw e;
        }
    }

    // nodes

    protected Node makeNodeAvailable(@Nonnull final Session session,
                                     @Nonnull final String path, @Nonnull final String primaryType)
            throws RepositoryException {
        Node node;
        try {
            node = session.getNode(StringUtils.isNotBlank(path) ? path : "/");
        } catch (PathNotFoundException nf) {
            LOG.info("createNode({},{})", path, primaryType);
            Node parent = makeNodeAvailable(session, StringUtils.substringBeforeLast(path, "/"), primaryType);
            node = parent.addNode(StringUtils.substringAfterLast(path, "/"), primaryType);
        } catch (RepositoryException e) {
            LOG.error("Error in makeNodeAvailable({},{}) : {}", new Object[]{path, primaryType, e.toString()});
            throw e;
        }
        return node;
    }

    protected void removeNode(@Nonnull final Session session, @Nonnull final String path)
            throws RepositoryException {
        Node node;
        try {
            node = session.getNode(path);
            LOG.info("removeNode({})", path);
            node.remove();
        } catch (PathNotFoundException ignore) {
        } catch (RepositoryException e) {
            LOG.error("Error in removeNode({},{}) : {}", path, e.toString());
            throw e;
        }
    }

    // groups

    protected Authorizable makeGroupAvailable(@Nonnull final Session session,
                                              @Nonnull final String id, @Nonnull final String intermediatePath)
            throws RepositoryException {
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Authorizable authorizable = userManager.getAuthorizable(id);
        if (authorizable != null) {
            if (authorizable.isGroup()) {
                return authorizable;
            }
            throw new RepositoryException("'" + id + "' exists but is not a group");
        }
        LOG.info("addGroup({},{})", id, intermediatePath);
        try {
            authorizable = userManager.createGroup(new Principal() {
                @Override
                public String getName() {
                    return id;
                }
            }, intermediatePath);
        session.save();
        } catch (RepositoryException e) {
            LOG.error("Error in makeGroupAvailable({},{}) : {}", new Object[]{id, intermediatePath, e.toString()});
            throw e;
        }
        return authorizable;
    }

    protected void removeGroup(@Nonnull final Session session, @Nonnull final String id)
            throws RepositoryException {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable authorizable = userManager.getAuthorizable(id);
            if (authorizable != null) {
                if (authorizable.isGroup()) {
                    LOG.info("removeGroup({})", id);
                    authorizable.remove();
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error in removeGroup({},{}) : {}", id, e.toString());
            throw e;
        }
    }

    protected void makeMemberAvailable(@Nonnull final Session session,
                                       @Nonnull final String memberId, @Nonnull final List<String> groupIds)
            throws RepositoryException {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable member = userManager.getAuthorizable(memberId);
            if (member != null) {
                for (String groupId : groupIds) {
                    Authorizable authorizable = userManager.getAuthorizable(groupId);
                    if (authorizable != null && authorizable.isGroup()) {
                        Group group = (Group) authorizable;
                        if (!group.isMember(member)) {
                            LOG.info("addMember({},{})", memberId, groupId);
                            group.addMember(member);
                            session.save();
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error in makeNodeAvailable({},{}) : {}", new Object[]{memberId, groupIds, e.toString()});
            throw e;
        }
    }

    protected void removeMember(@Nonnull final Session session,
                                @Nonnull final String memberId, @Nonnull final List<String> groupIds)
            throws RepositoryException {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable member = userManager.getAuthorizable(memberId);
            if (member != null) {
                for (String groupId : groupIds) {
                    Authorizable authorizable = userManager.getAuthorizable(groupId);
                    if (authorizable != null && authorizable.isGroup()) {
                        Group group = (Group) authorizable;
                        if (group.isMember(member)) {
                            LOG.info("removeMember({},{})", memberId, groupId);
                            group.removeMember(member);
                            session.save();
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error in makeNodeAvailable({},{}) : {}", new Object[]{memberId, groupIds, e.toString()});
            throw e;
        }
    }
}
