package com.composum.sling.core.usermanagement.core;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.RepositoryException;

/**
 * a simple service to provide a user and groups setup API
 */
public interface UserManagementService {

    /**
     * Retrieves a user by its name; if the user doesn't exist the user will be created.
     *
     * @param pathAndName the name of the user with an optional prepended intermediate path for user creation
     */
    Authorizable getOrCreateUser(JackrabbitSession session, UserManager userManager,
                                 String pathAndName, boolean systemUser)
            throws RepositoryException;

    /**
     * Retrieves a group by their name; if the group doesn't exist the group will be created.
     *
     * @param pathAndName the name of the group with an optional prepended intermediate path for group creation
     */
    Group getOrCreateGroup(JackrabbitSession session, UserManager userManager,
                           String pathAndName)
            throws RepositoryException;

    /**
     * Assign an member (user or group) to a group if not always a member.
     *
     * @param authorizable the name of the authorizable (the name only)
     * @param groupName    the name of the group (the simple name only)
     */
    void assignToGroup(JackrabbitSession session, UserManager userManager,
                       Authorizable authorizable, String groupName)
            throws RepositoryException;

    /**
     * Assign an member (user or group) to a group if not always a member.
     *
     * @param memberName the name of the authorizable (the name only)
     */
    void assignToGroup(JackrabbitSession session, UserManager userManager,
                       String memberName, Group group)
            throws RepositoryException;
}
