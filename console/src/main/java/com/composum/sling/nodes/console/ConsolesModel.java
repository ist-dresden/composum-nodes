package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static com.composum.sling.nodes.console.ConsolePage.SERVICE_KEY;

@Restricted(key = SERVICE_KEY)
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

    @NotNull
    public Collection<ConsoleModel> getConsoles() {
        return getConfig().getConsoles(context);
    }

    @Nullable
    public ConsoleModel getConsole(@NotNull final String name) {
        return getConfig().getConsole(context, name);
    }

    @NotNull
    public Consoles getConfig() {
        return Consoles.getInstance(context);
    }

    @NotNull
    public String getDataSet() {
        return Base64.encodeBase64String(toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @NotNull
    public String toString() {
        return getConfig().toString(context);
    }
}
