package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static com.composum.sling.nodes.console.ConsolePage.SERVICE_KEY;
import static com.composum.sling.nodes.console.Consoles.PN_CONSOLE_ID;

@Restricted(key = SERVICE_KEY)
public class ConsoleModel extends ConsolePage {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleModel.class);

    protected Consoles.Console console;

    public ConsoleModel(BeanContext context, Consoles.Console console) {
        this.console = console;
        initialize(context);
    }

    public ConsoleModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ConsoleModel(BeanContext context) {
        super(context);
    }

    public ConsoleModel() {
        super();
    }

    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
        if (console == null) {
            final Consoles consoles = Consoles.getInstance(context);
            console = consoles.getConsole(resource.getValueMap().get(PN_CONSOLE_ID, resource.getName()));
        }
    }

    @NotNull
    public String getId() {
        return console != null ? console.getId() : getName();
    }

    @NotNull
    public String getName() {
        return console != null ? console.getName() : super.getName();
    }

    @NotNull
    public String getPath() {
        return console != null ? console.getPath() : super.getPath();
    }

    @NotNull
    public String getLabel() {
        return console != null ? console.getLabel() : getName();
    }

    public boolean isSupportsPermissions() {
        return console != null && console.supportsPermissions();
    }

    @NotNull
    public String getDescription() {
        return console != null ? console.getDescription() : "";
    }

    @NotNull
    public String getContentSrc() {
        return console != null ? console.getContentSrc() : "";
    }

    @NotNull
    public String getUrl() {
        return console != null ? console.getUrl(context.getRequest()) : "#";
    }

    @NotNull
    public String getLinkAttributes() {
        return console != null ? console.getLinkAttributes() : "";
    }

    public boolean isMenu() {
        return console != null && console.isMenu();
    }

    public boolean isValidMenu() {
        return isMenu() && getMenuItems().size() > 0;
    }

    @NotNull
    public Collection<ConsoleModel> getMenuItems() {
        return console != null ? console.getMenuItems(context) : Collections.emptyList();
    }

    @Override
    @NotNull
    public String toString() {
        return console != null ? console.toString(context) : "";
    }

    @NotNull
    public String getDataSet() {
        return Base64.encodeBase64String(Consoles.getInstance(context).toString(context).getBytes(StandardCharsets.UTF_8));
    }
}
