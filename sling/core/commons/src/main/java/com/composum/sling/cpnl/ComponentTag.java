package com.composum.sling.cpnl;

import com.composum.sling.core.AbstractSlingBean;
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

    private String id;
    private String type;
    private int scope = PageContext.PAGE_SCOPE;
    private Boolean replace = null;

    private AbstractSlingBean component = null;
    private Object replacedValue = null;

    private static Map<Class<? extends AbstractSlingBean>, Field[]> fieldCache = new ConcurrentHashMap<Class<? extends AbstractSlingBean>, Field[]>();

    private transient Class<? extends AbstractSlingBean> componentType;

    public static final Map<String, Integer> SCOPES = new HashMap<>();

    static {
        SCOPES.put("page", PageContext.PAGE_SCOPE);
        SCOPES.put("request", PageContext.REQUEST_SCOPE);
        SCOPES.put("session", PageContext.SESSION_SCOPE);
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        if (replace == null) {
            replace = (scope == PageContext.PAGE_SCOPE);
        }
        try {
            if ((replacedValue = available()) == null || replace) {
                component = createComponent();
                pageContext.setAttribute(this.id, component, this.scope);
            }
        } catch (ClassNotFoundException ex) {
            log.error("Class not found: " + this.type, ex);
        } catch (IllegalAccessException ex) {
            log.error("Could not access: " + this.type, ex);
        } catch (InstantiationException ex) {
            log.error("Could not instantiate: " + this.type, ex);
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        if (replacedValue != null) {
            if (component != null) {
                pageContext.setAttribute(this.id, replacedValue, this.scope);
            }
        } else {
            pageContext.removeAttribute(this.id, this.scope);
        }
        super.doEndTag();
        return EVAL_PAGE;
    }

    /**
     * Configure an id / variable name to store the component in the context
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Component class to instantiate (full notation as in Class.name)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Determine the scope (<code>page</code>, <code>request</code> or <code>session</code>)
     * for the component instance attribute
     */
    public void setScope(String key) {
        Integer value = key != null ? SCOPES.get(key.toLowerCase()) : null;
        this.scope = value != null ? value : PageContext.PAGE_SCOPE;
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

    /**
     * get the content type class object
     */
    protected Class<? extends AbstractSlingBean> getComponentType() throws ClassNotFoundException {
        if (componentType == null) {
            componentType = (Class<? extends AbstractSlingBean>) sling.getType(this.type);
        }
        return componentType;
    }

    /**
     * Check for an existing instance of the same id and assignable type
     */
    protected Object available() throws ClassNotFoundException {
        Object result = null;
        Object value = pageContext.getAttribute(this.id, this.scope);
        if (value instanceof AbstractSlingBean) {
            Class<?> type = getComponentType();
            if (type != null && type.isAssignableFrom(value.getClass())) {
                result = value;
            }
        }
        return result;
    }

    /**
     * Create the requested component instance
     */
    protected AbstractSlingBean createComponent() throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        AbstractSlingBean component = null;
        Class<? extends AbstractSlingBean> type = getComponentType();
        if (type != null) {
            component = type.newInstance();
            component.initialize(new BeanContext.Page(pageContext));
            injectFieldDependecies(component);
        }
        return component;
    }

    /**
     * define attributes marked for injection in a new component instance
     */
    protected void injectFieldDependecies(AbstractSlingBean component) throws IllegalAccessException {
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
