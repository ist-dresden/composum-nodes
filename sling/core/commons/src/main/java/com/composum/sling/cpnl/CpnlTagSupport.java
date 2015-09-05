package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingHandle;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.jsp.util.TagUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;


@SuppressWarnings("serial")
public class CpnlTagSupport extends TagSupport {

    protected SlingHttpServletRequest request;
    protected SlingHandle sling;
    protected JspWriter out;

    protected Resource resource;
    protected ResourceResolver resourceResolver;

    /**
     * Reset all member variables to the (default) start values. Called prior
     * processing the tag and at release time.
     */
    protected void clear() {
        resource = null;
        resourceResolver = null;
        sling = null;
        out = null;
        request = null;
    }

    @Override
    public int doStartTag() throws JspException {
        sling = new SlingHandle(new BeanContext.Page(pageContext));
        out = pageContext.getOut();

        request = TagUtil.getRequest(pageContext);
        resourceResolver = request.getResourceResolver();
        resource = request.getResource();

        return super.doStartTag();
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
}
