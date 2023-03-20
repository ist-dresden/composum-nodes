package com.composum.sling.core.pckgmgr.regpckg.view;

import static com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil.THUMBNAIL_PNG;
import static com.composum.sling.core.util.LinkUtil.EXT_HTML;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.pckgmgr.PackagesServlet;
import com.composum.sling.core.pckgmgr.jcrpckg.PackageServlet;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.core.pckgmgr.jcrpckg.view.PackageBean;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;
import com.composum.sling.core.pckgmgr.regpckg.util.VersionComparator;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.NoSuchPackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.registry.DependencyReport;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.jackrabbit.vault.packaging.registry.impl.JcrRegisteredPackage;
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
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

@Restricted(key = PackageServlet.SERVICE_KEY)
public class VersionBean extends ConsoleSlingBean implements PackageView, AutoCloseable {

    public static final String RESOURCE_TYPE = "composum/nodes/pckgmgr/version";

    private static final Logger LOG = LoggerFactory.getLogger(VersionBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    protected static final Comparator<PackageId> BY_GROUP_AND_NAME_COMPARATOR
            = new VersionComparator.PackageIdByGroupAndNameComparator();
    protected static final Comparator<PackageId> PACKAGE_ID_COMPARATOR
            = new VersionComparator.PackageIdComparator(false);

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
        String path = resource.isResourceType(RESOURCE_TYPE) ? resource.getPath() : RegistryUtil.requestPath(request);
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

            Pair<String, PackageId> found = registries.resolve(getNamespace(), getPackageId());
            if (found != null && found.getRight() != null) {
                registryNamespace = found.getLeft();
            }
        }
        // we defer everything that needs opening the package until it's actually needed, since this is
        // expensive, and we don't need it e.g. when traversing the RegistryTree .
        // loadPackage() is called by the getters that need an open package.
    }

    /** Performs the expensive parts of the loading process (that need actually opening the package) if needed. */
    protected void loadPackage() {
        try {
            if (!loaded) {
                loaded = true;
                if (regPckg == null && registryNamespace != null) {
                    PackageRegistry registry = registries.getRegistry(registryNamespace);
                    regPckg = registry.open(getPackageId());
                    if (regPckg != null) {
                        packageProps = regPckg.getPackageProperties();
                        vltPckg = regPckg.getPackage();
                    }
                }
            }
        } catch (RuntimeException | IOException e) {
            LOG.error("Error loading {} {}", getNamespace(), getPackageId(), e);
            invalid = true;
        }
    }

    /** Returns a path containing the registry namespace even in merged mode. */
    public String getPathWithRegistry() {
        return RegistryUtil.toPath(registryNamespace, getPackageId());
    }

    @Override
    public void close() {
        try {
            if (regPckg != null) {
                regPckg.close();
            }
        } catch (Exception e) {
            LOG.error("Error closing {}", getPath(), e);
        } finally {
            try {
                if (vltPckg != null && (regPckg == null || vltPckg != regPckg.getPackage())) {
                    vltPckg.close();
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
        loadPackage();
        return !invalid && vltPckg != null && vltPckg.isValid();
    }

    public boolean isInstalled() {
        loadPackage();
        return regPckg != null && regPckg.isInstalled();
    }

    public boolean isClosed() {
        loadPackage();
        return vltPckg != null && vltPckg.isClosed();
    }

    public String getDescription() {
        loadPackage();
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
        loadPackage();
        WorkspaceFilter workspaceFilter = regPckg != null ? regPckg.getWorkspaceFilter() : null;
        return workspaceFilter != null ? workspaceFilter.getFilterSets() : null;
    }

    public String getAuditLogUrl() {
        return LinkUtil.getUrl(getRequest(), PackageBean.AUDIT_LOG_BASE + getPath());
    }

    public String getInstallationTime() {
        loadPackage();
        return packageProps != null ? format(regPckg.getInstallationTime(), null) : null;
    }

    public String getCreated() {
        loadPackage();
        return packageProps != null ? format(packageProps.getCreated(), packageProps.getProperty(PackageProperties.NAME_CREATED)) : null;
    }

    public String getCreatedBy() {
        loadPackage();
        return packageProps != null ? packageProps.getCreatedBy() : "--";
    }

    public String getLastModified() {
        loadPackage();
        return packageProps != null ? format(packageProps.getLastModified(), packageProps.getProperty(PackageProperties.NAME_LAST_MODIFIED)) : null;
    }

    public String getLastModifiedBy() {
        loadPackage();
        return packageProps != null ? packageProps.getLastModifiedBy() : "--";
    }

    public String getLastWrapped() {
        loadPackage();
        return packageProps != null ? format(packageProps.getLastWrapped(), packageProps.getProperty(PackageProperties.NAME_LAST_WRAPPED)) : null;
    }

    public String getLastWrappedBy() {
        loadPackage();
        return packageProps != null ? packageProps.getLastWrappedBy() : "--";
    }

    public String getAcHandling() {
        loadPackage();
        AccessControlHandling acHandling = packageProps.getACHandling();
        return acHandling != null ? acHandling.name() : "";
    }

    public String getAcHandlingLabel() {
        loadPackage();
        AccessControlHandling acHandling = packageProps.getACHandling();
        return acHandling != null ? acHandling.name() : "-- -- ";
    }

    public boolean getRequiresRestart() {
        loadPackage();
        return RegistryUtil.booleanProperty(packageProps,PackageUtil.DEF_REQUIRES_RESTART, false);
    }

    public boolean getRequiresRoot() {
        loadPackage();
        return packageProps.requiresRoot();
    }

    public String getProviderName()
    {
        loadPackage();
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_NAME);
    }

    public String getProviderUrl() {
        loadPackage();
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_URL);
    }

    public String getProviderLink() {
        loadPackage();
        return packageProps.getProperty(PackageUtil.DEF_PROVIDER_LINK);
    }

    public String[] getDependencies() {
        loadPackage();
        return Arrays.stream(packageProps.getDependencies()).map(Object::toString).toArray(String[]::new);
    }

    protected void calculateDependencies() {
        if (unresolvedDependencies == null) {
            loadPackage();
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
        loadPackage();
        // TODO: what is the format here? Does that really exist? That's just a guess here.
        String replaces = packageProps.getProperty(PackageUtil.DEF_REPLACES);
        return StringUtils.isNotBlank(replaces) ? replaces.split(",") : null;
    }

    public String[] getUsages() {
        String[] result = null;
        try {
            PackageId[] usages = registries.getRegistry(getRegistryNamespace()).usage(getPackageId());
            result = Arrays.stream(usages).map(Object::toString).toArray(String[]::new);
        } catch (RuntimeException | IOException e) {
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

    @Override
    public String getUrl() {
        return LinkUtil.getUrl(getRequest(), PackagesServlet.SERVLET_PATH + EXT_HTML + getPath());
    }

    /** True if this obsoletes the other version - that is, it has same group and name but a newer version. */
    public boolean obsoletes(VersionBean other) {
        return BY_GROUP_AND_NAME_COMPARATOR.compare(this.getPackageId(), other.getPackageId()) == 0 &&
                PACKAGE_ID_COMPARATOR.compare(this.getPackageId(), other.getPackageId()) > 0;
    }

    /** Thumbnail works only for JCR packages. */
    @Nullable
    public String getThumbnailUrl() throws IOException {
        loadPackage();
        Archive.Entry thumbnailEntry = vltPckg.getArchive().getEntry("META-INF/vault/definition/thumbnail.png");
        String thumbnailUrl = null;
        if (thumbnailEntry != null) {
            thumbnailUrl = PackageServlet.SERVLET_PATH + "." + PackageServlet.Operation.thumbnail + ".png" + RegistryUtil.toPath(registryNamespace, packageId);
        }
        return thumbnailUrl;
    }

}
