package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * an abstract base tag implementation to generate URL based elements
 */
public abstract class UrlTag extends TagBase {

    private static final Logger LOG = LoggerFactory.getLogger(UrlTag.class);

    private String urlAttr;
    private String url;
    private Boolean map;
    private String role;

    protected void clear() {
        super.clear();
        urlAttr = null;
        url = null;
        map = null;
        role = null;
    }

    protected abstract String getDefaultUrlAttr();

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUrlAttr(String urlAttr) {
        this.urlAttr = urlAttr;
    }

    protected String getUrlAttr() {
        return StringUtils.isNotBlank(urlAttr) ? urlAttr : getDefaultUrlAttr();
    }

    public void setMap(Boolean mapIt) {
        this.map = mapIt;
    }

    public void setRole(String role) {
        this.role = role;
    }

    protected void writeAttributes(JspWriter writer) throws IOException {
        writer.write(" ");
        writer.write(getUrlAttr());
        writer.write("=\"");
        String urlValue = null;
        if (map != null) {
            urlValue = map
                    ? CpnlElFunctions.mappedUrl(request, url)
                    : CpnlElFunctions.unmappedUrl(request, url);
        } else {
            urlValue = CpnlElFunctions.url(request, url);
        }
        writer.write(urlValue);
        writer.write("\"");
        if (StringUtils.isNotBlank(role)) {
            writer.write(" role=\"");
            writer.write(role);
            writer.write("\"");
        }
        super.writeAttributes(writer);
    }
}
