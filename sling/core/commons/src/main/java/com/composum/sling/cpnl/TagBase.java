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
 * an abstract base tag implementation to generate HTML tags with dynamic attributes and an optional condition
 */
public abstract class TagBase extends CpnlBodyTagSupport implements DynamicAttributes {

    private static final Logger LOG = LoggerFactory.getLogger(TagBase.class);

    protected String tagName;
    protected String classes;

    protected Object test;
    private transient Boolean testResult;

    protected Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    protected void clear() {
        super.clear();
        dynamicAttributes = new LinkedHashMap<>();
        testResult = null;
        test = null;
        classes = null;
        tagName = null;
    }

    protected abstract String getDefaultTagName();

    /**
     * @param tagName the tagName to set
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    protected String getTagName() {
        return StringUtils.isNotBlank(tagName) ? tagName : getDefaultTagName();
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    /**
     * the 'test' expression for conditional tags
     */
    public void setTest(Object value) {
        test = value;
    }

    /**
     * evaluates the test expression if present and returns the evaluation result; default: 'true'
     */
    protected boolean getTestResult() {
        if (testResult == null) {
            testResult = eval(test, test instanceof Boolean ? (Boolean) test : Boolean.TRUE);
        }
        return testResult;
    }

    //
    // dynamic tag attributes
    //

    /**
     * extension hook to check and filter dynamic attributes
     */
    protected boolean acceptDynamicAttribute(String key, Object value) {
        return value != null;
    }

    public void setDynamicAttribute(String namespace, String name, Object value) {
        String key = name;
        if (StringUtils.isNotBlank(namespace)) {
            key = namespace + ":" + key;
        }
        if (acceptDynamicAttribute(key, value)) {
            dynamicAttributes.put(key, value);
        }
    }

    protected void writeAttributes(JspWriter writer) throws IOException {
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

    /**
     * if this returns 'false' nothing is rendered (extension hook; returns the test result)
     */
    protected boolean renderTag() {
        return getTestResult();
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        if (renderTag()) {
            renderTagStart();
            return EVAL_BODY_INCLUDE;
        } else {
            return SKIP_BODY;
        }
    }

    protected void renderTagStart() {
        try {
            JspWriter writer = this.pageContext.getOut();
            writer.write("<");
            writer.write(getTagName());
            writeAttributes(writer);
            writer.write(">");
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
    }

    @Override
    public int doEndTag() throws JspException {
        if (renderTag()) {
            renderTagEnd();
        }
        super.doEndTag();
        return EVAL_PAGE;
    }

    protected void renderTagEnd() {
        try {
            JspWriter writer = this.pageContext.getOut();
            writer.write("</");
            writer.write(getTagName());
            writer.write(">");
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
    }
}
