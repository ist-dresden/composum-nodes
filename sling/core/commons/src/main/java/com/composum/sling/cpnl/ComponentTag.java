package com.composum.sling.cpnl;

import com.composum.sling.core.SlingBean;
import com.composum.sling.core.BeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * a tag to instantiate a bean or model object
 */
public class ComponentTag extends CpnlBodyTagSupport {

    private static final Logger log = LoggerFactory.getLogger(ComponentTag.class);

    private String var;
    private String type;
    private Integer varScope;
    private Boolean replace;

    protected BeanContext context;
    protected SlingBean component;
    protected Object replacedValue;

    private transient Class<? extends SlingBean> componentType;

    private static Map<Class<? extends SlingBean>, Field[]> fieldCache = new ConcurrentHashMap<Class<? extends SlingBean>, Field[]>();

    public static final Map<String, Integer> SCOPES = new HashMap<>();

    static {
        SCOPES.put("page", PageContext.PAGE_SCOPE);
        SCOPES.put("request", PageContext.REQUEST_SCOPE);
        SCOPES.put("session", PageContext.SESSION_SCOPE);
    }

    @Override
    protected void clear() {
        var = null;
        type = null;
        varScope = null;
        replace = null;
        component = null;
        replacedValue = null;
        componentType = null;
        super.clear();
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        context = new BeanContext.Page(pageContext);
        if (getVar() != null) {
            try {
                if ((replacedValue = available()) == null || getReplace()) {
                    component = createComponent();
                    pageContext.setAttribute(getVar(), component, getVarScope());
                }
            } catch (ClassNotFoundException ex) {
                log.error("Class not found: " + this.type, ex);
            } catch (IllegalAccessException ex) {
                log.error("Could not access: " + this.type, ex);
            } catch (InstantiationException ex) {
                log.error("Could not instantiate: " + this.type, ex);
            }
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        if (getVar() != null) {
            if (replacedValue != null) {
                if (component != null) {
                    pageContext.setAttribute(getVar(), replacedValue, getVarScope());
                }
            } else {
                pageContext.removeAttribute(getVar(), getVarScope());
            }
        }
        clear();
        super.doEndTag();
        return EVAL_PAGE;
    }

    /**
     * Configure an var / variable name to store the component in the context
     */
    public void setId(String id) {
        setVar(id);
    }

    /**
     * Configure an var / variable name to store the component in the context
     */
    public void setVar(String id) {
        this.var = id;
    }

    public String getVar() {
        return this.var;
    }

    /**
     * Component class to instantiate (full notation as in Class.name)
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Determine the varScope (<code>page</code>, <code>request</code> or <code>session</code>)
     * for the component instance attribute
     */
    public void setScope(String key) {
        Integer value = key != null ? SCOPES.get(key.toLowerCase()) : null;
        varScope = value != null ? value : null;
    }

    public void setVarScope(Integer value) {
        varScope = value;
    }

    public Integer getVarScope() {
        return varScope != null ? varScope : PageContext.PAGE_SCOPE;
    }

    /**
     * Determine the reuse policy if an appropriate instance is already existing.
     *
     * @param flag <code>false</code> - (re)use an appropriate available instance;
     *             <code>true</code> - replace each potentially existing instance
     *             (default in 'page' context).
     */
    public void setReplace(boolean flag) {
        this.replace = flag;
    }

    public Boolean getReplace() {
        return replace != null ? replace : (getVarScope() == PageContext.PAGE_SCOPE);
    }

    /**
     * get the content type class object
     */
    protected Class<? extends SlingBean> getComponentType() throws ClassNotFoundException {
        if (componentType == null) {
            componentType = (Class<? extends SlingBean>) sling.getType(getType());
        }
        return componentType;
    }

    /**
     * Check for an existing instance of the same var and assignable type
     */
    protected Object available() throws ClassNotFoundException {
        Object result = null;
        if (getVar() != null) {
            Object value = pageContext.getAttribute(getVar(), getVarScope());
            if (value instanceof SlingBean) {
                Class<?> type = getComponentType();
                if (type != null && type.isAssignableFrom(value.getClass())) {
                    result = value;
                }
            }
        }
        return result;
    }

    /**
     * Create the requested component instance
     */
    protected SlingBean createComponent() throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        SlingBean component = null;
        Class<? extends SlingBean> type = getComponentType();
        if (type != null) {
            component = type.newInstance();
            initialize(component);
            injectFieldDependecies(component);
        }
        return component;
    }

    protected void initialize(SlingBean component) {
        component.initialize(new BeanContext.Page(pageContext));
    }

    /**
     * define attributes marked for injection in a new component instance
     */
    protected void injectFieldDependecies(SlingBean component) throws IllegalAccessException {
        final Field[] declaredFields;
        if (fieldCache.containsKey(component.getClass())) {
            declaredFields = fieldCache.get(component.getClass());
        } else {
            declaredFields = component.getClass().getDeclaredFields();
            fieldCache.put(component.getClass(), declaredFields);
        }
        for (Field field : declaredFields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (field.isAnnotationPresent(Inject.class)) {
                String filter = null;
                if (field.isAnnotationPresent(Named.class)) {
                    Named name = field.getAnnotation(Named.class);
                    filter = "(service.pid=" + name.value() + ")";
                }
                Class<?> typeOfField = field.getType();
                Object o = retrieveFirstServiceOfType(typeOfField, filter);
                field.set(component, o);
            }
        }
    }

    /**
     *
     */
    protected <T> T retrieveFirstServiceOfType(Class<T> serviceType, String filter) {
        T[] services = sling.getScriptHelper().getServices(serviceType, filter);
        return services == null ? null : services[0];
    }
}
