package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.console.ConsoleSlingBean;

/**
 * Created by mzeibig on 17.11.15.
 */
public class Group extends ConsoleSlingBean {
    private String groupId;
    private String groupPath;

    public String getGroupId() {
        return groupId;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getSuffix() {
        return XSS.filter(getRequest().getRequestPathInfo().getSuffix());
    }


}
