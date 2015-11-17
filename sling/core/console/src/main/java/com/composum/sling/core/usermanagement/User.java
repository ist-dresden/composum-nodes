package com.composum.sling.core.usermanagement;

import com.composum.sling.core.AbstractSlingBean;

/**
 * Created by mzeibig on 16.11.15.
 */
public class User extends AbstractSlingBean {
    private String userId = "testid";
    private String userPath = "/test/userPath";

    public String getUserId() {
        return userId;
    }

    public String getUserPath() {
        return userPath;
    }

    public String getSuffix() {
        return getRequest().getRequestPathInfo().getSuffix();
    }


}
