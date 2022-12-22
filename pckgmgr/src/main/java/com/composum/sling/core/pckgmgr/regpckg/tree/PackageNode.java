package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil.PropertyMap;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.pckgmgr.regpckg.view.PackageView;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class PackageNode extends AbstractNode implements PackageView {

    private static final Logger LOG = LoggerFactory.getLogger(PackageNode.class);

    public static final Comparator<String> COMPARATOR = new VersionComparator().reversed();

    protected final String namespace;
    protected final GroupNode group;
    protected final PackageId packageId;

    private transient VersionNode currentInstalled;

    public PackageNode(GroupNode group, PackageId packageId) {
        String path = group.getPath() + "/" + packageId.getName();
        this.namespace = RegistryUtil.namespace(path);
        this.group = group;
        this.packageId = packageId;
        put(KEY_PATH, path);
        put(KEY_NAME, packageId.getName());
        put(KEY_TEXT, packageId.getName());
        put(KEY_TYPE, "package");
        put("package", new PropertyMap());
        Map<String, VersionNode> items = new TreeMap<>(COMPARATOR);
        put(KEY_ITEMS, items);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Nonnull
    public PackageId getPackageId() {
        return packageId;
    }

    @Override
    public String getGroup() {
        return getPackageId().getGroup();
    }

    @Override
    public String getVersion() {
        return currentInstalled != null ? currentInstalled.getVersion() : getPackageId().getVersionString();
    }

    @Override
    public boolean isValid() {
        return currentInstalled != null && currentInstalled.isValid();
    }

    @Override
    public boolean isInstalled() {
        return currentInstalled != null && currentInstalled.isInstalled();
    }

    @Override
    public boolean isClosed() {
        return currentInstalled != null && currentInstalled.isClosed();
    }

    @Override
    protected void toTreeState(@Nonnull final JsonWriter writer) throws IOException {
        super.toTreeState(writer);
        VersionNode version = getCurrentInstalled();
        if (version != null) {
            writer.name("version").value(version.getVersion());
            writer.name("installed").value(version.isInstalled());
        }
    }

    public VersionNode getCurrentInstalled() {
        return currentInstalled;
    }

    @Override
    public void load(@Nonnull final BeanContext context) throws IOException {
        if (!isLoaded()) {
            currentInstalled = null;
            for (RegistryItem item : getItems()) {
                if (item instanceof VersionNode) {
                    VersionNode version = (VersionNode) item;
                    if (!version.isLoaded()) {
                        version.load(context);
                    }
                    if (version.isInstalled()) {
                        Calendar installTime = version.getInstallTime();
                        if (currentInstalled == null ||
                                (currentInstalled.getInstallTime() != null && installTime != null &&
                                        installTime.after(currentInstalled.getInstallTime()))) {
                            currentInstalled = version;
                        }
                    }
                }
            }
            if (currentInstalled != null) {
                PropertyMap versionProps = currentInstalled.getPackageProps();
                PropertyMap pckgProps = (PropertyMap) get("package");
                pckgProps.put("version", currentInstalled.getVersion());
                pckgProps.put("installed", currentInstalled.isInstalled());
                pckgProps.put("installTime", currentInstalled.getInstallTime());
                pckgProps.put("size", versionProps.get("size"));
                pckgProps.put("valid", versionProps.get("valid"));
            }
            setLoaded(true);
        }
    }

    @Nullable
    public VersionNode getVersion(String versionKey) {
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        return (VersionNode) items.get(versionKey);
    }

    @Nonnull
    public VersionNode addVersion(@Nonnull String registryNamespace, @Nonnull PackageId id) {
        String versionKey = id.getVersionString();
        if (StringUtils.isBlank(versionKey)) {
            versionKey = RegistryUtil.NO_VERSION;
        }
        Map<String, RegistryItem> items = Objects.requireNonNull(getItemsMap());
        VersionNode version = (VersionNode) items.get(versionKey);
        if (version == null) {
            version = new VersionNode(this, registryNamespace, id, versionKey);
            items.put(versionKey, version);
        }
        return version;
    }
}
