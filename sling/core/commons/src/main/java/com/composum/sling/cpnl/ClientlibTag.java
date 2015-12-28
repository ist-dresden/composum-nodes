package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag build references to styles and script files
 */
public class ClientlibTag extends CpnlBodyTagSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibTag.class);

    enum Type {link, css, js}

    private Type type;
    private String path;
    private String rel;

    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    protected void setType(Type type) {
        this.type = type;
    }

    protected Type getType() {
        if (type == null) {
            String ext = StringUtils.substringAfterLast(path, ".").toLowerCase();
            try {
                setType(ext);
            } catch (Exception ex) {
                setType(Type.link);
            }
        }
        return type;
    }

    public void setPath(String path) {
        this.path = path;
    }

    protected String getPath() {
        if (!path.startsWith("/")) {
            Resource libResource = resourceResolver.getResource("/apps/" + path);
            if (libResource != null) {
                path = libResource.getPath();
            } else {
                libResource = resourceResolver.getResource("/libs/" + path);
                if (libResource != null) {
                    path = libResource.getPath();
                }
            }
        }
        return path;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    protected void reset() {

    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        reset();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            JspWriter writer = this.pageContext.getOut();
            switch (getType()) {
                case link:
                case css:
                    writer.write("<link rel=\"");
                    writer.write(rel != null ? rel : "stylesheet");
                    writer.write("\" href=\"");
                    writer.write(CpnlElFunctions.url(request, getPath()));
                    writer.write("\" />");
                    break;
                case js:
                    writer.write("<script type=\"text/javascript\" src=\"");
                    writer.write(CpnlElFunctions.url(request, getPath()));
                    writer.write("\"></script>");
                    break;
                default:
                    break;
            }
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
        super.doEndTag();
        return EVAL_PAGE;
    }
}
