/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.sling.cpnl;

import javax.servlet.jsp.JspException;

/**
 * a tag to render a 'div' HTML tag with support for a 'test' condition
 */
public class DivTag extends TagBase {

    protected Object body;
    private transient Boolean bodyResult;

    @Override
    protected void clear() {
        super.clear();
        bodyResult = null;
        body = null;
    }

    @Override
    protected String getDefaultTagName() {
        return "div";
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
}
