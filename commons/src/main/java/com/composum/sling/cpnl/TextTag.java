/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.sling.cpnl;

import com.composum.sling.core.util.I18N;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TextTag extends TagBase {

    private static final Logger LOG = LoggerFactory.getLogger(TextTag.class);

    public enum Type {text, rich, script, style, cdata, path, value}

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
        ESCAPE_FUNCTION_MAP.put(Type.style, new EscapeFunction() {
            @Override
            public Object escape(SlingHttpServletRequest request, Object value) {
                return CpnlElFunctions.style(TextTag.toString(value));
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
    private Format formatter;
    private String format;
    private Locale locale;
    private String output;

    public TextTag() {
        super();
    }

    @Override
    protected void clear() {
        this.type = Type.text;
        this.value = null;
        this.propertyName = null;
        this.locale = null;
        this.format = null;
        this.formatter = null;
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
            Format formatter = null;
            if (this.value != null) {
                formatter = getFormatter(this.value);
                if (formatter != null) {
                    String stringValue = this.value instanceof String ? (String) this.value : null;
                    if (StringUtils.isNotBlank(stringValue) && this.i18n) {
                        stringValue = I18N.get(this.request, stringValue);
                    }
                    this.output = formatter.format(
                            formatter instanceof MessageFormat
                                    ? new String[]{stringValue != null ? stringValue : toString(this.value)}
                                    : this.value instanceof Calendar ? ((Calendar) this.value).getTime()
                                    : stringValue != null ? stringValue : this.value);
                } else {
                    this.output = toString(this.value);
                }
            }
            if (StringUtils.isNotBlank(this.output) && this.i18n && formatter == null) {
                this.output = I18N.get(this.request, this.output);
            }
            return this.output != null ? SKIP_BODY : EVAL_BODY_BUFFERED;
        }
        return result;
    }

    @Override
    public int doAfterBody() {
        this.output = this.bodyContent.getString().trim();
        return SKIP_BODY;
    }

    @Override
    protected void renderTagStart() {
    }

    /**
     * is rendering the text and a tag around if 'tagName' is set or CSS classes are specified
     */
    @Override
    protected void renderTagEnd() {
        try {
            if (StringUtils.isNotEmpty(this.output)) {
                this.output = toString(this.escape
                        ? escape(this.output)
                        : this.output);
                JspWriter writer = this.pageContext.getOut();
                boolean renderTag = renderTag()
                        && (StringUtils.isNotBlank(this.tagName) || StringUtils.isNotBlank(getClasses()));
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
        this.format = format;
    }

    public Format getFormatter(Object value) {
        if (formatter == null && format != null) {
            formatter = CpnlElFunctions.getFormatter(getLocale(),
                    this.i18n ? I18N.get(this.request, format) : format,
                    value != null ? value.getClass() : null);
        }
        return formatter;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        if (locale == null) {
            locale = request.getLocale();
        }
        return locale;
    }

    /**
     * @param tagClass the tagClass to set
     */
    public void setTagClass(String tagClass) {
        setClasses(tagClass);
    }
}
