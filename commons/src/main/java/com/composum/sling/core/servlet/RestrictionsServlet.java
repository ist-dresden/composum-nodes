package com.composum.sling.core.servlet;

import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.service.ServiceRestrictions.Permission;
import com.composum.sling.core.util.RequestUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

import static com.composum.sling.core.service.ServiceRestrictions.Permission.read;
import static com.composum.sling.core.service.ServiceRestrictions.Permission.write;
import static com.composum.sling.core.service.impl.ServiceRestrictionsImpl.SA_PERMISSION;

/**
 *
 */
@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Service Restrictions Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + RestrictionsServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
        }
)
public class RestrictionsServlet extends SlingAllMethodsServlet implements RestrictedService {

    private static final Logger LOG = LoggerFactory.getLogger(RestrictionsServlet.class);

    public static final String SERVICE_KEY = "core/commons/restrictions";

    public static final String SERVLET_PATH = "/bin/cpm/core/restrictions";

    @Reference
    private ServiceRestrictions restrictions;

    @Override
    @NotNull
    public ServiceRestrictions.Key getServiceKey() {
        return new ServiceRestrictions.Key(SERVICE_KEY);
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws IOException {
        final Status status = new Status(request, response);
        final ServiceRestrictions.Key key = new ServiceRestrictions.Key(
                RequestUtil.getParameter(request, "key", request.getRequestPathInfo().getSuffix()));
        final Permission check = RequestUtil.getParameter(request, "check",
                RequestUtil.getSelector(request, read));
        Map<String, Object> data = status.data("result");
        data.put("service", key.toString());
        data.put("check", check);
        data.put("permissible", restrictions.isPermissible(request, key, check));
        data.put("permission", restrictions.getPermission(key));
        data.put("restrictions", restrictions.getRestrictions(key));
        status.sendJson();
    }

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response)
            throws IOException {
        final Status status = new Status(request, response);
        HttpSession session = request.getSession(true);
        Permission user = null;
        final Permission system = restrictions.getDefaultPermisson();
        user = RequestUtil.getParameter(request, "permission",
                RequestUtil.getSelector(request, Permission.none));
        if (user == Permission.none) {
            try {
                user = (Permission) session.getAttribute(SA_PERMISSION);
            } catch (ClassCastException ignore) {
                user = null;
            }
            if (user == null) {
                user = system == write ? read : write;
            } else {
                user = user == write ? null : write;
            }
            if (user != null && (user == system || !restrictions.isUserOptionAllowed(request, user))) {
                user = null;
            }
        }
        if (user != null) {
            session.setAttribute(SA_PERMISSION, user);
        } else {
            session.removeAttribute(SA_PERMISSION);
        }
        Map<String, Object> data = status.data("result");
        data.put("user", user);
        data.put("system", system);
        status.sendJson();
    }
}
