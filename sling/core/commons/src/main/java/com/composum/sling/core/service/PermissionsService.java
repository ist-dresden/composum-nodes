package com.composum.sling.core.service;

import javax.jcr.Session;

/**
 * Permission and Member check service
 */
public interface PermissionsService {

    /**
     * @param session         the current session
     * @param authorizableIds a list of group ids to check
     * @return the first matching group found; NULL if no group found; probably "" if user is 'admin'
     */
    String isMemberOfOne(Session session, String... authorizableIds);

    /**
     * @param session         the current session
     * @param authorizableIds a list of group ids to check
     * @return 'true' if session user is member of all groups (or of THE group if only one is checked)
     */
    boolean isMemberOfAll(Session session, String... authorizableIds);

    /**
     * @param session       the current session
     * @param privilegeKeys a list of privilege keys to check
     * @return the first privilege found; NULL if no privilege found
     */
    String hasOneOfPrivileges(Session session, String path, String... privilegeKeys);

    /**
     * @param session       the current session
     * @param privilegeKeys a list of privilege keys to check
     * @return 'true' if session user has all privileges (or has THE privilege if only one is checked)
     */
    boolean hasAllPrivileges(Session session, String path, String... privilegeKeys);
}
