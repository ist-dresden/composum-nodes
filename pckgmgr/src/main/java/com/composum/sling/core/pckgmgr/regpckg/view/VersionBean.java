package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionBean extends ConsoleSlingBean implements PackageView, AutoCloseable {

    public static final String RESOURCE_TYPE = "";

    private static final Logger LOG = LoggerFactory.getLogger(VersionBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    protected String namespace;
    protected PackageId packageId;

    protected boolean loaded = false;

    private transient RegisteredPackage regPckg;
    private transient VaultPackage vltPckg;
    private transient PackageProperties packageProps;
    private transient boolean invalid;
    private transient String registryNamespace;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        SlingHttpServletRequest request = context.getRequest();
        String path = RegistryUtil.requestPath(request);
        this.namespace = RegistryUtil.namespace(path);
        this.packageId = RegistryUtil.fromPath(path);
        super.initialize(context, new SyntheticResource(context.getResolver(), path, RESOURCE_TYPE));
        try {
            load(context);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            invalid = true;
        }
    }

    @Override
    public void close() {
        try {
            if (vltPckg != null) {
                vltPckg.close();
            }
        } catch (Exception e) {
            LOG.error("Error closing {}", getPath(), e);
        } finally {
            try {
                if (regPckg != null) {
                    regPckg.close();
                }
            } catch (Exception e) {
                LOG.error("Error closing {}", getPath(), e);
            }
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

    /** Namespace as given in the path. This is empty in mixed mode - then only {@link #getRegistryNamespace()} is set. */
    public String getNamespace() {
        return namespace;
    }

    /** Namespace of the registry, if the package was really found. */
    public String getRegistryNamespace() {
        return registryNamespace;
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
        return !invalid && vltPckg != null && vltPckg.isValid();
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
            Pair<String, RegisteredPackage> foundPckg = RegistryUtil.open(context, getNamespace(), getPackageId());
            if (foundPckg != null) {
                regPckg = foundPckg.getRight();
                registryNamespace = foundPckg.getLeft();
            }
        }
        if (regPckg != null) {
            packageProps = regPckg.getPackageProperties();
            vltPckg = regPckg.getPackage();
        }
        loaded = true;
    }

    public String getDescription() {
        return packageProps != null ? packageProps.getDescription() : null;
    }

    public String getCssClasses() {
        List<String> classes = new ArrayList<>();
        if (isInstalled()) {
            classes.add("installed");
        }
        if (isValid()) {
            classes.add("valid");
        }
        // TODO where to get sealed? Is this really necessary, anyway?
        return StringUtils.join(classes, " ");
    }

    public String getFilename() {
        return getPackageId().getDownloadName();
        // probably better than return vltPckg.getFile().getName(), and that's not set on JCR packages, anyway
    }

    public String getDownloadUrl() {
        return LinkUtil.getUrl(getRequest(), RegistryUtil.getDownloadURI(registryNamespace, getPackageId()));
    }

    public List<PathFilterSet> getFilterList() {
        WorkspaceFilter workspaceFilter = regPckg != null ? regPckg.getWorkspaceFilter() : null;
        return workspaceFilter != null ? workspaceFilter.getFilterSets() : null;
    }

    public String getAuditLogUrl() {
        return "auditlog-invalid-yet";
        // FIXME(hps,27.05.21) implement, compare com.composum.sling.core.pckgmgr.jcrpckg.view.PackageBean
    }

    public String getThumbnailUrl() {
        // FIXME(hps,27.05.21) implement, compare com.composum.sling.core.pckgmgr.jcrpckg.view.PackageBean
        return "";
    }

    public String getInstallationTime() {
        return packageProps != null ? format(regPckg.getInstallationTime(), null) : null;
    }

    public String getCreated() {
        return packageProps != null ? format(packageProps.getCreated(), packageProps.getProperty(PackageProperties.NAME_CREATED)) : null;
    }

    public String getCreatedBy() {
        return packageProps != null ? packageProps.getCreatedBy() : "--";
    }

    public String getLastModified() {
        return packageProps != null ? format(packageProps.getLastModified(), packageProps.getProperty(PackageProperties.NAME_LAST_MODIFIED)) : null;
    }

    public String getLastModifiedBy() {
        return packageProps != null ? packageProps.getLastModifiedBy() : "--";
    }

    public String getLastWrapped() {
        return packageProps != null ? format(packageProps.getLastWrapped(), packageProps.getProperty(PackageProperties.NAME_LAST_WRAPPED)) : null;
    }

    public String getLastWrappedBy() {
        return packageProps != null ? packageProps.getLastWrappedBy() : "--";
    }

    /**
     * Formats a date, including a workaround for <a href="https://issues.apache.org/jira/browse/JCRVLT-526>JCRVLT-526</a> when reading dates from {@link PackageProperties}.
     * Usage e.g. {code}format(packageProps.getLastModified(), packageProps.getProperty(PackageProperties.NAME_LAST_MODIFIED)){code}
     */
    protected String format(Calendar rawDate, String dateRep) {
        Calendar date = RegistryUtil.readPackagePropertyDate(rawDate, dateRep);
        return date != null ? new SimpleDateFormat(DATE_FORMAT).format(date.getTime()) : "";
    }

}
