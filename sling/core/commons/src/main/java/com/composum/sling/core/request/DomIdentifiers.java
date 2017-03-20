package com.composum.sling.core.request;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * a request attribute implementation to create ids for the DOM elements during component rendering
 */
public class DomIdentifiers {

    public static final String ATTRIBUTE_KEY = DomIdentifiers.class.getName();

    public static synchronized DomIdentifiers getInstance(BeanContext context) {
        SlingHttpServletRequest request = context.getRequest();
        DomIdentifiers manager = (DomIdentifiers) request.getAttribute(ATTRIBUTE_KEY);
        if (manager == null) {
            manager = new DomIdentifiers();
            request.setAttribute(ATTRIBUTE_KEY, manager);
        }
        return manager;
    }

    protected DomIdentifiers() {
    }

    private Integer identifierCount = 0;
    private Map<String, Element> registeredElements = new HashMap<>();

    private synchronized int nextId() {
        return ++identifierCount;
    }

    public class Element {

        public final String id;
        public final String path;
        public final String type;

        public Element(String path, String type) {
            this.path = path;
            this.type = StringUtils.isNotBlank(type) ? type : "any";
            int idValue = nextId();
            this.id = path.substring(path.lastIndexOf('/') + 1) + "-"
                    + type.substring(type.lastIndexOf('/') + 1) + "-" + idValue;
        }
    }

    public String getElementId(SlingBean bean) {
        return getElementId(bean.getPath(), bean.getType());
    }

    public String getElementId(Resource resource) {
        return getElementId(resource.getPath(), resource.getResourceType());
    }

    public String getElementId(String path, String type) {
        return getElement(path, type).id;
    }

    public Element getElement(String path, String type) {
        Element element = registeredElements.get(path);
        if (element == null) {
            element = new Element(path, type);
            registeredElements.put(path, element);
        }
        return element;
    }
}
