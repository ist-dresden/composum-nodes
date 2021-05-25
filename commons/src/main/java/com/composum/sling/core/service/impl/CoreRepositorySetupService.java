package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.RepositorySetupService;
import com.composum.sling.core.util.ValueEmbeddingReader;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
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
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Security Service"
        }
)
public class CoreRepositorySetupService implements RepositorySetupService {

    private static final Logger LOG = LoggerFactory.getLogger(CoreRepositorySetupService.class);

    public interface Tracker {

        void info(String message);

        void warn(String message);

        void error(String message);
    }

    public static final ThreadLocal<Tracker> TRACKER = new ThreadLocal<>();

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final String jsonFilePath,
                           @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        if (StringUtils.isNotBlank(jsonFilePath)) {
            LOG.info("add JSON  ({})...", jsonFilePath);
            final Node jsonFileNode = session.getNode(jsonFilePath);
            if (jsonFileNode != null) {
                final Property property = jsonFileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA);
                try (final InputStream stream = property.getBinary().getStream();
                     final Reader streamReader = new InputStreamReader(stream, UTF_8)) {
                    addJsonAcl(session, streamReader, values);
                }
            } else {
                throw new IOException("configuration file node not found (" + jsonFilePath + ")");
            }
        }
    }

    @Override
    public void addJsonAcl(@Nonnull final Session session, @Nonnull final Reader reader,
                           @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        try (final JsonReader jsonReader = new JsonReader(
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
        if (StringUtils.isNotBlank(jsonFilePath)) {
            LOG.info("del JSON  ({})...", jsonFilePath);
            final Node jsonFileNode = session.getNode(jsonFilePath);
            if (jsonFileNode != null) {
                final Property property = jsonFileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA);
                try (final InputStream stream = property.getBinary().getStream();
                     final Reader streamReader = new InputStreamReader(stream, UTF_8)) {
                    removeJsonAcl(session, streamReader, values);
                }
            } else {
                throw new IOException("configuration file node not found (" + jsonFilePath + ")");
            }
        }
    }

    @Override
    public void removeJsonAcl(@Nonnull final Session session, @Nonnull final Reader reader,
                              @Nullable final Map<String, Object> values)
            throws RepositoryException, IOException {
        try (final JsonReader jsonReader = new JsonReader(
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
        final Map<String, Object> map = gson.fromJson(reader, Map.class);
        final Object location = map.get("path");
        if (location != null) {
            final String primaryType = (String) map.get(JcrConstants.JCR_PRIMARYTYPE);
            final Object acl = map.get("acl");
            final Boolean reset = (Boolean) map.get("reset");
            final List<String> paths = location instanceof List ? (List<String>) location
                    : Collections.singletonList(location.toString());
            for (final String path : paths) {
                if (StringUtils.isNotBlank(path)) {
                    LOG.info("add OBJ   ({})...", path);
                    if (StringUtils.isNotBlank(primaryType)) {
                        makeNodeAvailable(session, path, primaryType);
                    }
                    if (acl != null) {
                        if (reset != null && reset) {
                            info("reset OBJ ({})...", path);
                            removeAcRule(session, path, null);
                        }
                        addAcList(session, path, acl instanceof List
                                ? (List<Map<String, Object>>) acl
                                : Collections.singletonList((Map<String, Object>) acl));
                    } else {
                        // for compatibility to the first version of acl scripts
                        info("reset NOP ({})...", path);
                        removeAcRule(session, path, null);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void removeAclObject(@Nonnull final Session session, @Nonnull final JsonReader reader)
            throws RepositoryException {
        final Gson gson = new Gson();
        final Map<String, Object> map = gson.fromJson(reader, Map.class);
        final Object location = map.get("path");
        if (location != null) {
            final String primaryType = (String) map.get(JcrConstants.JCR_PRIMARYTYPE);
            final List<Map<String, Object>> acl = (List<Map<String, Object>>) map.get("acl");
            List<String> paths = location instanceof List ? (List<String>) location
                    : Collections.singletonList(location.toString());
            for (final String path : paths) {
                if (StringUtils.isNotBlank(path)) {
                    LOG.info("del OBJ   ({})...", path);
                    if (acl != null) {
                        removeAcList(session, path, acl);
                    } else {
                        removeAcRule(session, path, null);
                    }
                    if (StringUtils.isNotBlank(primaryType)) {
                        removeNode(session, path);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addAcList(@Nonnull final Session session, @Nonnull final String path,
                             @Nonnull final List<Map<String, Object>> list)
            throws RepositoryException {
        info("adjust ACL '{}'...", path);
        for (final Map<String, Object> map : list) {
            final Object principalRule = map.get("principal");
            if (principalRule != null) {
                final String userPath = (String) map.get(USER_PATH);
                final String groupPath = (String) map.get(GROUP_PATH);
                final List<String> memberOf = (List<String>) map.get(MEMBER_OF);
                Boolean reset = (Boolean) map.get("reset");
                Object ruleSet = map.get("rule");
                if (ruleSet == null) {
                    ruleSet = map.get("rules");
                    if (ruleSet == null) {
                        // for compatibility to the first version of rule sets
                        ruleSet = map.get("acl");
                        if (ruleSet == null) {
                            reset = true;
                        }
                    }
                }
                for (final String principal : principalRule instanceof List ? (List<String>) principalRule
                        : Collections.singletonList(principalRule.toString())) {
                    if (StringUtils.isNotBlank(principal)) {
                        if (StringUtils.isNotBlank(userPath)) {
                            makeUserAvailable(session, principal, userPath);
                        } else if (StringUtils.isNotBlank(groupPath)) {
                            makeGroupAvailable(session, principal, groupPath);
                        }
                        if (memberOf != null) {
                            makeMemberAvailable(session, principal, memberOf, groupPath);
                        }
                        if (reset != null && reset) {
                            info("reset ACL({},{})...", path, principal);
                            removeAcRule(session, path, principal);
                        }
                        final List<Map<String, Object>> rules = ruleSet instanceof List ? (List<Map<String, Object>>) ruleSet
                                : Collections.singletonList(ruleSet != null ? (Map<String, Object>) ruleSet : map);
                        for (final Map<String, Object> rule : rules) {
                            boolean grant = true;
                            Object object = rule.get("grant");
                            if (object == null) {
                                object = rule.get("deny");
                                if (object != null) {
                                    grant = false;
                                } else {
                                    // for compatibility to the first version of rules
                                    object = rule.get("privileges");
                                    Object allow = rule.get("allow");
                                    grant = (allow == null || (allow instanceof Boolean && (Boolean) allow));
                                }
                            }
                            String[] privileges = null;
                            if (object instanceof List) {
                                final List<String> privList = (List<String>) object;
                                privileges = privList.toArray(new String[0]);
                            } else if (object instanceof String) {
                                privileges = new String[]{(String) object};
                            }
                            if (privileges != null) {
                                object = rule.get("restrictions");
                                if (object != null) {
                                    if (object instanceof List) {
                                        for (final Map<String, Object> restrictions : (List<Map<String, Object>>) object) {
                                            addAcRule(session, path, principal, grant, privileges, restrictions);
                                        }
                                    } else {
                                        addAcRule(session, path, principal, grant, privileges, (Map<String, Object>) object);
                                    }
                                } else {
                                    addAcRule(session, path, principal, grant, privileges, Collections.EMPTY_MAP);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void removeAcList(@Nonnull final Session session, @Nonnull final String path,
                                @Nonnull final List<Map<String, Object>> list)
            throws RepositoryException {
        info("del ACL   ({})...", path);
        for (final Map<String, Object> map : list) {
            final String principal = (String) map.get("principal");
            if (StringUtils.isNotBlank(principal)) {
                final List<Map<String, Object>> acl = (List<Map<String, Object>>) map.get("acl");
                removeAcRule(session, path, principal);
                final List<String> memberOf = (List<String>) map.get(MEMBER_OF);
                if (memberOf != null) {
                    removeMember(session, principal, memberOf);
                }
                final String groupPath = (String) map.get(GROUP_PATH);
                if (StringUtils.isNotBlank(groupPath)) {
                    removeGroup(session, principal);
                }
            }
        }
    }

    // ACL

    protected void addAcRule(@Nonnull final Session session, @Nonnull final String path,
                             @Nonnull final String principalName, boolean allow,
                             @Nonnull final String[] privilegeKeys,
                             @Nonnull final Map<String, Object> restrictionKeys)
            throws RepositoryException {
        try {
            final AccessControlManager acManager = session.getAccessControlManager();
            final PrincipalManager principalManager = ((JackrabbitSession) session).getPrincipalManager();
            final JackrabbitAccessControlList policies = AccessControlUtils.getAccessControlList(acManager, path);
            final Principal principal = principalManager.getPrincipal(principalName);
            final Privilege[] privileges = AccessControlUtils.privilegesFromNames(acManager, privilegeKeys);
            final Map<String, Value> restrictions = new HashMap<>();
            final ValueFactory valueFactory = session.getValueFactory();
            for (final String key : restrictionKeys.keySet()) {
                restrictions.put(key, valueFactory.createValue((String) restrictionKeys.get(key), policies.getRestrictionType(key)));
            }
            policies.addEntry(principal, privileges, allow, restrictions);
            info("add Rule  ({},{},{},{})",
                    principalName, allow ? "grant" : "deny", Arrays.toString(privilegeKeys), restrictionKeys);
            acManager.setPolicy(path, policies);
        } catch (Exception ex) {
            error("Error in addAcRule({},{},{},{},{}) : {}",
                    path, principalName, allow, Arrays.asList(privilegeKeys), restrictionKeys, ex.toString());
            throw ex;
        }
    }

    protected void removeAcRule(@Nonnull final Session session, @Nonnull final String path,
                                @Nullable final String principal)
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
                        info("del Rule  ({},{},{})", entry.getPrincipal().getName(), jrEntry.isAllow() ? "grant" : "deny",
                                Arrays.toString(entry.getPrivileges()));
                        policy.removeAccessControlEntry(entry);
                    }
                }
                acManager.setPolicy(path, policy);
                if (policy.isEmpty()) {
                    acManager.removePolicy(path, policy);
                }
            }
        } catch (RepositoryException e) {
            error("Error in removeAcRule({},{}) : {}", path, principal, e.toString());
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
            info("new Node  ({},{})", path, primaryType);
            final Node parent = makeNodeAvailable(session, StringUtils.substringBeforeLast(path, "/"), primaryType);
            node = parent.addNode(StringUtils.substringAfterLast(path, "/"), primaryType);
        } catch (RepositoryException e) {
            error("Error in makeNodeAvailable({},{}) : {}", path, primaryType, e.toString());
            throw e;
        }
        return node;
    }

    protected void removeNode(@Nonnull final Session session, @Nonnull final String path)
            throws RepositoryException {
        Node node;
        try {
            node = session.getNode(path);
            info("del Node  ({})", path);
            node.remove();
        } catch (PathNotFoundException ignore) {
        } catch (RepositoryException e) {
            error("Error in removeNode({}) : {}", path, e.toString());
            throw e;
        }
    }

    // users

    protected Authorizable makeUserAvailable(@Nonnull final Session session,
                                             @Nonnull final String id, @Nonnull final String intermediatePath)
            throws RepositoryException {
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Authorizable authorizable = userManager.getAuthorizable(id);
        if (authorizable != null) {
            if (!authorizable.isGroup()) {
                return authorizable;
            }
            throw new RepositoryException("'" + id + "' exists but is not a user");
        }
        info("add User  ({},{})", id, intermediatePath);
        try {
            authorizable = intermediatePath.startsWith("system/")
                    ? userManager.createSystemUser(id, intermediatePath)
                    : userManager.createUser(id, intermediatePath);
            session.save();
        } catch (RepositoryException e) {
            error("Error in makeUserAvailable({},{}) : {}", id, intermediatePath, e.toString());
            throw e;
        }
        return authorizable;
    }

    // groups

    protected Authorizable makeGroupAvailable(@Nonnull final Session session,
                                              @Nonnull final String id, @Nonnull final String intermediatePath)
            throws RepositoryException {
        final UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Authorizable authorizable = userManager.getAuthorizable(id);
        if (authorizable != null) {
            if (authorizable.isGroup()) {
                return authorizable;
            }
            throw new RepositoryException("'" + id + "' exists but is not a group");
        }
        info("add Group ({},{})", id, intermediatePath);
        try {
            authorizable = userManager.createGroup(() -> id, intermediatePath);
            session.save();
        } catch (RepositoryException e) {
            error("Error in makeGroupAvailable({},{}) : {}", id, intermediatePath, e.toString());
            throw e;
        }
        return authorizable;
    }

    protected void removeGroup(@Nonnull final Session session, @Nonnull final String id)
            throws RepositoryException {
        try {
            final UserManager userManager = ((JackrabbitSession) session).getUserManager();
            final Authorizable authorizable = userManager.getAuthorizable(id);
            if (authorizable != null) {
                if (authorizable.isGroup()) {
                    info("del Group ({})", id);
                    authorizable.remove();
                }
            }
        } catch (RepositoryException e) {
            error("Error in removeGroup({}): {}", id, e.toString());
            throw e;
        }
    }

    protected void makeMemberAvailable(@Nonnull final Session session, @Nonnull final String memberId,
                                       @Nonnull final List<String> groupIds, @Nullable final String groupPath)
            throws RepositoryException {
        try {
            final UserManager userManager = ((JackrabbitSession) session).getUserManager();
            final Authorizable member = userManager.getAuthorizable(memberId);
            if (member != null) {
                for (String groupId : groupIds) {
                    Authorizable authorizable = userManager.getAuthorizable(groupId);
                    if (authorizable == null && StringUtils.isNotBlank(groupPath)) {
                        authorizable = makeGroupAvailable(session, groupId, groupPath);
                    }
                    if (authorizable != null && authorizable.isGroup()) {
                        final Group group = (Group) authorizable;
                        if (!group.isMember(member)) {
                            info("add Member({},{})", memberId, groupId);
                            group.addMember(member);
                            session.save();
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            error("Error in makeMemberAvailable({},{}) : {}", memberId, groupIds, e.toString());
            throw e;
        }
    }

    protected void removeMember(@Nonnull final Session session,
                                @Nonnull final String memberId, @Nonnull final List<String> groupIds)
            throws RepositoryException {
        try {
            final UserManager userManager = ((JackrabbitSession) session).getUserManager();
            final Authorizable member = userManager.getAuthorizable(memberId);
            if (member != null) {
                for (String groupId : groupIds) {
                    final Authorizable authorizable = userManager.getAuthorizable(groupId);
                    if (authorizable != null && authorizable.isGroup()) {
                        final Group group = (Group) authorizable;
                        if (group.isMember(member)) {
                            info("del Member({},{})", memberId, groupId);
                            group.removeMember(member);
                            session.save();
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            error("Error in removeMember({},{}) : {}", memberId, groupIds, e.toString());
            throw e;
        }
    }

    protected void info(String pattern, Object... args) {
        LOG.info(pattern, args);
        final Tracker tracker = TRACKER.get();
        if (tracker != null) {
            tracker.info(MessageFormatter.arrayFormat(pattern, args).getMessage());
        }
    }

    protected void warn(String pattern, Object... args) {
        LOG.warn(pattern, args);
        final Tracker tracker = TRACKER.get();
        if (tracker != null) {
            tracker.warn(MessageFormatter.arrayFormat(pattern, args).getMessage());
        }
    }

    protected void error(String pattern, Object... args) {
        LOG.error(pattern, args);
        final Tracker tracker = TRACKER.get();
        if (tracker != null) {
            tracker.error(MessageFormatter.arrayFormat(pattern, args).getMessage());
        }
    }
}
