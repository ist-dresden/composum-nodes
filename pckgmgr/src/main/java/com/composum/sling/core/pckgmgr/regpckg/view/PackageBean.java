package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.tree.PackageNode;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryItem;
import com.composum.sling.core.pckgmgr.regpckg.tree.RegistryTree;
import com.composum.sling.core.pckgmgr.regpckg.tree.VersionNode;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PackageBean extends ConsoleSlingBean implements PackageView, Comparable<PackageBean> {

    private static final Logger LOG = LoggerFactory.getLogger(PackageBean.class);

    public static final String RESOURCE_TYPE = "";

    protected VersionBean currentVersion;
    protected Map<String, VersionBean> versionSet = new LinkedHashMap<>();

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
        RegistryTree tree = new RegistryTree(false);
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
}
