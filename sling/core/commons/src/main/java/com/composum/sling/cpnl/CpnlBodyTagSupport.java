package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.ExpressionUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.jsp.util.TagUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;


@SuppressWarnings("serial")
public class CpnlBodyTagSupport extends BodyTagSupport {

    protected SlingHttpServletRequest request;
    protected BeanContext context;
    protected JspWriter out;

    protected Resource resource;
    protected ResourceResolver resourceResolver;

    private transient ExpressionUtil expressionUtil;

    /**
     * Reset all member variables to the (default) start values. Called prior
     * processing the tag and at release time.
     */
    protected void clear() {
        resource = null;
        resourceResolver = null;
        context = null;
        out = null;
        request = null;
        expressionUtil = null;
    }

    @Override
    public int doStartTag() throws JspException {
        context = createContext(pageContext);
        out = pageContext.getOut();
        request = TagUtil.getRequest(pageContext);
        resourceResolver = request.getResourceResolver();
        resource = request.getResource();

        return super.doStartTag();
    }

    protected BeanContext createContext(PageContext pageContext) {
        return new BeanContext.Page(pageContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.servlet.jsp.tagext.TagSupport#setPageContext(javax.servlet.jsp.
     * PageContext)
     */
    @Override
    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);
        clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release() {
        clear();
        super.release();
    }

    /**
     * Returns or creates the expressionUtil . Not null.
     */
    protected com.composum.sling.core.util.ExpressionUtil getExpressionUtil() {
        if (expressionUtil == null) {
            expressionUtil = new ExpressionUtil(pageContext);
        }
        return expressionUtil;
    }


    /**
     * evaluate an EL expression value, the value can contain @{..} expression rules which are transformed to ${..}
     */
    protected <T> T eval(Object value, T defaultValue) {
        return getExpressionUtil().eval(value, defaultValue);
    }

}
