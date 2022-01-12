package com.composum.sling.nodes.tools;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Tools Bundles Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + SettingsServlet.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        }
)
public class SettingsServlet extends SlingSafeMethodsServlet implements RestrictedService {

    public static final String RESOURCE_TYPE = "composum/nodes/system/tools/runtime/settings";

    public static final String SERVICE_KEY = "system/runtime/settings";

    public static final String HTTP_SERVICE = "org.osgi.service.http.HttpService";

    @Reference
    protected SlingSettingsService slingSettingsService;

    @Reference
    protected ServiceRestrictions restrictions;

    private ServiceRestrictions.Permission permission;
    private boolean enabled = false;

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        permission = restrictions.getPermission(getServiceKey());
        enabled = permission != ServiceRestrictions.Permission.none;
    }

    @Override
    @NotNull
    public ServiceRestrictions.Key getServiceKey() {
        return new ServiceRestrictions.Key(SERVICE_KEY);
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response) throws IOException {
        if (enabled) {
            final BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
            final JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginArray();
            final String[] selectors = request.getRequestPathInfo().getSelectors();
            switch (selectors.length > 0 ? selectors[0] : "") {
                case "sling":
                    writeProperties(writer, slingSettings());
                    break;
                case "http":
                    writeProperties(writer, httpSettings());
                    break;
                case "system":
                default:
                    writeProperties(writer, systemSettings());
                    break;
            }
            writer.endArray();
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    protected Map<String, Object> slingSettings() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("Id", slingSettingsService.getSlingId());
        values.put("Name", slingSettingsService.getSlingName());
        values.put("Description", slingSettingsService.getSlingDescription());
        values.put("Home", slingSettingsService.getSlingHome());
        values.put("Home Path", slingSettingsService.getSlingHomePath());
        values.put("Run Modes", slingSettingsService.getRunModes());
        return values;
    }

    protected Map<String, Object> httpSettings() {
        final Map<String, Object> values = new TreeMap<>();
        final ServiceReference<?> reference = bundleContext.getServiceReference(HTTP_SERVICE);
        if (reference != null) {
            final OsgiServiceModel service = new OsgiServiceModel(reference);
            values.put("Service Id", service.getServiceId());
            values.put("Service Pid", service.getServicePid());
            values.put("Bundle Id", service.getBundleId());
            values.put("Component Id", service.getComponentId());
            values.put("Component Name", service.getComponentName());
            values.put("Description", service.getDescription());
            values.put("Object Class", service.getObjectClass());
            values.putAll(service.getProperties());
        }
        return values;
    }

    protected Map<String, Object> systemSettings() {
        final Map<String, Object> values = new TreeMap<>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            values.put(entry.getKey().toString(), entry.getValue());
        }
        return values;
    }

    protected void writeProperties(@NotNull final JsonWriter writer,
                                   @NotNull final Map<String, Object> values) throws IOException {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            final Object value = entry.getValue();
            if (value != null) {
                writer.beginObject();
                writer.name("key").value(entry.getKey());
                writer.name("value").value((value instanceof String[] ? Arrays.asList((String[]) value) : value).toString());
                writer.endObject();
            }
        }
    }
}
