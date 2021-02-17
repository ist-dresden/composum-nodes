package com.composum.sling.core.pckgmgr.registry.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.registry.util.RegistryUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class VersionBean extends ConsoleSlingBean implements PackageView {

    public static final String RESOURCE_TYPE = "";

    private static final Logger LOG = LoggerFactory.getLogger(VersionBean.class);

    protected String namespace;
    protected PackageId packageId;

    protected boolean loaded = false;

    private transient RegisteredPackage regPckg;
    private transient VaultPackage vltPckg;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        String path = resource != null ? resource.getPath() : RegistryUtil.requestPath(request);
        this.namespace = RegistryUtil.namespace(path);
        this.packageId = RegistryUtil.fromPath(path);
        try {
            load(context);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public VersionBean() {
    }

    public VersionBean(BeanContext context, String path) {
        super(context, new SyntheticResource(context.getResolver(), path, RESOURCE_TYPE));
    }

    public VersionBean(BeanContext context, String namespace, PackageId packageId) {
        super(context, new SyntheticResource(context.getResolver(),
                RegistryUtil.toPath(namespace, packageId), RESOURCE_TYPE));
    }

    public String getNamespace() {
        return namespace;
    }

    public PackageId getPackageId() {
        return packageId;
    }

    public String getGroup() {
        return getPackageId().getGroup();
    }

    @Override
    public String getName() {
        return getPackageId().getName();
    }

    public String getVersion() {
        return getPackageId().getVersionString();
    }

    public boolean isValid() {
        return vltPckg != null && vltPckg.isValid();
    }

    public boolean isInstalled() {
        return regPckg != null && regPckg.isInstalled();
    }

    public boolean isClosed() {
        return vltPckg != null && vltPckg.isClosed();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void load(BeanContext context) throws IOException {
        if (regPckg == null) {
            regPckg = RegistryUtil.open(context, getNamespace(), getPackageId());
        }
        if (regPckg != null) {
            vltPckg = regPckg.getPackage();
        }
        loaded = true;
    }
}
