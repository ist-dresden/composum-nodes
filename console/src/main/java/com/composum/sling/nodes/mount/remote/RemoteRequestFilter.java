package com.composum.sling.nodes.mount.remote;

import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        service = {Filter.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Remote Load Filter",
                "sling.filter.scope=REQUEST",
                "service.ranking:Integer=" + 4000
        },
        immediate = true
)
public class RemoteRequestFilter implements Filter {

    protected Map<String, RemoteProvider> remoteProviders = new ConcurrentHashMap<>();

    @Reference(
            service = RemoteProvider.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void addRemoteProvider(@NotNull RemoteProvider provider) {
        remoteProviders.put(provider.getProviderRoot(), provider);
    }

    protected void removeRemoteProvider(@NotNull RemoteProvider provider) {
        remoteProviders.remove(provider.getProviderRoot());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        if (servletRequest instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            if ("json".equals(pathInfo.getExtension())) {
                String path = pathInfo.getResourcePath();
                for (String remoteRoot : remoteProviders.keySet()) {
                    if (path.equals(remoteRoot) || path.startsWith(remoteRoot + "/")) {
                        // send a 'terminating' response if a recusive remote request is detected
                        SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;
                        response.setContentType("application/json;charset=UTF-8");
                        JsonWriter writer = new JsonWriter(response.getWriter());
                        writer.beginObject();
                        writer.name(JcrConstants.JCR_PRIMARYTYPE).value(JcrConstants.NT_UNSTRUCTURED);
                        writer.endObject();
                        return;
                    }
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
