package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Restricted(key = ConfigSet.SERVICE_KEY)
public class Template extends ConfigItem {

    public static final String PROP_GROUP = "group";
    public static final String PROP_XPATH = "xpath";
    public static final String PROP_SQL2 = "sql2";

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
}
