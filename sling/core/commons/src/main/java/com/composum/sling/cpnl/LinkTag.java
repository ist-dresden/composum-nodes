package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag to build hypertext links with mapped URLs
 */
public class LinkTag extends UrlTag {

    private String alt;

    protected String getDefaultTagName() {
        return "a";
    }

    protected String getDefaultUrlAttr() {
        return "href";
    }

    public void setHref(String href) {
        setUrl(href);
    }

    protected void clear() {
        super.clear();
        alt = null;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    protected void writeAttributes (JspWriter writer) throws IOException {
        super.writeAttributes(writer);
        if (StringUtils.isNotBlank(alt)) {
            writer.write(" alt=\"");
            writer.write(alt);
            writer.write("\"");
        }
    }
}
