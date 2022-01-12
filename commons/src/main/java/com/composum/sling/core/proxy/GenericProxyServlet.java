package com.composum.sling.core.proxy;

import com.composum.sling.core.Restricted;
import com.composum.sling.core.util.XSS;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * the generic proxy servlet delegates proxy requests to the collected generic proxy service implementations
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Generic Proxy Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/proxy",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=fwd",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
@Restricted(key = GenericProxyServlet.SERVICE_KEY)
public class GenericProxyServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(GenericProxyServlet.class);

    public static final String SERVICE_KEY = "core/proxy/generic";

    public static final Pattern EXTERNAL_SUFFIX = Pattern.compile("^/https?://", Pattern.CASE_INSENSITIVE);

    protected List<GenericProxyService> instances = Collections.synchronizedList(new ArrayList<>());

    @Reference(service = GenericProxyService.class, policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE)
    protected void addProxyService(@NotNull final GenericProxyService service) {
        LOG.info("addProxyService: {}", service.getName());
        instances.add(service);
    }

    protected void removeProxyService(@NotNull final GenericProxyService service) {
        LOG.info("removeProxyService: {}", service.getName());
        instances.remove(service);
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws IOException {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String targetSuffix = XSS.filter(pathInfo.getSuffix());
        if (StringUtils.isNotBlank(targetSuffix)) {
            // proxy traget URL: 'suffix' + '?' + 'query string' of the proxy request
            String targetUrl = EXTERNAL_SUFFIX.matcher(targetSuffix).find()
                    ? targetSuffix.substring(1) : targetSuffix;
            String queryString = request.getQueryString();
            if (StringUtils.isNotBlank(queryString)) {
                targetUrl += "?" + queryString;
            }
            for (GenericProxyService service : instances) {
                if (service.doProxy(request, response, targetUrl)) {
                    return; // the first service which has handled the request terminates the servlets request handling
                }
            }
        }
        // send 404 if no service can handle the request
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
