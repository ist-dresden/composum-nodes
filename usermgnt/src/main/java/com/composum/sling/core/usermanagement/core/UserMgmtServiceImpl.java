package com.composum.sling.core.usermanagement.core;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;

@Component
@Service
public class UserMgmtServiceImpl implements UserManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(UserMgmtServiceImpl.class);

    /**
     * Retrieves a user by its name; if the user doesn't exist the user will be created.
     *
     * @param path the name of the user with an optional prepended intermediate path for user creation
     */
    public Authorizable getOrCreateUser(JackrabbitSession session, UserManager userManager,
                                        String path, boolean systemUser)
            throws RepositoryException {
        String[] pathAndName = pathAndName(path);
        Authorizable user = userManager.getAuthorizable(pathAndName[1]);
        LOG.debug("user.check: " + pathAndName[1] + " - " + user);
        if (user == null) {
            LOG.info("user.create: " + pathAndName[1]);
            Principal principal = new NamePrincipal(pathAndName[1]);
            if (systemUser) {
                try {
                    // using reflection to create systems users for compatibility to older API versions
                    Method createSystemUser = userManager.getClass().getMethod("createSystemUser", String.class, String.class);
                    user = (User) createSystemUser.invoke(userManager, pathAndName[1], pathAndName[0]);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    LOG.error(ex.toString());
                }
            } else {
                user = userManager.createUser(pathAndName[1], pathAndName[1],
                        principal, pathAndName[0]);
            }
        }
        return user;
    }

    /**
     * Retrieves a group by their name; if the group doesn't exist the group will be created.
     *
     * @param path the name of the group with an optional prepended intermediate path for group creation
     */
    public Group getOrCreateGroup(JackrabbitSession session, UserManager userManager,
                                  String path)
            throws RepositoryException {
        String[] pathAndName = pathAndName(path);
        Group group = (Group) userManager.getAuthorizable(pathAndName[1]);
        LOG.debug("group.check: " + pathAndName[1] + " - " + group);
        if (group == null) {
            LOG.info("group.create: " + pathAndName[1]);
            group = userManager.createGroup(new NamePrincipal(pathAndName[1]), pathAndName[0]);
        }
        return group;
    }

    /**
     * Assign an member (user or group) to a group if not always a member.
     *
     * @param authorizable the name of the authorizable (the name only)
     * @param groupName    the name of the group (the simple name only)
     */
    public void assignToGroup(JackrabbitSession session, UserManager userManager,
                              Authorizable authorizable, String groupName)
            throws RepositoryException {
        if (authorizable != null) {
            Group group = (Group) userManager.getAuthorizable(groupName);
            if (group != null) {
                if (!group.isMember(authorizable)) {
                    LOG.info("assign.member: " + authorizable.getID() + " to group: " + groupName);
                    group.addMember(authorizable);
                }
            } else {
                LOG.error("group not found: " + groupName);
            }
        }
    }

    /**
     * Assign an member (user or group) to a group if not always a member.
     *
     * @param memberName the name of the authorizable (the name only)
     */
    public void assignToGroup(JackrabbitSession session, UserManager userManager,
                              String memberName, Group group)
            throws RepositoryException {
        if (group != null) {
            Authorizable authorizable = userManager.getAuthorizable(memberName);
            if (authorizable != null) {
                if (!group.isMember(authorizable)) {
                    LOG.info("assign.member: " + authorizable.getID() + " to group: " + group.getID());
                    group.addMember(authorizable);
                }
            }
        }
    }

    protected String[] pathAndName(String pathAndName) {
        String[] result = new String[2];
        int pathEnd = pathAndName.lastIndexOf('/');
        result[0] = pathEnd > 0 ? pathAndName.substring(0, pathEnd) : "";
        result[1] = pathAndName.substring(pathEnd + 1);
        return result;
    }

    protected class NamePrincipal implements Principal {

        private final String name;

        public NamePrincipal(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
