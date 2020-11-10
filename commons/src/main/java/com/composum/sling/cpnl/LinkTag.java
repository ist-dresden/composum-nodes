package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag to build hypertext links with mapped URLs
 */
public class LinkTag extends UrlTag {

    protected String target;
    protected Object body;
    private transient Boolean bodyResult;

    @Override
    protected void clear() {
        super.clear();
        bodyResult = null;
        body = null;
        target = null;
    }

    public void setTarget(String target) {
        this.target = target;
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

    @Override
    protected String getDefaultTagName() {
        return "a";
    }

    @Override
    protected String getDefaultUrlAttr() {
        return "href";
    }

    public void setHref(String href) {
        setUrl(href);
    }

    @Override
    protected void writeAttributes(JspWriter writer) throws IOException {
        super.writeAttributes(writer);
        if (StringUtils.isNotBlank(target)) {
            writer.write(" target=\"");
            writer.write(CpnlElFunctions.text(target));
            writer.write("\"");
        }
    }
}
