package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.NoSuchPackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.DependencyReport;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class VersionBean extends ConsoleSlingBean implements PackageView, AutoCloseable {

    public static final String RESOURCE_TYPE = "";

    private static final Logger LOG = LoggerFactory.getLogger(VersionBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    protected String namespace;
    protected PackageId packageId;

    protected boolean loaded = false;

    protected transient RegisteredPackage regPckg;
    protected transient VaultPackage vltPckg;
    protected transient PackageProperties packageProps;
    protected transient boolean invalid;
    protected transient String registryNamespace;
    protected transient PackageRegistries.Registries registries;

    protected String[] satisfiedDependencies;
    protected String[] notInstalledDependencies;
    protected String[] unresolvedDependencies;

    public VersionBean() {
    }

    public VersionBean(BeanContext context, String path) {
        super(context, new SyntheticResource(context.getResolver(), path, RESOURCE_TYPE));
    }

    public VersionBean(BeanContext context, String namespace, PackageId packageId) {
        super(context, new SyntheticResource(context.getResolver(),
                RegistryUtil.toPath(namespace, packageId), RESOURCE_TYPE));
    }

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
            LOG.error("Error loading {}", getPackageId(), ex);
            invalid = true;
        }
    }

    public void load(BeanContext context) throws IOException {
        if (registries == null) {
            PackageRegistries service = context.getService(PackageRegistries.class);
            registries = service.getRegistries(context.getResolver());
        }
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

    public String getAcHandling() {
        AccessControlHandling acHandling = packageProps.getACHandling();
        return acHandling != null ? acHandling.name() : "";
    }

    public String getAcHandlingLabel() {
        AccessControlHandling acHandling = packageProps.getACHandling();
        return acHandling != null ? acHandling.name() : "-- -- ";
    }

    public boolean getRequiresRestart() {
        return RegistryUtil.booleanProperty(packageProps,PackageUtil.DEF_REQUIRES_RESTART, false);
    }

    public boolean getRequiresRoot() {
        return packageProps.requiresRoot();
    }

    public String getProviderName()
    {
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_NAME);
    }

    public String getProviderUrl() {
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_URL);
    }

    public String getProviderLink() {
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_LINK);
    }

    public String[] getDependencies() {
        return Arrays.stream(packageProps.getDependencies()).map(Object::toString).toArray(String[]::new);
    }

    protected void calculateDependencies() {
        if (unresolvedDependencies == null) {
            try {
                DependencyReport reportInstalled = registries.getRegistry(getRegistryNamespace()).analyzeDependencies(getPackageId(), true);;
                DependencyReport reportAll = registries.getRegistry(getRegistryNamespace()).analyzeDependencies(getPackageId(), false);
                satisfiedDependencies = Arrays.stream(reportInstalled.getResolvedDependencies()).map(Object::toString).toArray(String[]::new);
                List<PackageId> uninstalledDependencyList = new ArrayList<>(Arrays.asList(reportAll.getResolvedDependencies()));
                uninstalledDependencyList.removeAll(Arrays.asList(reportInstalled.getResolvedDependencies()));
                notInstalledDependencies = uninstalledDependencyList.stream().map(Object::toString).toArray(String[]::new);
                unresolvedDependencies = Arrays.stream(reportAll.getUnresolvedDependencies()).map(Object::toString).toArray(String[]::new);
            } catch (IOException | NoSuchPackageException | RuntimeException e) {
                LOG.error("Error calculating dependencies for {}", getPath(), e);
            }
        }
    }

    public String[] getSatisfiedDependencies() {
        calculateDependencies();
        return satisfiedDependencies;
    }

    public String[] getNotInstalledDependencies() {
        calculateDependencies();
        return notInstalledDependencies;
    }

    public String[] getUnresolvedDependencies() {
        calculateDependencies();
        return unresolvedDependencies;
    }

    public String[] getReplaces() {
        // TODO: what is the format here? Does that really exist? That's just a guess here.
        String replaces = packageProps.getProperty(PackageUtil.DEF_REPLACES);
        return StringUtils.isNotBlank(replaces) ? replaces.split(",") : null;
    }

    public String[] getUsages() {
        String[] result = null;
        try {
            PackageId[] usages = registries.getRegistry(getRegistryNamespace()).usage(getPackageId());
            result = Arrays.stream(usages).map(Object::toString).toArray(String[]::new);
        } catch (IOException e) {
            LOG.error("Error calculating dependencies for {}", getPackageId(), e);
        }
        return result;
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
