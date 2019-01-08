package com.composum.sling.nodes.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Map;

/**
 * Security configuration management service
 */
public interface SecurityService {

    /**
     * adds ACL accordiong to rules declared as JSON file; e.g.
     * <ul>
     *     <li>allow read for 'everyone' on root '/' to walk trough (this node only)</li>
     *     <li>allow read for 'everyone' on root '/apps' and all subnodes</li>
     *     <li>remove each ACL for 'everyone' from '/conf'</li>
     * </ul>
     * [{
     *   "path": "/",
     *   "acl": [{
     *     "principal": "everyone",
     *     "acl": [{
     *       "allow": true,
     *       "privileges": "jcr:read",
     *       "restrictions": {
     *         "rep:glob": ""
     *       }
     *     }]
     *   }]
     * },{
     *   "path": "/apps",
     *   "acl": [{
     *     "principal": "everyone",
     *     "acl": [{
     *       "allow": true,
     *       "privileges": [
     *         "jcr:read"
     *       ]
     *     }]
     *   }]
     * },{
     *   "path": "/conf",
     *   "acl": [{
     *     "principal": "everyone"
     *   }]
     * }]
     *
     * @param session      the session (not resolver to make it easy usable in install hooks)
     * @param jsonFilePath a repository path to the ACL JSON file
     */
    void addJsonAcl(@Nonnull Session session, @Nonnull String jsonFilePath)
            throws RepositoryException, IOException;

    void addAcl(@Nonnull Session session, @Nonnull String path,
                @Nonnull String principal, boolean allow,
                @Nonnull String[] privileges,
                @Nonnull Map restrictions)
            throws RepositoryException;

    void removeAcl(@Nonnull Session session, @Nonnull String path, @Nullable String principal)
            throws RepositoryException;
}
