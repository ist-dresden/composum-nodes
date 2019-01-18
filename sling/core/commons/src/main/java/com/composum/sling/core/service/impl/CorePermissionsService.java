package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.PermissionsService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
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
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public boolean isMemberOfAll(Session session, String... authorizableIds) {
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable user = userManager.getAuthorizable(session.getUserID());
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
