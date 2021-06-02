package com.composum.sling.core.pckgmgr.regpckg.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil.PropertyMap;
import com.composum.sling.core.pckgmgr.regpckg.view.PackageView;
import com.composum.sling.core.util.RequestUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.config.Registry;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil.date;

public class VersionNode extends AbstractNode implements PackageView {

    private static final Logger LOG = LoggerFactory.getLogger(PackageNode.class);

    protected final PackageNode pckg;
    protected final String namespace;
    protected final PackageId packageId;
    protected final String version;
    protected final String registryNamespace;

    private transient Boolean valid;
    private transient Boolean installed;
    private transient Calendar installTime;

    public VersionNode(@Nonnull PackageNode pckg, @Nonnull String registryNamespace, @Nonnull PackageId packageId, @Nonnull String version) {
        String path = pckg.getPath() + "/" + version;
        this.pckg = pckg;
        this.namespace = RegistryUtil.namespace(path);
        this.packageId = packageId;
        this.version = version;
        this.registryNamespace = registryNamespace;
        put(KEY_PATH, path);
        put(KEY_NAME, version);
        put(KEY_TEXT, version);
        put(KEY_TYPE, "version");
    }

    /** The namespace encoded in the path - can be empty if the tree is merged. */
    @Override
    public String getNamespace() {
        return namespace;
    }

    public PackageId getPackageId() {
        return packageId;
    }

    /** Returns the full path for the version, including the registry namespace - even in merged mode. */
    public String getNamespacedPath() {
        return RegistryUtil.pathWithNamespace(registryNamespace, getPath());
    }

    @Override
    public String getGroup() {
        return getPackageId().getGroup();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isValid() {
        return valid != null && valid;
    }

    public boolean isInstalled() {
        return installed != null && installed;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    protected void toTreeState(@Nonnull final JsonWriter writer) throws IOException {
        super.toTreeState(writer);
        writer.name("installed").value(isInstalled());
        writer.name("current").value(isCurrent());
        if (isInstalled()) {
            Calendar installTime = getInstallTime();
            if (installTime != null) {
                writer.name("installTime").value(date(installTime));
            }
        }
    }

    public PackageNode getPackageNode() {
        return pckg;
    }

    public boolean isCurrent() {
        return pckg.getCurrentInstalled() == this;
    }

    public Calendar getInstallTime() {
        return installTime;
    }

    public PropertyMap getPackageProps() {
        return (PropertyMap) get("package");
    }

    @Override
    protected void toTreeProperties(@Nonnull JsonWriter writer) throws IOException {
        super.toTreeProperties(writer);
        writer.name("namespace").value(registryNamespace);
        writer.name("namespacedPath").value(getNamespacedPath());
        writer.name("packageid");
        writer.beginObject();
        writer.name("name").value(packageId.getName());
        writer.name("group").value(packageId.getGroup());
        writer.name("version").value(packageId.getVersionString());
        writer.name("downloadName").value(packageId.getDownloadName());
        writer.name("registry").value(registryNamespace);
        writer.endObject();
    }

    @Override
    public void load(@Nonnull final BeanContext context) throws IOException {
        if (!isLoaded()) {
            installed = null;
            installTime = null;
            PackageId packageId = getPackageId();
            PackageRegistries service = context.getService(PackageRegistries.class);
            PackageRegistry registry = service.getRegistries(context.getResolver()).getRegistry(getPath());
            if (registry != null) {
                try (RegisteredPackage pckg = registry.open(packageId);
                     VaultPackage vaultPckg = pckg != null ? pckg.getPackage() : null) {
                    if (pckg != null) {
                        Map<String, Object> pckgProps = new PropertyMap();
                        put("package", pckgProps);
                        pckgProps.put("installed", installed = pckg.isInstalled());
                        pckgProps.put("installTime", this.installTime = pckg.getInstallationTime());
                        pckgProps.put("size", pckg.getSize());
                        pckgProps.put("valid", valid = vaultPckg.isValid());
                        MetaInf metaInf = vaultPckg.getMetaInf();
                        put("properties", RegistryUtil.properties(pckg));
                        if (RequestUtil.checkSelector(context.getRequest(), "filter")) {
                            List<Map<String, Object>> filterList = new ArrayList<>();
                            put("filter", filterList);
                            WorkspaceFilter filter = pckg.getWorkspaceFilter();
                            for (PathFilterSet filterSet : filter.getFilterSets()) {
                                Map<String, Object> setProps = new PropertyMap();
                                filterList.add(setProps);
                                setProps.put("root", filterSet.getRoot());
                                //setProps.put("type", filterSet.getType());
                                setProps.put("importMode", filterSet.getImportMode());
                                List<Map<String, Object>> entryList = new ArrayList<>();
                                for (FilterSet.Entry<PathFilter> entry : filterSet.getEntries()) {
                                    Map<String, Object> entryProps = new PropertyMap();
                                    entryList.add(entryProps);
                                    PathFilter pathFilter = entry.getFilter();
                                    entryProps.put(entry.isInclude() ? "include" : "exclude", pathFilter.toString());
                                    //entryProps.put("absolute", pathFilter.isAbsolute());
                                }
                                if (entryList.size() > 0) {
                                    setProps.put("entries", entryList);
                                }
                            }
                        }
                    } else {
                        LOG.warn("can't open package '{}' ({})", packageId, registry);
                    }
                }
            } else {
                LOG.warn("no registry found for '{}'", getPath());
            }
            setLoaded(true);
        }
    }
}
