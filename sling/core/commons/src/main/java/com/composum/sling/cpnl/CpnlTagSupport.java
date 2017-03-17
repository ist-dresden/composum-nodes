package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.jsp.util.TagUtil;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;


@SuppressWarnings("serial")
public class CpnlTagSupport extends TagSupport {

    protected SlingHttpServletRequest request;
    protected BeanContext context;
    protected JspWriter out;

    protected Resource resource;
    protected ResourceResolver resourceResolver;

    private transient JspApplicationContext jspAppContext;
    private transient ExpressionFactory expressionFactory;
    private transient ELContext elContext;

    /**
     * Reset all member variables to the (default) start values. Called prior
     * processing the tag and at release time.
     */
    protected void clear() {
        resource = null;
        resourceResolver = null;
        context = null;
        out = null;
        request = null;
        elContext = null;
        expressionFactory = null;
        jspAppContext = null;
    }

    @Override
    public int doStartTag() throws JspException {
        context = createContext(pageContext);
        out = pageContext.getOut();
        request = TagUtil.getRequest(pageContext);
        resourceResolver = request.getResourceResolver();
        resource = request.getResource();

        return super.doStartTag();
    }

    protected BeanContext createContext(PageContext pageContext) {
        return new BeanContext.Page(pageContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.servlet.jsp.tagext.TagSupport#setPageContext(javax.servlet.jsp.
     * PageContext)
     */
    @Override
    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);
        clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release() {
        clear();
        super.release();
    }

    protected JspApplicationContext getJspAppContext() {
        if (jspAppContext == null) {
            ServletContext servletContext = pageContext.getServletContext();
            jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext(servletContext);
        }
        return jspAppContext;
    }

    protected ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = getJspAppContext().getExpressionFactory();
        }
        return expressionFactory;
    }

    protected ELContext getELContext() {
        if (elContext == null) {
            elContext = pageContext.getELContext();
        }
        return elContext;
    }

    protected ValueExpression createValueExpression(ELContext elContext, String expression, Class<?> type) {
        return getExpressionFactory().createValueExpression(elContext, expression, type);
    }

    /**
     * evaluate an EL expression value, the value can contain @{..} expression rules which are transformed to ${..}
     */
    protected <T> T eval(Object value, T defaultValue) {
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
