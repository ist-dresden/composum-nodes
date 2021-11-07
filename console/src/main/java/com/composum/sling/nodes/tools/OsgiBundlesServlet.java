package com.composum.sling.nodes.tools;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Tools Bundles Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + OsgiBundlesServlet.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        }
)
public class OsgiBundlesServlet extends SlingSafeMethodsServlet {

    public static final String RESOURCE_TYPE = "composum/nodes/system/tools/osgi/bundles";

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
                         @Nonnull final SlingHttpServletResponse response) throws IOException {
        final BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
        final JsonWriter writer = ResponseUtil.getJsonWriter(response);
        final OsgiBundleModel bundle = new OsgiBundleModel(context);
        if (bundle.isValid()) {
            writeBundle(context, writer, bundle);
        } else {
            writeBundles(context, writer);
        }
    }

    protected void writeBundle(@Nonnull final BeanContext context, @Nonnull final JsonWriter writer,
                               @Nonnull final OsgiBundleModel model)
            throws IOException {
        writer.beginObject();
        writeProperties(writer, model);
        writer.name("more").beginObject();
        for (final Map.Entry<String, String> entry : model.getHeaders()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
        writer.name("exported").beginArray();
        for (final OsgiBundleModel.Exported exported : model.getExported()) {
            writer.beginObject();
            writer.name("symbolicName").value(exported.symbolicName);
            if (exported.version != null) {
                writer.name("version").value(exported.version.toString());
            }
            writer.endObject();
        }
        writer.endArray();
        writer.name("imported").beginArray();
        for (final OsgiBundleModel.Imported imported : model.getImported()) {
            writer.beginObject();
            writer.name("symbolicName").value(imported.symbolicName);
            if (imported.resolved != null) {
                if (imported.version != null) {
                    writer.name("range").value(imported.version.toString());
                }
                if (imported.resolved.version != null) {
                    writer.name("version").value(imported.resolved.version.toString());
                }
                writer.name("bundle").value(imported.resolved.bundle.getBundleId());
                writer.name("active").value(imported.resolved.active);
            } else {
                if (imported.version != null) {
                    writer.name("range").value(imported.version.toString());
                }
            }
            writer.name("resolved").value(imported.resolved != null);
            if (imported.optional) {
                writer.name("optional").value(true);
            }
            writer.endObject();
        }
        writer.endArray();
        writer.name("provided");
        writeServices(writer, model.getProvidedServices());
        writer.name("used");
        writeServices(writer, model.getUsedServices());
        writer.name("headers").beginObject();
        final Dictionary<String, String> headers = model.bundle.getHeaders();
        for (final Enumeration<String> keys = headers.keys(); keys.hasMoreElements(); ) {
            final String key = keys.nextElement();
            writer.name(key).value(headers.get(key).replaceAll("([,;])([^ )\\]0-9])", "$1 $2"));
        }
        writer.endObject();
        writer.endObject();
    }

    protected void writeBundles(@Nonnull final BeanContext context, @Nonnull final JsonWriter writer)
            throws IOException {
        final OsgiBundlesModel model = new OsgiBundlesModel(context);
        final Collection<OsgiBundleModel> bundles = model.getBundles();
        writer.beginObject();
        writer.name("total").value(model.getCountTotal());
        writer.name("active").value(model.getCountActive());
        writer.name("bundles").beginArray();
        for (final OsgiBundleModel bundle : bundles) {
            writer.beginObject();
            writeProperties(writer, bundle);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    protected void writeServices(@Nonnull final JsonWriter writer,
                                 @Nonnull final Iterator<OsgiBundleModel.ServiceModel> iterator)
            throws IOException {
        writer.beginArray();
        while (iterator.hasNext()) {
            OsgiBundleModel.ServiceModel service = iterator.next();
            writer.beginObject();
            writer.name("id").value(service.serviceId);
            if (service.description != null) {
                writer.name("description").value(service.description);
            }
            writer.name("short").beginArray();
            for (String type : service.objectClass) {
                writer.value(StringUtils.substringAfterLast(type, "."));
            }
            writer.endArray();
            writer.name("classes").beginArray();
            for (String type : service.objectClass) {
                writer.value(type);
            }
            writer.endArray();
            if (service.servicePid != null) {
                writer.name("servicePid").value(service.servicePid);
            }
            writer.name("bundle").value(service.bundleId);
            if (service.componentName != null) {
                writer.name("component").beginObject();
                writer.name("id").value(service.componentId);
                writer.name("name").value(service.componentName);
                writer.endObject();
            }
            writer.name("properties").beginObject();
            for (final Map.Entry<String, Object> entry : service.getProperties().entrySet()) {
                writer.name(entry.getKey());
                final Object value = entry.getValue();
                if (value instanceof Object[]) {
                    writer.beginArray();
                    for (Object val : (Object[]) value) {
                        writer.value(val != null ? val.toString() : null);
                    }
                    writer.endArray();
                } else {
                    writer.value(value != null ? value.toString() : null);
                }
            }
            writer.endObject();
            writer.endObject();
        }
        writer.endArray();
    }

    protected void writeProperties(@Nonnull final JsonWriter writer,
                                   @Nonnull final OsgiBundleModel model) throws IOException {
        writer.name("id").value(model.getBundleId());
        writer.name("name").value(model.getName());
        writer.name("symbolicName").value(model.getSymbolicName());
        writer.name("location").value(model.getLocation());
        writer.name("version").value(model.getVersion());
        writer.name("lastModified").value(model.getLastModified());
        writer.name("category").value(model.getCategory());
        writer.name("state").value(model.getState().name());
        writer.name("active").value(model.isActive());
    }
}
