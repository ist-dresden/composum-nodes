package com.composum.sling.core.pckgmgr.regpckg.view;

import static com.composum.sling.core.util.LinkUtil.EXT_HTML;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.PackagesServlet;
import com.composum.sling.core.pckgmgr.regpckg.tree.PackageNode;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryItem;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryTree;
import com.composum.sling.core.pckgmgr.regpckg.tree.VersionNode;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PackageBean extends ConsoleSlingBean implements PackageView, Comparable<PackageBean>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PackageBean.class);

    public static final String RESOURCE_TYPE = "";

    protected VersionBean currentVersion;
    protected final Map<String, VersionBean> versionSet = new TreeMap<>(new VersionComparator().reversed());

    @Override
    public void initialize(BeanContext context) {
        SlingHttpServletRequest request = context.getRequest();
        String path = RegistryUtil.requestPath(request);
        super.initialize(context,new SyntheticResource(context.getResolver(), path, RESOURCE_TYPE));
        try {
            load(context);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void close() throws Exception {
        if (currentVersion != null) {
            currentVersion.close();
        }
        versionSet.values().stream().forEach(VersionBean::close);
    }

    public PackageBean() {
    }

    public PackageBean(BeanContext context, String path) {
        super(context, new SyntheticResource(context.getResolver(), path, RESOURCE_TYPE));
    }

    public PackageBean(BeanContext context, String namespace, PackageId packageId) {
        super(context, new SyntheticResource(context.getResolver(),
                RegistryUtil.toPath(namespace, packageId), RESOURCE_TYPE));
    }

    public String getNamespace() {
        return currentVersion.getNamespace();
    }

    public PackageId getPackageId() {
        return currentVersion.getPackageId();
    }

    public String getGroup() {
        return getPackageId().getGroup();
    }

    @Override
    public String getName() {
        return getPackageId().getName();
    }

    public String getVersion() {
        return currentVersion.getVersion();
    }

    public boolean isValid() {
        return currentVersion.isValid();
    }

    public boolean isInstalled() {
        return currentVersion.isInstalled();
    }

    public boolean isClosed() {
        return currentVersion.isClosed();
    }

    public boolean isLoaded() {
        return currentVersion != null && currentVersion.isLoaded();
    }

    public void load(BeanContext context) throws IOException {
        boolean merged = RegistryUtil.namespace(getPath()) == null;
        RegistryTree tree = new RegistryTree(merged);
        RegistryItem treeItem = tree.getItem(context, getPath());
        if (treeItem instanceof VersionNode) {
            treeItem = ((VersionNode) treeItem).getPackageNode();
        }
        if (treeItem instanceof PackageNode) {
            PackageNode node = (PackageNode) treeItem;
            for (RegistryItem item : node.getItems()) {
                if (item instanceof VersionNode) {
                    VersionNode versionNode = (VersionNode) item;
                    VersionBean version = new VersionBean(context, versionNode.getPath());
                    if (versionNode.isCurrent()) {
                        currentVersion = version;
                    }
                    versionSet.put(versionNode.getVersion(), version);
                }
            }
            if (versionSet.size() > 0) {
                if (currentVersion == null) {
                    currentVersion = versionSet.values().iterator().next();
                }
            } else {
                LOG.error("no version found for '{}'", getPath());
            }
        } else {
            LOG.error("can't load PackageBean '{}' ({})", getPath(), treeItem != null ? treeItem.getPath() : "NULL");
        }
    }

    @Override
    public int compareTo(PackageBean other) {
        return getName().compareTo(other.getName());
    }

    public String getCssClasses() {
        return currentVersion != null ? currentVersion.getCssClasses() : "";
    }

    public String getDownloadUrl() {
        return currentVersion != null ? currentVersion.getDownloadUrl() : "";
    }

    @Override
    public String getUrl() {
        return LinkUtil.getUrl(getRequest(), PackagesServlet.SERVLET_PATH + EXT_HTML + getPath());
    }

    /**
     * All versions of the package that are currently in the registry.
     */
    public Collection<VersionBean> getAllVersions() {
        return versionSet.values();
    }

    /** A list of package versions that are obsolete because older than the current version. */
    public Collection<VersionBean> getObsoleteVersions() {
        return versionSet.values().stream()
                .filter(v -> currentVersion.obsoletes(v))
                .collect(Collectors.toList());
    }

    /** True iff there is more than one version of the package present. */
    public boolean isHasAlternativeVersions() {
        return versionSet.size() > 1;
    }

}
