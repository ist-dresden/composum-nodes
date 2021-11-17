package com.composum.sling.nodes.tools;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.bean.RestrictedBean;
import com.composum.sling.core.servlet.RestrictionsServlet;

@Restricted(key = RestrictionsServlet.SERVICE_KEY)
public class RestrictionsView extends AbstractSlingBean {

    protected void retrieveRestrictedBeans() {

        try {
            Class<?> restictedBeanType = context.getType(RestrictedBean.class.getName());
        } catch (ClassNotFoundException ex) {
        }
    }
}
