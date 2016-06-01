package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.scripting.jsp.util.TagUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;

public class TextTag extends CpnlBodyTagSupport {

    public enum Type {text, rich, script, value}

    public interface EscapeFunction {
        Object escape(Object value);
    }

    public static final Map<Type, EscapeFunction> ESCAPE_FUNCTION_MAP;

    static {
        ESCAPE_FUNCTION_MAP = new HashMap<>();
        ESCAPE_FUNCTION_MAP.put(Type.text, new EscapeFunction() {
            @Override
            public Object escape(Object value) {
                return CpnlElFunctions.text(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.rich, new EscapeFunction() {
            @Override
            public Object escape(Object value) {
                return CpnlElFunctions.rich(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.script, new EscapeFunction() {
            @Override
            public Object escape(Object value) {
                return CpnlElFunctions.script(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.value, new EscapeFunction() {
            @Override
            public Object escape(Object value) {
                return CpnlElFunctions.value(value);
            }
        });
    }

    protected Type type = Type.text;
    private Object value;
    private String propertyName;
    private boolean escape = true;
    private Format format;
    private String output;
    private String tagClass;
    private String tagName;

    public TextTag() {
        super();
    }

    private void init() {
        this.type = Type.text;
        this.value = null;
        this.propertyName = null;
        this.format = null;
        this.output = null;
        this.tagName = null;
        this.tagClass = null;
    }

    public void release() {
        super.release();
        init();
    }

    public int doStartTag() throws JspException {
        this.bodyContent = null;
        this.output = null;
        if (this.value != null) {
            this.output = String.valueOf(this.value);
        }
        if ((this.output == null) && (this.propertyName != null)) {
            Resource resource = TagUtil.getRequest(this.pageContext).getResource();
            this.output = ((String) ResourceUtil.getValueMap(resource).get(this.propertyName, String.class));
        }
        return this.output != null ? SKIP_BODY : EVAL_BODY_BUFFERED;
    }

    public int doAfterBody() throws JspException {
        this.output = this.bodyContent.getString().trim();
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        try {
            if ((this.output != null) && (this.format != null)) {
                this.output = this.format.format(this.output);
            }
            if (StringUtils.isNotEmpty(output)) {
                this.output = toString(this.escape
                        ? escape(this.output)
                        : this.output);
                JspWriter writer = this.pageContext.getOut();
                if (StringUtils.isNotBlank(this.tagName) || StringUtils.isNotBlank(this.tagClass)) {
                    if (this.tagName == null) {
                        this.tagName = "div";
                    }
                    writer.write("<");
                    writer.write(this.tagName);
                    if (StringUtils.isNotBlank(this.tagClass)) {
                        writer.write(" class=\"");
                        writer.write(this.tagClass);
                        writer.write("\"");
                    }
                    writer.write(">");
                }
                writer.write(this.output);
                if (StringUtils.isNotBlank(this.tagName)) {
                    writer.write("</");
                    writer.write(this.tagName);
                    writer.write(">");
                }
            }
        } catch (IOException e) {
            throw new JspTagException(e);
        }
        return EVAL_PAGE;
    }

    public static String toString(Object value) {
        return value instanceof String ? (String) value : (value != null ? value.toString() : "");
    }

    /**
     * the extension hook for the various types of encoding; must work with Object values to ensure
     * that non String values can be used as is
     *
     * @param value the value (String) to encode
     * @return the encoded String or an appropriate Object
     */
    protected Object escape(Object value) {
        EscapeFunction function = ESCAPE_FUNCTION_MAP.get(this.type);
        return function != null ? function.escape(value) : CpnlElFunctions.text(toString(value));
    }

    /**
     * @param type the type of the value and the escape strategy key
     */
    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @param escape flag for escaping the text (default: 'true')
     */
    public void setEscape(boolean escape) {
        this.escape = escape;
    }

    /**
     * @param format the fmt to set
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * @param tagName the tagName to set
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * @param tagClass the tagClass to set
     */
    public void setTagClass(String tagClass) {
        this.tagClass = tagClass;
    }
}
