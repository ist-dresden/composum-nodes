package com.composum.sling.core.usermanagement;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.filter.StringFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by Mirko Zeibig on 16.11.15.
 */
public class UserManagement extends AbstractSlingBean {

    public String getViewType() {
        try {
            final JackrabbitSession session = (JackrabbitSession) getSession();
            final UserManager userManager = session.getUserManager();
            String suffix = getRequest().getRequestPathInfo().getSuffix();
            if (suffix != null) {
                Authorizable authorizableByPath = userManager.getAuthorizableByPath(suffix);
                if (authorizableByPath == null) {
                    return "blank";
                } else if (authorizableByPath.isGroup()) {
                    return "group";
                } else {
                    return "user";
                }
            } else {
                return "blank";
            }
        } catch (RepositoryException e) {
            return "blank";
        }
    }

    public String getPath() {
        String suffix = getRequest().getRequestPathInfo().getSuffix();
        return suffix;
    }

    public String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "general";
    }

}
