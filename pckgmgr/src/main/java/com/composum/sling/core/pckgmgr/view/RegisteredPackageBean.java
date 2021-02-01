package com.composum.sling.core.pckgmgr.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.PackageJobExecutor;
import com.composum.sling.core.pckgmgr.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.util.PackageUtil;
import com.composum.sling.core.pckgmgr.util.RegistryUtil;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.composum.sling.core.util.LinkUtil.EXT_HTML;

public class RegisteredPackageBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(RegisteredPackageBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String AUDIT_LOG_BASE = "/bin/browser.html" + PackageJobExecutor.AUDIT_BASE_PATH;

    protected String path;
    protected RegisteredPackage pckg;

    private transient PackageProperties properties;

    private transient String description;
    private transient String filename;
    private transient String downloadUrl;
    private transient String auditLogUrl;

    private transient Calendar created;
    private transient String createdBy;
    private transient Calendar lastModified;
    private transient String lastModifiedBy;
    private transient Calendar lastWrapped;
    private transient String lastWrappedBy;

    private transient List<PathFilterSet> filterList;
    private transient List<Dependency> dependencies;

    private transient String thumbnailUrl;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        SlingHttpServletRequest request = context.getRequest();
        path = RegistryUtil.getTreePath(request);
        PackageId id = RegistryUtil.fromTreePath(path);
        pckg = context.getService(PackageRegistries.class).getPackage(id);
        super.initialize(context, resource);
    }

    protected String format(Calendar date) {
        return date != null ? new SimpleDateFormat(DATE_FORMAT).format(date.getTime()) : "";
    }

    public String getCssClasses() {
        return StringUtils.join(collectCssClasses(new ArrayList<>()), " ");
    }

    protected List<String> collectCssClasses(List<String> collection) {
        if (pckg != null) {
            if (pckg.isInstalled()) {
                collection.add("installed");
            }
        }
        return collection;
    }

    @Override
    public String getUrl() {
        return LinkUtil.getUrl(getRequest(), PackagesServlet.SERVLET_PATH + EXT_HTML + getPath());
    }

    @Override
    public String getPath() {
        return RegistryUtil.toTreePath(pckg.getId());
    }

    public String getGroup() {
        return pckg.getId().getGroup();
    }

    @Override
    public String getName() {
        return pckg.getId().getName();
    }

    public String getVersion() {
        return pckg.getId().getVersionString();
    }

    public String getDescription() {
        if (description == null) {
            description = getProperties().getDescription();
            if (description == null) {
                description = "";
            }
        }
        return description;
    }

    public String getFilename() {
        if (filename == null) {
            filename = RegistryUtil.getFilename(pckg.getId());
        }
        return filename;
    }

    public String getDownloadUrl() {
        if (downloadUrl == null) {
            downloadUrl = LinkUtil.getUrl(getRequest(), RegistryUtil.getDownloadURI(pckg.getId()));
        }
        return downloadUrl;
    }

    public String getCreated() {
        if (created == null) {
            created = getProperties().getCreated();
        }
        return created != null ? format(created) : "--";
    }

    public String getCreatedBy() {
        if (createdBy == null) {
            createdBy = getProperties().getCreatedBy();
        }
        return createdBy != null ? createdBy : "--";
    }

    public String getLastModified() {
        if (lastModified == null) {
            lastModified = getProperties().getLastModified();
        }
        return lastModified != null ? format(lastModified) : "--";
    }

    public String getLastModifiedBy() {
        if (lastModifiedBy == null) {
            lastModifiedBy = getProperties().getLastModifiedBy();
        }
        return lastModifiedBy != null ? lastModifiedBy : "--";
    }

    public String getLastWrapped() {
        if (lastWrapped == null) {
            lastWrapped = getProperties().getLastWrapped();
        }
        return lastWrapped != null ? format(lastWrapped) : "--";
    }

    public String getLastWrappedBy() {
        if (lastWrappedBy == null) {
            lastWrappedBy = getProperties().getLastWrappedBy();
        }
        return lastWrappedBy != null ? lastWrappedBy : "--";
    }

    public List<PathFilterSet> getFilterList() {
        if (filterList == null) {
            filterList = pckg.getWorkspaceFilter().getFilterSets();
        }
        return filterList;
    }

    public List<Dependency> getDependencies() {
        if (dependencies == null) {
            dependencies = Arrays.asList(pckg.getDependencies());
        }
        return dependencies;
    }

    public String getThumbnailUrl() {
        if (thumbnailUrl == null) {
            // FIXME package thumbnail
            thumbnailUrl = "";
        }
        return thumbnailUrl;
    }

    public String getAuditLogUrl() {
        if (auditLogUrl == null) {
            auditLogUrl = LinkUtil.getUrl(getRequest(), AUDIT_LOG_BASE + getPath());
        }
        return auditLogUrl;
    }

    public String getAcHandling() {
        AccessControlHandling acHandling = getProperties().getACHandling();
        return acHandling != null ? acHandling.name() : "";
    }

    public String getAcHandlingLabel() {
        AccessControlHandling acHandling = getProperties().getACHandling();
        return acHandling != null ? acHandling.name() : "-- -- ";
    }

    public boolean getRequiresRestart() {
        return RegistryUtil.booleanProperty(getProperties(), PackageUtil.DEF_REQUIRES_RESTART, false);
    }

    public boolean getRequiresRoot() {
        return getProperties().requiresRoot();
    }

    public String getProviderName() {
        return getProperties().getProperty(PackageUtil.DEF_PROVIDER_NAME);
    }

    public String getProviderUrl() {
        return getProperties().getProperty(PackageUtil.DEF_PROVIDER_URL);
    }

    public String getProviderLink() {
        return getProperties().getProperty(PackageUtil.DEF_PROVIDER_LINK);
    }

    @Nonnull
    public PackageProperties getProperties() {
        if (properties == null) {
            try {
                properties = pckg.getPackageProperties();
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                properties = new PackageProperties() {

                    @Override
                    public PackageId getId() {
                        return pckg.getId();
                    }

                    @Override
                    public Calendar getLastModified() {
                        return pckg.getInstallationTime();
                    }

                    @Override
                    public String getLastModifiedBy() {
                        return null;
                    }

                    @Override
                    public Calendar getCreated() {
                        return pckg.getInstallationTime();
                    }

                    @Override
                    public String getCreatedBy() {
                        return null;
                    }

                    @Override
                    public Calendar getLastWrapped() {
                        return pckg.getInstallationTime();
                    }

                    @Override
                    public String getLastWrappedBy() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public boolean requiresRoot() {
                        return false;
                    }

                    @Override
                    public Dependency[] getDependencies() {
                        return new Dependency[0];
                    }

                    @Override
                    public AccessControlHandling getACHandling() {
                        return null;
                    }

                    @Override
                    public SubPackageHandling getSubPackageHandling() {
                        return null;
                    }

                    @Override
                    public Calendar getDateProperty(String name) {
                        return null;
                    }

                    @Override
                    public String getProperty(String name) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public PackageType getPackageType() {
                        return null;
                    }
                };
            }
        }
        return properties;
    }
}
