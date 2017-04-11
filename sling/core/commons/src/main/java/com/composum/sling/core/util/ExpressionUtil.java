package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

/**
 * Utility to evaluate expressions e.g. in tags. Include ExpressionUtil as lazy getter into tag.
 */
public class ExpressionUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ExpressionUtil.class);

    private transient final PageContext pageContext;

    private transient JspApplicationContext jspAppContext;
    private transient ExpressionFactory expressionFactory;
    private transient ELContext elContext;

    public ExpressionUtil(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    private JspApplicationContext getJspAppContext() {
        if (jspAppContext == null) {
            ServletContext servletContext = pageContext.getServletContext();
            jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext(servletContext);
        }
        return jspAppContext;
    }

    private ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = getJspAppContext().getExpressionFactory();
        }
        return expressionFactory;
    }

    private ELContext getELContext() {
        if (elContext == null) {
            elContext = pageContext.getELContext();
        }
        return elContext;
    }

    private ValueExpression createValueExpression(ELContext elContext, String expression, Class<?> type) {
        return getExpressionFactory().createValueExpression(elContext, expression, type);
    }

    /**
     * evaluate an EL expression value, the value can contain @{..} expression rules which are transformed to ${..}
     */
    public <T> T eval(Object value, T defaultValue) {
        T result = null;
        if (value instanceof String) {
            String expression = (String) value;
            if (StringUtils.isNotBlank(expression)) {
                expression = expression.replaceAll("@\\{([^\\}]+)\\}", "\\${$1}");
                Class type = defaultValue != null ? defaultValue.getClass() : String.class;
                ELContext elContext = getELContext();
                ValueExpression valueExpression = createValueExpression(elContext, expression, type);
                result = (T) valueExpression.getValue(elContext);
            }
        }
        return result != null ? result : defaultValue;
    }

}
