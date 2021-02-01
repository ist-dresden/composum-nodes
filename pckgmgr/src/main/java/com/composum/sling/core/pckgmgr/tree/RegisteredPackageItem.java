package com.composum.sling.core.pckgmgr.tree;

import com.composum.sling.core.pckgmgr.util.RegistryUtil;
import com.composum.sling.core.util.JsonUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;

import static com.composum.sling.core.pckgmgr.util.PackageUtil.DATE_FORMAT;

public class RegisteredPackageItem implements TreeItem {

    protected final RegisteredPackage registeredPackage;
    protected final String treePath;

    public RegisteredPackageItem(RegisteredPackage registeredPackage) {
        this.registeredPackage = registeredPackage;
        this.treePath = RegistryUtil.toTreePath(registeredPackage.getId());
    }

    @Override
    public String getName() {
        return registeredPackage.getId().getName();
    }

    @Override
    public String getPath() {
        return treePath;
    }

    @Override
    public void toJson(JsonWriter writer) throws IOException {
        final PackageId id = registeredPackage.getId();
        final String filename = RegistryUtil.getFilename(id);
        final String path = getPath();
        final PackageProperties properties = registeredPackage.getPackageProperties();
        writer.beginObject();
        JsonUtil.jsonMapEntries(writer, new LinkedHashMap<String, Object>() {{
            put("id", path);
            put("path", path);
            put("name", filename);
            put("text", filename);
            put("type", "package");
            put("state", new LinkedHashMap<String, Object>() {{
                put("loaded", Boolean.TRUE);
            }});
            put("properties", new LinkedHashMap<String, Object>() {{
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                String description = properties.getDescription();
                Calendar date;
                put(JcrPackageDefinition.PN_GROUP, id.getGroup());
                put(JcrPackageDefinition.PN_NAME, id.getName());
                put(JcrPackageDefinition.PN_VERSION, id.getVersionString());
                if (description != null) {
                    put(JcrPackageDefinition.PN_DESCRIPTION, description);
                }
                if ((date = properties.getLastModified()) != null) {
                    put(JcrPackageDefinition.PN_LASTMODIFIED, dateFormat.format(date.getTime()));
                }
            }});
            put("file", filename);
        }});
        writer.endObject();
    }
}
