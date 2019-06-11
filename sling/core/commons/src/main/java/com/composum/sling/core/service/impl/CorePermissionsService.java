package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.PermissionsService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

@Component(
        label = "Composum Nodes Permissions Service"
)
@Service
public class CorePermissionsService implements PermissionsService {

    private static final Logger LOG = LoggerFactory.getLogger(CorePermissionsService.class);

    /**
     * @param session         the current session
     * @param authorizableIds a list of group ids to check
     * @return the first matching group found; NULL if no group found; probably "" if user is 'admin'
     */
    @Override
    public String isMemberOfOne(Session session, String... authorizableIds) {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable user = userManager.getAuthorizable(session.getUserID());
            for (String authorizableId : authorizableIds) {
                Authorizable authorizable = userManager.getAuthorizable(authorizableId);
                if (authorizable instanceof Group && ((Group) authorizable).isMember(user)) {
                    return authorizableId;
                }
            }
            if (user instanceof User && ((User) user).isAdmin()) {
                return ""; // always 'include' 'admin' user but no specific group returned
            }
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param session         the current session
     * @param authorizableIds a list of group ids to check
     * @return 'true' if session user is member of all groups (or of THE group if only one is checked)
     */
    @Override
    public boolean isMemberOfAll(Session session, String... authorizableIds) {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable user = userManager.getAuthorizable(session.getUserID());
            if (user instanceof User && ((User) user).isAdmin()) {
                return true; // always include 'admin' user
            }
            for (String authorizableId : authorizableIds) {
                Authorizable authorizable = userManager.getAuthorizable(authorizableId);
                if (!(authorizable instanceof Group) || !((Group) authorizable).isMember(user)) {
                    return false;
                }
            }
            return true;
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * @param session       the current session
     * @param privilegeKeys a list of privilege keys to check
     * @return the first privilege found; NULL if no privilege found
     */
    @Override
    public String hasOneOfPrivileges(Session session, String path, String... privilegeKeys) {
        try {
            final AccessControlManager acManager = session.getAccessControlManager();
            final Privilege[] privileges = AccessControlUtils.privilegesFromNames(acManager, privilegeKeys);
            for (Privilege privilege : privileges) {
                if (acManager.hasPrivileges(path, new Privilege[]{privilege})) {
                    return privilege.getName();
                }
            }
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param session       the current session
     * @param privilegeKeys a list of privilege keys to check
     * @return 'true' if session user has all privileges (or has THE privilege if only one is checked)
     */
    @Override
    public boolean hasAllPrivileges(Session session, String path, String... privilegeKeys) {
        try {
            final AccessControlManager acManager = session.getAccessControlManager();
            final Privilege[] privileges = AccessControlUtils.privilegesFromNames(acManager, privilegeKeys);
            return acManager.hasPrivileges(path, privileges);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return false;
    }
}
