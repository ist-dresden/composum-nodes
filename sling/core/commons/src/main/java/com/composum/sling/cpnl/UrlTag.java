package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * an abstract base tag implementation to generate URL based elements
 */
public abstract class UrlTag extends CpnlBodyTagSupport implements DynamicAttributes {

    private static final Logger LOG = LoggerFactory.getLogger(UrlTag.class);

    private String tagName;
    private String urlAttr;
    private String url;
    private Boolean map;
    private String role;
    private String classes;

    protected Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    protected void clear() {
        super.clear();
        dynamicAttributes = new LinkedHashMap<>();
        tagName = null;
        urlAttr = null;
        url = null;
        map = null;
        role = null;
        classes = null;
    }

    protected abstract String getDefaultTagName();

    protected abstract String getDefaultUrlAttr();

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    protected String getTagName() {
        return StringUtils.isNotBlank(tagName) ? tagName : getDefaultTagName();
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

    public void setClasses(String classes) {
        this.classes = classes;
    }

    //
    // dynamic tag attributes
    //

    /**
     * extension hook to check and filter dynamic attributes
     */
    protected boolean acceptDynamicAttribute(String key, Object value) throws JspException {
        return value != null;
    }

    public void setDynamicAttribute(String namespace, String name, Object value) throws JspException {
        String key = name;
        if (StringUtils.isNotBlank(namespace)) {
            key = namespace + ":" + key;
        }
        if (acceptDynamicAttribute(key, value)) {
            dynamicAttributes.put(key, value);
        }
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
        if (StringUtils.isNotBlank(classes)) {
            writer.write(" class=\"");
            writer.write(classes);
            writer.write("\"");
        }
        for (Map.Entry<String, Object> entry : dynamicAttributes.entrySet()) {
            String string = entry.getValue().toString();
            writer.write(" ");
            writer.write(entry.getKey());
            writer.write("=\"");
            writer.write(eval(string, string));
            writer.write("\"");
        }
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        try {
            JspWriter writer = this.pageContext.getOut();
            writer.write("<");
            writer.write(getTagName());
            writeAttributes(writer);
            writer.write(">");
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            JspWriter writer = this.pageContext.getOut();
            writer.write("</");
            writer.write(getTagName());
            writer.write(">");
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
        super.doEndTag();
        return EVAL_PAGE;
    }
}
