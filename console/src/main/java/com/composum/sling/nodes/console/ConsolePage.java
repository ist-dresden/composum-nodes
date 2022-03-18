package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import static com.composum.sling.core.service.ServiceRestrictions.Permission.none;
import static com.composum.sling.nodes.console.ConsolePage.SERVICE_KEY;

@Restricted(key = SERVICE_KEY)
public class ConsolePage extends ConsoleSlingBean {

    public static final String SERVICE_KEY = "nodes/console/view";

    private static final Logger LOG = LoggerFactory.getLogger(ConsolePage.class);

    public ConsolePage(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsolePage(BeanContext context) {
        super(context);
    }

    public ConsolePage() {
        super();
    }

    public String getURL(String path) {
        return LinkUtil.getUrl(getRequest(), path);
    }

    //
    // restrictions
    //

    public ServiceRestrictions.Permission getUserPermission() {
        ServiceRestrictions.Permission permission = none;
        HttpSession session = getRequest().getSession(false);
        if (session != null) {
            Object attribute = session.getAttribute(ServiceRestrictions.SA_PERMISSION);
            if (attribute instanceof ServiceRestrictions.Permission) {
                permission = (ServiceRestrictions.Permission) attribute;
            }
        }
        return permission;
    }

    public ServiceRestrictions.Permission getSystemPermission() {
        return context.getService(ServiceRestrictions.class).getDefaultPermisson();
    }

    //
    // workspace, user and profile
    //

    public String getCurrentUser() {
        Session session = getSession();
        return session.getUserID();
    }

    public String getLogoutUrl() {
        CoreConfiguration service = this.context.getService(CoreConfiguration.class);
        return service != null ? service.getLogoutUrl(service.getLoggedoutUrl()) : null;
    }

    @NotNull
    public String getWorkspaceName() {
        return getSession().getWorkspace().getName();
    }
}
