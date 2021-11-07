package com.composum.sling.nodes.console;

import com.composum.sling.core.BeanContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static com.composum.sling.nodes.console.Consoles.PN_CONSOLE_ID;

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

    @Nonnull
    public String getId() {
        return console != null ? console.getId() : getName();
    }

    @Nonnull
    public String getName() {
        return console != null ? console.getName() : super.getName();
    }

    @Nonnull
    public String getPath() {
        return console != null ? console.getPath() : super.getPath();
    }

    @Nonnull
    public String getLabel() {
        return console != null ? console.getLabel() : getName();
    }

    @Nonnull
    public String getDescription() {
        return console != null ? console.getDescription() : "";
    }

    @Nonnull
    public String getContentSrc() {
        return console != null ? console.getContentSrc() : "";
    }

    @Nonnull
    public String getUrl() {
        return console != null ? console.getUrl(context.getRequest()) : "#";
    }

    @Nonnull
    public String getLinkAttributes() {
        return console != null ? console.getLinkAttributes() : "";
    }

    public boolean isMenu() {
        return console != null && console.isMenu();
    }

    public boolean isValidMenu() {
        return isMenu() && getMenuItems().size() > 0;
    }

    @Nonnull
    public Collection<ConsoleModel> getMenuItems() {
        return console != null ? console.getMenuItems(context) : Collections.emptyList();
    }

    @Override
    @Nonnull
    public String toString() {
        return console != null ? console.toString(context) : "";
    }

    @Nonnull
    public String getDataSet() {
        return Base64.encodeBase64String(Consoles.getInstance(context).toString(context).getBytes(StandardCharsets.UTF_8));
    }
}
