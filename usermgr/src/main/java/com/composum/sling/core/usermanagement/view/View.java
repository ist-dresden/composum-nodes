package com.composum.sling.core.usermanagement.view;

import com.composum.sling.core.Restricted;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.usermanagement.core.UserManagementServlet;
import com.composum.sling.core.usermanagement.model.AuthorizableModel;
import com.composum.sling.core.usermanagement.model.AuthorizablesView;
import com.composum.sling.core.usermanagement.model.UserModel;
import com.composum.sling.core.usermanagement.service.AuthorizableWrapper;
import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
@Restricted(key = UserManagementServlet.SERVICE_KEY)
public class View extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(View.class);

    public static final String RA_VIEW_BEAN = View.class.getName() + "#view";

    private transient AuthorizablesView view;
    private transient AuthorizableModel model;

    public @NotNull AuthorizablesView getView() {
        if (this.view == null) {
            SlingHttpServletRequest request = getRequest();
            this.view = (AuthorizablesView) request.getAttribute(RA_VIEW_BEAN);
            if (this.view == null) {
                try {
                    final String path = getSuffix();
                    if (StringUtils.isNotBlank(path)) {
                        final Authorizables authorizables = context.getService(Authorizables.class);
                        this.view = new AuthorizablesView(
                                new Authorizables.Context(authorizables, request, getResponse()),
                                getSelector(), null, getPathFilter(path));
                        request.setAttribute(RA_VIEW_BEAN, this.view);
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return this.view;
    }

    protected @Nullable Class<? extends AuthorizableWrapper> getSelector() {
        return null;
    }

    protected @Nullable Authorizables.Filter.Path getPathFilter(@Nullable final String path) {
        return StringUtils.isNotBlank(path) ? new Authorizables.Filter.Path("^" + path + "$") : null;
    }

    public @Nullable AuthorizableModel getModel() {
        if (this.model == null) {
            this.model = getView().getSingleFocus();
        }
        return this.model;
    }

    public @NotNull String getViewType() {
        final AuthorizableModel model = getModel();
        String viewType = model != null ? model.getType() : "blank";
        return ("user".equals(viewType) && ((UserModel) model).isSystemUser()) ? "system" : viewType;
    }

    public @NotNull String getId() {
        final AuthorizableModel model = getModel();
        return model != null ? model.getId() : "";
    }

    public @NotNull String getPath() {
        final AuthorizableModel model = getModel();
        return model != null ? model.getPath() : "";
    }

    public @NotNull Set<String> getDeclaredMemberOf() {
        final AuthorizableModel model = getModel();
        return model != null ? model.getDeclaredMemberOf() : Collections.emptySet();
    }

    public @Nullable String getSuffix() {
        final String suffix = getRequest().getRequestPathInfo().getSuffix();
        return suffix != null ? XSS.filter(suffix) : null;
    }

    public @NotNull String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "general";
    }
}
