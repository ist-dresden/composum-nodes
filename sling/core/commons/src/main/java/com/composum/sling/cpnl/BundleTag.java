package com.composum.sling.cpnl;

import com.composum.sling.core.RequestBundle;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.JspException;

public class BundleTag extends CpnlBodyTagSupport {

    protected String basename;

    protected void clear() {
        basename = null;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        if (StringUtils.isNotBlank(basename)) {
            RequestBundle.get(context.getRequest()).push(basename);
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        if (StringUtils.isNotBlank(basename)) {
            RequestBundle.get(context.getRequest()).pop();
        }
        super.doEndTag();
        return EVAL_PAGE;
    }
}
