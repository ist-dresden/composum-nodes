package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingBean;
import com.composum.sling.core.bean.BeanFactory;
import com.composum.sling.core.bean.SlingBeanFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * a tag to instantiate a bean or model object
 */
public class ComponentTag extends CpnlBodyTagSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentTag.class);

    protected String var;
    protected String type;
    protected Integer varScope;
    protected Boolean replace;

    protected SlingBean component;
    private transient Class<? extends SlingBean> componentType;
    private static final Map<Class<? extends SlingBean>, Field[]> fieldCache = new ConcurrentHashMap<>();

    protected ArrayList<Map<String, Object>> replacedAttributes;
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
        replacedAttributes = null;
        componentType = null;
        super.clear();
    }

    @Override
    public int doStartTag() throws JspException {
        super.doStartTag();
        if (getVar() != null) {
            try {
                if (available() == null || getReplace()) {
                    component = createComponent();
                    setAttribute(getVar(), component, getVarScope());
                }
            } catch (ClassNotFoundException ex) {
                LOG.error("Class not found: " + this.type, ex);
            } catch (IllegalAccessException ex) {
                LOG.error("Could not access: " + this.type, ex);
            } catch (InstantiationException ex) {
                LOG.error("Could not instantiate: " + this.type, ex);
            } catch (IllegalArgumentException ex) {
                LOG.error("Could not adapt to: " + this.type, ex);
            }
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        restoreAttributes();
        clear();
        super.doEndTag();
        return EVAL_PAGE;
    }

    /**
     * Configure an var / variable name to store the component in the context
     */
    @Override
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
        varScope = key != null ? SCOPES.get(key.toLowerCase()) : null;
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
    public void setReplace(Boolean flag) {
        this.replace = flag;
    }

    public Boolean getReplace() {
        return replace != null ? replace : (getVarScope() == PageContext.PAGE_SCOPE);
    }

    /**
     * get the content type class object
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends SlingBean> getComponentType() throws ClassNotFoundException {
        if (componentType == null) {
            String type = getType();
            if (StringUtils.isNotBlank(type)) {
                componentType = (Class<? extends SlingBean>) context.getType(type);
            }
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
            BeanFactory factoryRule = type.getAnnotation(BeanFactory.class);
            Resource modelResource = getModelResource(context);
            if (factoryRule != null) {
                SlingBeanFactory factory = context.getService(factoryRule.serviceClass());
                if (factory != null) {
                    return factory.createBean(context, modelResource, type);
                }
            }
            BeanContext baseContext = context.withResource(modelResource);
            component = baseContext.adaptTo(type);
            if (component == null) {
                throw new IllegalArgumentException("Could not adapt " + modelResource + " to " + type);
            }
            injectServices(component);
            additionalInitialization(component);
        }
        return component;
    }

    /**
     * Hook that can change the resource used for {@link #createComponent()} if necessary. This implementation just uses
     * the resource from the {@link #context} ( {@link BeanContext#getResource()} ).
     */
    public Resource getModelResource(BeanContext context) {
        return context.getResource();
    }

    /**
     * Hook for perform additional initialization of the component. When called, the fields of the component are already
     * initialized with Sling-Models or {@link SlingBean#initialize(BeanContext)} / {@link
     * SlingBean#initialize(BeanContext, Resource)}.
     */
    protected void additionalInitialization(SlingBean component) {
        // empty
    }

    /**
     * Inject OSGI services for attributes marked for injection in a new component instance, if not already
     * initialized e.g. by Sling-Models.
     */
    protected void injectServices(SlingBean component) throws IllegalAccessException {
        final Field[] declaredFields;
        if (fieldCache.containsKey(component.getClass())) {
            declaredFields = fieldCache.get(component.getClass());
        } else {
            declaredFields = component.getClass().getDeclaredFields();
            fieldCache.put(component.getClass(), declaredFields);
        }
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (null == field.get(component)) { // if not initialized already by Sling-Models
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
    }

    /**
     *
     */
    protected <T> T retrieveFirstServiceOfType(Class<T> serviceType, String filter) {
        T[] services = null;
        try {
            services = context.getServices(serviceType, filter);
        } catch (InvalidSyntaxException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return services == null ? null : services[0];
    }

    // attribute replacement registry...

    /**
     * retrieves the registry for one scope
     */
    protected Map<String, Object> getReplacedAttributes(int scope) {
        if (replacedAttributes == null) {
            replacedAttributes = new ArrayList<>();
        }
        while (replacedAttributes.size() <= scope) {
            replacedAttributes.add(new HashMap<String, Object>());
        }
        return replacedAttributes.get(scope);
    }

    /**
     * each attribute set by a tag should use this method for attribute declaration;
     * an existing value with the same key is registered and restored if the tag rendering ends
     */
    protected void setAttribute(String key, Object value, int scope) {
        Map<String, Object> replacedInScope = getReplacedAttributes(scope);
        if (!replacedInScope.containsKey(key)) {
            Object current = pageContext.getAttribute(key, scope);
            replacedInScope.put(key, current);
        }
        pageContext.setAttribute(key, value, scope);
    }

    /**
     * restores all replaced values and removes all attributes declared in this tag
     */
    protected void restoreAttributes() {
        if (replacedAttributes != null) {
            for (int scope = 0; scope < replacedAttributes.size(); scope++) {
                Map<String, Object> replaced = replacedAttributes.get(scope);
                for (Map.Entry<String, Object> entry : replaced.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        pageContext.setAttribute(key, value, scope);
                    } else {
                        pageContext.removeAttribute(key, scope);
                    }
                }
            }
        }
    }
}
