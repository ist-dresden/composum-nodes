package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static com.composum.sling.core.util.ResourceUtil.PROP_DESCRIPTION;

public class Template extends ConsoleSlingBean implements Comparable<Template> {

    public static final String PROP_GROUP = "group";
    public static final String PROP_XPATH = "xpath";
    public static final String PROP_SQL2 = "sql2";

    public static final String PROP_ORDER = "order";
    public static final int ORDER_DEFAULT = 50;

    private transient String data;

    public Template(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Template(BeanContext context) {
        super(context);
    }

    public Template() {
        super();
    }

    public int getOrder() {
        return getProperty(PROP_ORDER, ORDER_DEFAULT);
    }

    public String getId() {
        return getPath().replace('/', '-');
    }

    @Override
    public String getTitle() {
        String title = super.getTitle();
        return StringUtils.isNotBlank(title) ? title : getName();
    }

    public String getDescription() {
        return getProperty(PROP_DESCRIPTION, "");
    }

    public String getData() {
        if (data == null) {
            StringWriter stringWriter = new StringWriter();
            try {
                JsonWriter writer = new JsonWriter(stringWriter);
                writer.beginObject();
                writer.name(PROP_XPATH).value(getXpath());
                writer.name(PROP_SQL2).value(getSql2());
                writer.endObject();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stringWriter.flush();
            data = Base64.encodeBase64String(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
        }
        return data;
    }

    public String getXpath() {
        return getProperty(PROP_XPATH, "");
    }

    public String getSql2() {
        return getProperty(PROP_SQL2, "");
    }

    @Override
    public int compareTo(Template other) {
        int result = getOrder() - other.getOrder();
        if (result == 0) {
            result = getTitle().compareTo(other.getTitle());
        }
        return result;
    }
}
