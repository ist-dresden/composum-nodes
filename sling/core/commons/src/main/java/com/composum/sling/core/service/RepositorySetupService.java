package com.composum.sling.core.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Security configuration management service
 */
public interface RepositorySetupService {

    String GROUP_PATH = "groupPath";
    String MEMBER_OF = "memberOf";

    /**
     * adds ACL accordiong to rules declared as JSON file; e.g.
     * <ul>
     *     <li>allow read for 'everyone' on root ('/') to walk trough (this node only)</li>
     *     <li>allow read for 'a-group' on '/apps' and all subnodes and ensure that this folder and the group exists</li>
     *     <li>make 'everyone' a member of 'a-group' (both principals must exist)</li>
     *     <li>remove each ACL for 'a-group' from '/conf' and ensure that this group exists</li>
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
     *   "jcr:primaryType": "sling:Folder"
     *   "acl": [{
     *     "principal": "a-group",
     *     "groupPath": "example",
     *     "acl": [{
     *       "allow": true,
     *       "privileges": [
     *         "jcr:read"
     *       ]
     *     }]
     *   },{
     *     "principal": "everyone",
     *     "memberOf": [
     *       "a-group"
     *     ]
     *   }]
     * },{
     *   "path": "/conf",
     *   "acl": [{
     *     "principal": "a-group"
     *   }]
     * }]
     *
     * @param session      the session (not resolver to make it easy usable in install hooks)
     * @param jsonFilePath a repository path to the ACL JSON file
     */
    void addJsonAcl(@Nonnull Session session, @Nonnull String jsonFilePath,
                    @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    void addJsonAcl(@Nonnull Session session, @Nonnull Reader reader,
                    @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    /**
     * revert all changes made by 'addJsonAcl'... - use the same configuration file
     */
    void removeJsonAcl(@Nonnull Session session, @Nonnull String jsonFilePath,
                       @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    void removeJsonAcl(@Nonnull Session session, @Nonnull Reader reader,
                       @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;
}
