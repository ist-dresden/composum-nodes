package com.composum.sling.cpnl;

import javax.servlet.jsp.JspException;

/**
 * a tag to build hypertext links with mapped URLs
 */
public class LinkTag extends UrlTag {

    protected Object body;
    private transient Boolean bodyResult;

    protected void clear() {
        super.clear();
        bodyResult = null;
        body = null;
    }

    /**
     * if the 'body' expression is 'true' the body is rendered without a link around even if 'test' is 'false'
     */
    public void setBody(Object value) {
        body = value;
    }

    /**
     * evaluates the 'body' test expression if present and returns the evaluation result; default: 'true'
     */
    protected boolean getBodyResult() {
        if (bodyResult == null) {
            bodyResult = eval(body, body instanceof Boolean ? (Boolean) body : Boolean.TRUE);
        }
        return bodyResult;
    }

    @Override
    public int doStartTag() throws JspException {
        int result = super.doStartTag();
        return body != null ? (getBodyResult() ? EVAL_BODY_INCLUDE : SKIP_BODY) : result;
    }

    protected String getDefaultTagName() {
        return "a";
    }

    protected String getDefaultUrlAttr() {
        return "href";
    }

    public void setHref(String href) {
        setUrl(href);
    }
}
