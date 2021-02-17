package com.composum.sling.core.pckgmgr.registry.tree;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.registry.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.registry.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.registry.util.RegistryUtil.PropertyMap;
import com.composum.sling.core.pckgmgr.registry.view.PackageView;
import com.composum.sling.core.util.RequestUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
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

import static com.composum.sling.core.pckgmgr.registry.util.RegistryUtil.date;

public class VersionNode extends AbstractNode implements PackageView {

    private static final Logger LOG = LoggerFactory.getLogger(PackageNode.class);

    protected final PackageNode pckg;
    protected final String namespace;
    protected final PackageId packageId;
    protected final String version;

    private transient Boolean valid;
    private transient Boolean installed;
    private transient Calendar installTime;

    public VersionNode(PackageNode pckg, PackageId packageId, String version) {
        String path = pckg.getPath() + "/" + version;
        this.pckg = pckg;
        this.namespace = RegistryUtil.namespace(path);
        this.packageId = packageId;
        this.version = version;
        put(KEY_PATH, path);
        put(KEY_NAME, version);
        put(KEY_TEXT, version);
        put(KEY_TYPE, "version");
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public PackageId getPackageId() {
        return packageId;
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
    public void load(@Nonnull final BeanContext context) throws IOException {
        if (!isLoaded()) {
            installed = null;
            installTime = null;
            PackageId packageId = getPackageId();
            PackageRegistries service = context.getService(PackageRegistries.class);
            PackageRegistry registry = service.getRegistries(context.getResolver()).getRegistry(getPath());
            if (registry != null) {
                RegisteredPackage pckg = registry.open(packageId);
                if (pckg != null) {
                    Map<String, Object> pckgProps = new PropertyMap();
                    put("package", pckgProps);
                    pckgProps.put("installed", installed = pckg.isInstalled());
                    pckgProps.put("installTime", this.installTime = pckg.getInstallationTime());
                    pckgProps.put("size", pckg.getSize());
                    VaultPackage vaultPckg = pckg.getPackage();
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
            } else {
                LOG.warn("no registry found for '{}'", getPath());
            }
            setLoaded(true);
        }
    }
}
