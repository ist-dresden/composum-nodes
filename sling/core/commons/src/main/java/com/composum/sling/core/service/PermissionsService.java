package com.composum.sling.core.service;

import javax.jcr.Session;

/**
 * Permission and Member check service
 */
public interface PermissionsService {

    /**
     * @return the first matching member or NULL if not a member
     */
    String isMemberOfOne(Session session, String... authorizableIds);

    boolean isMemberOfAll(Session session, String... authorizableIds);

    /**
     * @return the first matching privilege or NULL if check fails
     */
    String hasOneOfPrivileges(Session session, String path, String... privilegeKeys);

    boolean hasAllPrivileges(Session session, String path, String... privilegeKeys);
}
