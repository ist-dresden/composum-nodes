package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;

import javax.servlet.jsp.PageContext;

public class DefineObjectsTag extends org.apache.sling.scripting.jsp.taglib.DefineObjectsTag {

    public static final String RA_CONTEXT_PATH = "contextPath";
    public static final String RA_BEAN_CONTEXT = "beanContext";
    public static final String RA_COMPOSUM_BASE = "composumBase";

    protected BeanContext context;
    protected String composumBase;

    @Override
    public int doEndTag() {
        int result = super.doEndTag();
        context = createContext(pageContext);
        composumBase = context.getService(CoreConfiguration.class).getComposumBase();
        context.setAttribute(RA_CONTEXT_PATH, context.getRequest().getContextPath(), BeanContext.Scope.request);
        context.setAttribute(RA_BEAN_CONTEXT, context, BeanContext.Scope.request);
        context.setAttribute(RA_COMPOSUM_BASE, composumBase, BeanContext.Scope.request);

        return result;
    }

    protected BeanContext createContext(PageContext pageContext) {
        return new BeanContext.Page(pageContext);
    }
}
