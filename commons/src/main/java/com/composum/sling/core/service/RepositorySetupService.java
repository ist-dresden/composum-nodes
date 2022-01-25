package com.composum.sling.core.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Security configuration management service
 */
public interface RepositorySetupService {

	String USER_PATH = "userPath";
    String GROUP_PATH = "groupPath";
    String MEMBER_OF = "memberOf";

    /**
     * adds ACL accordiong to rules declared as JSON file; e.g.
     * <ul>
     *     <li>allow read for 'everyone' on root ('/') to walk trough (this node only)</li>
	 *     <li>deny read for 'a-group' on '/apps' and all subnodes and ensure that this folder and the group exists</li>
	 *     <li>make 'everyone' and 'someone' a member of 'a-group' (both principals must exist)</li>
     *     <li>remove each ACL for 'a-group' from '/conf' and ensure that this group exists</li>
     * </ul>
     * [{
     *   "path": "/",
     *   "acl": [{
     *     "principal": "everyone",
	 *     "reset": true,
	 *     "rules": [{
	 *       "grant": "jcr:read",
     *       "restrictions": {
     *         "rep:glob": ""
     *       }
     *     }]
     *   }]
     * },{
     *   "path": "/apps",
     *   "jcr:primaryType": "sling:Folder"
     *   "acl": [{
	 *     "principal": [
	 *       "a-group"
	 *       "another-group"
	 *     ],
     *     "groupPath": "example",
	 *     "deny": [
     *         "jcr:read"
     *       ]
     *     }]
     *   },{
	 *     "principal": [
	 *       "everyone",
	 *       "someone"
	 *     ],
     *     "memberOf": [
     *       "a-group"
     *     ]
     *   }]
     * },{
     *   "path": "/conf",
     *   "acl": [{
	 *     "principal": "a-user",
	 *     "userPath": "example",
	 *     "memberOf": [
	 *       "a-group"
	 *     ]
     *   }]
     * }]
     *
     * @param session      the session (not resolver to make it easy usable in install hooks)
     * @param jsonFilePath a repository path to the ACL JSON file
     */
    void addJsonAcl(@NotNull Session session, @NotNull String jsonFilePath,
                    @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    void addJsonAcl(@NotNull Session session, @NotNull Reader reader,
                    @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    /**
     * revert all changes made by 'addJsonAcl'... - use the same configuration file
     */
    void removeJsonAcl(@NotNull Session session, @NotNull String jsonFilePath,
                       @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;

    void removeJsonAcl(@NotNull Session session, @NotNull Reader reader,
                       @Nullable Map<String, Object> values)
            throws RepositoryException, IOException;
}
