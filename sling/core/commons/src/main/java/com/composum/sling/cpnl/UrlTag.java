package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.Format;

/**
 * an abstract base tag implementation to generate URL based elements
 */
public abstract class UrlTag extends TagBase {

    private static final Logger LOG = LoggerFactory.getLogger(UrlTag.class);

    private String urlAttr;
    private String url;
    private Boolean map;
    private String role;
    private String format;
    private Format formatter;

    @Override
    protected void clear() {
        super.clear();
        urlAttr = null;
        url = null;
        map = null;
        role = null;
        format = null;
        formatter = null;
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

    /**
     * @param format the fmt to set to build a url from the 'url' value
     */
    public void setFormat(String format) {
        this.format = format;
    }

    public Format getFormatter(Object value) {
        if (formatter == null && format != null) {
            formatter = CpnlElFunctions.getFormatter(pageContext.getRequest().getLocale(),
                    format, value != null ? value.getClass() : null);
        }
        return formatter;
    }

    @Override
    protected void writeAttributes(JspWriter writer) throws IOException {
        writer.write(" ");
        writer.write(getUrlAttr());
        writer.write("=\"");
        String urlValue = url;
        Format format = getFormatter(urlValue);
        if (format != null) {
            urlValue = format.format(urlValue);
        }
        if (map != null) {
            urlValue = map
                    ? CpnlElFunctions.mappedUrl(request, urlValue)
                    : CpnlElFunctions.unmappedUrl(request, urlValue);
        } else {
            urlValue = CpnlElFunctions.url(request, urlValue);
        }
        writer.write(urlValue);
        writer.write("\"");
        if (StringUtils.isNotBlank(role)) {
            writer.write(" role=\"");
            writer.write(CpnlElFunctions.text(role));
            writer.write("\"");
        }
        super.writeAttributes(writer);
    }
}
