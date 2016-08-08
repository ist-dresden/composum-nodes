package com.composum.sling.core.usermanagement.view;

import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mzeibig on 16.11.15.
 */
public class User extends ConsoleSlingBean {
    private org.apache.jackrabbit.api.security.user.User user;

    public org.apache.jackrabbit.api.security.user.User getUser() throws RepositoryException {
        if (this.user == null)  {
            final JackrabbitSession session = (JackrabbitSession) getSession();
            final UserManager userManager = session.getUserManager();
            Authorizable authorizableByPath = userManager.getAuthorizableByPath(getRequest().getRequestPathInfo().getSuffix());
            this.user = (org.apache.jackrabbit.api.security.user.User) authorizableByPath;
        }
        return this.user;
    }

    public String getUserId() throws RepositoryException {
        return getUser().getID();
    }

    public String getUserPath() throws RepositoryException {
        return getUser().getPath();
    }

    public boolean isAdmin() throws RepositoryException {
        return getUser().isAdmin();
    }

    public boolean isDisabled() throws RepositoryException {
        return getUser().isDisabled();
    }

    public String getDisabledReason() throws RepositoryException {
        return getUser().getDisabledReason();
    }

    public List<String> getGroups() throws RepositoryException {
        List<String> groups = new ArrayList<>();
        Iterator<org.apache.jackrabbit.api.security.user.Group> groupIterator = getUser().memberOf();
        while (groupIterator.hasNext()) {
            Group group = groupIterator.next();
            groups.add(group.getID());
        }
        return groups;
    }

    public String getSuffix() {
        return getRequest().getRequestPathInfo().getSuffix();
    }


}
