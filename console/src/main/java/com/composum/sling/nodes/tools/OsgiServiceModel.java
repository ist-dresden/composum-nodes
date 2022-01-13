package com.composum.sling.nodes.tools;

import org.jetbrains.annotations.NotNull;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OsgiServiceModel {

    protected final ServiceReference<?> service;
    protected final Map<String, Object> properties = new TreeMap<>();

    protected List<String> objectClass = new ArrayList<>();
    protected int componentId;
    protected String componentName;
    protected int serviceId;
    protected String servicePid;
    protected int bundleId;
    protected String description;

    public OsgiServiceModel(@NotNull final ServiceReference<?> service) {
        this.service = service;
        for (final String key : service.getPropertyKeys()) {
            final Object value = service.getProperty(key);
            if (value != null) {
                switch (key) {
                    case "objectClass":
                        objectClass.addAll(Arrays.asList((String[]) value));
                        break;
                    case "component.id":
                        componentId = Integer.parseInt(value.toString());
                        break;
                    case "component.name":
                        componentName = value.toString();
                        break;
                    case "service.id":
                        serviceId = Integer.parseInt(value.toString());
                        break;
                    case "service.pid":
                        servicePid = value.toString();
                        break;
                    case "service.bundleid":
                        bundleId = Integer.parseInt(value.toString());
                        break;
                    case "service.description":
                        description = value.toString();
                        break;
                    case "service.scope":
                        break;
                    default:
                        properties.put(key, value);
                        break;
                }
            }
        }
    }

    public List<String> getObjectClass() {
        return objectClass;
    }

    public int getComponentId() {
        return componentId;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getServicePid() {
        return servicePid;
    }

    public int getBundleId() {
        return bundleId;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
