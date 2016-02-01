package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag to build form elements with mapped action URLs
 */
public class TableTag extends UrlTag {

    private String toolbar;

    protected String getDefaultTagName() {
        return "table";
    }

    protected String getDefaultUrlAttr() {
        return "data-path";
    }

    public void setPath(String path) {
        setUrl(path);
    }

    protected void clear() {
        super.clear();
        toolbar = null;
    }

    public void setToolbar(String toolbar) {
        this.toolbar = toolbar;
    }

    protected void writeAttributes (JspWriter writer) throws IOException {
        super.writeAttributes(writer);
        if (StringUtils.isNotBlank(toolbar)) {
            writer.write(" data-toolbar=\"");
            writer.write(toolbar);
            writer.write("\"");
        }
    }
}
