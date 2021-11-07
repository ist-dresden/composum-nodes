package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.CoreConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Session;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class ConsolesModel extends ConsolePage {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolesModel.class);

    public ConsolesModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsolesModel(BeanContext context) {
        super(context);
    }

    public ConsolesModel() {
        super();
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
        return service != null ? service.getLogoutUrl() : null;
    }

    @Nonnull
    public String getWorkspaceName() {
        return getSession().getWorkspace().getName();
    }

    @Nonnull
    public Collection<ConsoleModel> getConsoles() {
        return getConfig().getConsoles(context);
    }

    @Nullable
    public ConsoleModel getConsole(@Nonnull final String name) {
        return getConfig().getConsole(context, name);
    }

    @Nonnull
    public Consoles getConfig() {
        return Consoles.getInstance(context);
    }

    @Nonnull
    public String getDataSet() {
        return Base64.encodeBase64String(toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Nonnull
    public String toString() {
        return getConfig().toString(context);
    }
}
