package com.composum.sling.core.usermanagement;

import com.composum.sling.core.AbstractSlingBean;

import javax.jcr.Session;

/**
 * Created by mzeibig on 16.11.15.
 */
public class UserManagement extends AbstractSlingBean {

    public String getViewType() {
//        final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
//        final UserManager userManager = session.getUserManager();
        String primaryType = resource.getPrimaryType();
        return "user";
    }

    public String getPath() {
        String suffix = getRequest().getRequestPathInfo().getSuffix();
        return suffix;
    }
}
