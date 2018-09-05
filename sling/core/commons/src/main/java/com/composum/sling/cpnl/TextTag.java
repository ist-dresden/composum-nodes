package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.scripting.jsp.util.TagUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTag extends TagBase {

    private static final Logger LOG = LoggerFactory.getLogger(TextTag.class);

    public enum Type {text, rich, script, cdata, path, value}

    public interface EscapeFunction {
        Object escape(SlingHttpServletRequest request, Object value);
    }

    public static final Map<Type, EscapeFunction> ESCAPE_FUNCTION_MAP;

    static {
        ESCAPE_FUNCTION_MAP = new HashMap<>();
        ESCAPE_FUNCTION_MAP.put(Type.text, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.text(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.rich, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.rich(request, TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.script, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.script(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.cdata, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.cdata(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.path, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.path(TextTag.toString(value));
            }
        });
        ESCAPE_FUNCTION_MAP.put(Type.value, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.value(value);
            }
        });
    }

    protected Type type = Type.text;
    private Object value;
    private String propertyName;
    private boolean escape = true;
    private boolean i18n = false;
    private Format format;
    private String output;

    public TextTag() {
        super();
    }

    @Override
    protected void clear() {
        this.type = Type.text;
        this.value = null;
        this.propertyName = null;
        this.format = null;
        this.i18n = false;
        this.escape = true;
        this.output = null;
        super.clear();
    }

    @Override
    protected String getDefaultTagName() {
        return "div";
    }

    @Override
    public int doStartTag() throws JspException {
        int result = super.doStartTag();
        if (renderTag()) {
            this.bodyContent = null;
            this.output = null;
            if (this.value == null) {
                if (this.propertyName != null) {
                    Resource resource = TagUtil.getRequest(this.pageContext).getResource();
                    this.value = ResourceUtil.getValueMap(resource).get(this.propertyName, Object.class);
                }
            }
            if (this.value != null) {
                if (this.format != null) {
                    this.output = this.format.format(
                            this.format instanceof MessageFormat
                                    ? new String[]{String.valueOf(this.value)}
                                    : this.value);
                } else {
                    this.output = String.valueOf(this.value);
                }
            }
            if (StringUtils.isNotBlank(this.output) && this.i18n) {
                this.output = CpnlElFunctions.i18n(this.request, this.output);
            }
            return this.output != null ? SKIP_BODY : EVAL_BODY_BUFFERED;
        }
        return result;
    }

    public int doAfterBody() {
        this.output = this.bodyContent.getString().trim();
        return SKIP_BODY;
    }

    @Override
    protected void renderTagStart() {
    }

    @Override
    protected void renderTagEnd() {
        try {
            if (StringUtils.isNotEmpty(this.output)) {
                this.output = toString(this.escape
                        ? escape(this.output)
                        : this.output);
                JspWriter writer = this.pageContext.getOut();
                boolean renderTag = renderTag()
                        && (StringUtils.isNotBlank(this.tagName) || StringUtils.isNotBlank(this.classes));
                if (renderTag) {
                    super.renderTagStart();
                }
                writer.write(this.output);
                if (renderTag) {
                    super.renderTagEnd();
                }
            }
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
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
        return function != null ? function.escape(TagUtil.getRequest(this.pageContext), value) : CpnlElFunctions.text(toString(value));
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
     * @param i18n flag for translating the text (default: 'false')
     */
    public void setI18n(boolean i18n) {
        this.i18n = i18n;
    }

    /**
     * @param format the fmt to set
     */
    public void setFormat(String format) {
        Pattern TEXT_FORMAT_STRING = Pattern.compile("\\{([^}]+)}(.+)$");
        Matcher matcher = TEXT_FORMAT_STRING.matcher(format);
        if (matcher.matches()) {
            switch (matcher.group(1)) {
                case "Message":
                    this.format = new MessageFormat(matcher.group(2));
                    break;
                case "Date":
                    this.format = new SimpleDateFormat(matcher.group(2));
                    break;
            }
        }
    }

    /**
     * @param tagClass the tagClass to set
     */
    public void setTagClass(String tagClass) {
        setClasses(tagClass);
    }
}
