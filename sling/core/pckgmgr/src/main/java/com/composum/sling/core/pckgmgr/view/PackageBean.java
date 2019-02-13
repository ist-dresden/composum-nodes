package com.composum.sling.core.pckgmgr.view;

import com.composum.sling.core.BeanContext;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import com.composum.sling.core.pckgmgr.PackageJobExecutor;
import com.composum.sling.core.pckgmgr.util.PackageUtil;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.composum.sling.core.util.LinkUtil.EXT_HTML;

public class PackageBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PackageBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String AUDIT_LOG_BASE = "/bin/browser.html" + PackageJobExecutor.AUDIT_BASE_PATH;

    protected String path;
    protected JcrPackageManager pckgMgr;
    protected JcrPackage pckg;
    protected JcrPackageDefinition pckgDef;

    private transient String group;
    private transient String name;
    private transient String version;
    private transient String description;

    private transient String filename;
    private transient String downloadUrl;
    private transient String auditLogUrl;

    private transient Calendar created;
    private transient String createdBy;
    private transient Calendar lastModified;
    private transient String lastModifiedBy;
    private transient Calendar lastUnpacked;
    private transient String lastUnpackedBy;
    private transient Calendar lastUnwrapped;
    private transient String lastUnwrappedBy;
    private transient Calendar lastWrapped;
    private transient String lastWrappedBy;

    private transient List<PathFilterSet> filterList;

    private transient String thumbnailUrl;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        SlingHttpServletRequest request = context.getRequest();
        path = PackageUtil.getPath(request);
        try {
            pckgMgr = PackageUtil.getPackageManager(context.getService(Packaging.class), request);
            resource = PackageUtil.getResource(pckgMgr, request, path);
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        super.initialize(context, resource);
        try {
            pckg = pckgMgr.open(getNode());
            if (pckg != null) {
                pckgDef = pckg.getDefinition();
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
    }

    protected String format(Calendar date) {
        return date != null ? new SimpleDateFormat(DATE_FORMAT).format(date.getTime()) : "";
    }

    public String getCssClasses() {
        return StringUtils.join(collectCssClasses(new ArrayList<String>()), " ");
    }

    protected List<String> collectCssClasses(List<String> collection) {
        try {
            if (pckg != null) {
                if (pckg.isInstalled()) {
                    collection.add("installed");
                }
                if (pckg.isSealed()) {
                    collection.add("sealed");
                }
                if (pckg.isValid()) {
                    collection.add("valid");
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return collection;
    }


    @Override
    public String getUrl() {
        return LinkUtil.getUrl(getRequest(), PackagesServlet.SERVLET_PATH + EXT_HTML + getPath());
    }

    @Override
    public String getPath() {
        String path = PackageUtil.getPackagePath(pckgMgr, pckg);
        return path;
    }

    public String getGroup() {
        if (group == null) {
            group = PackageUtil.getDefAttr(pckgDef, JcrPackageDefinition.PN_GROUP, "");
            if (StringUtils.isBlank(group)) {
                int lastSlash;
                if (path != null && (lastSlash = path.lastIndexOf('/')) >= 0) {
                    group = path.substring(0, lastSlash);
                }
            }
        }
        return group;
    }

    @Override
    public String getName() {
        if (name == null) {
            name = PackageUtil.getDefAttr(pckgDef, JcrPackageDefinition.PN_NAME, "");
            if (StringUtils.isBlank(name)) {
                if (path != null) {
                    name = path.substring(path.lastIndexOf('/') + 1);
                }
            }
        }
        return name;
    }

    public String getVersion() {
        if (version == null) {
            version = PackageUtil.getDefAttr(pckgDef, JcrPackageDefinition.PN_VERSION, "");
        }
        return version;
    }

    public String getDescription() {
        if (description == null) {
            if (pckgDef != null) {
                description = pckgDef.getDescription();
            }
            if (description == null) {
                description = "";
            }
        }
        return description;
    }

    public String getFilename() {
        if (filename == null) {
            filename = PackageUtil.getFilename(pckg);
        }
        return filename;
    }

    public String getDownloadUrl() {
        if (downloadUrl == null) {
            downloadUrl = LinkUtil.getUrl(getRequest(), PackageUtil.getDownloadUrl(pckg));
        }
        return downloadUrl;
    }

    public String getCreated() {
        if (created == null) {
            created = PackageUtil.getCreated(pckg);
        }
        return created != null ? format(created) : "--";
    }

    public String getCreatedBy() {
        if (createdBy == null) {
            createdBy = PackageUtil.getCreatedBy(pckg);
        }
        return createdBy != null ? createdBy : "--";
    }

    public String getLastModified() {
        if (lastModified == null) {
            lastModified = PackageUtil.getLastModified(pckg);
        }
        return lastModified != null ? format(lastModified) : "--";
    }

    public String getLastModifiedBy() {
        if (lastModifiedBy == null) {
            lastModifiedBy = PackageUtil.getLastModifiedBy(pckg);
        }
        return lastModifiedBy != null ? lastModifiedBy : "--";
    }

    public String getLastUnpacked() {
        if (lastUnpacked == null) {
            if (pckgDef != null) {
                lastUnpacked = pckgDef.getLastUnpacked();
            }
        }
        return lastUnpacked != null ? format(lastUnpacked) : "--";
    }

    public String getLastUnpackedBy() {
        if (lastUnpackedBy == null) {
            if (pckgDef != null) {
                lastUnpackedBy = pckgDef.getLastUnpackedBy();
            }
        }
        return lastUnpackedBy != null ? lastUnpackedBy : "--";
    }

    public String getLastUnwrapped() {
        if (lastUnwrapped == null) {
            if (pckgDef != null) {
                lastUnwrapped = pckgDef.getLastUnwrapped();
            }
        }
        return lastUnwrapped != null ? format(lastUnwrapped) : "--";
    }

    public String getLastUnwrappedBy() {
        if (lastUnwrappedBy == null) {
            if (pckgDef != null) {
                lastUnwrappedBy = pckgDef.getLastUnwrappedBy();
            }
        }
        return lastUnwrappedBy != null ? lastUnwrappedBy : "--";
    }

    public String getLastWrapped() {
        if (lastWrapped == null) {
            if (pckgDef != null) {
                lastWrapped = pckgDef.getLastWrapped();
            }
        }
        return lastWrapped != null ? format(lastWrapped) : "--";
    }

    public String getLastWrappedBy() {
        if (lastWrappedBy == null) {
            if (pckgDef != null) {
                lastWrappedBy = pckgDef.getLastWrappedBy();
            }
        }
        return lastWrappedBy != null ? lastWrappedBy : "--";
    }

    public List<PathFilterSet> getFilterList() {
        if (filterList == null) {
            try {
                return PackageUtil.getFilterList(pckgDef);
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
            filterList = new ArrayList<>();
        }
        return filterList;
    }

    public String getThumbnailUrl() {
        if (thumbnailUrl == null) {
            StringBuilder builder = new StringBuilder();
            try {
                String path = PackageUtil.getThumbnailPath(pckgDef);
                if (StringUtils.isNotBlank(path)) {
                    builder.append(LinkUtil.getUrl(request, path));
                }
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
            thumbnailUrl = builder.toString();
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
        AccessControlHandling acHandling = pckgDef.getAccessControlHandling();
        return acHandling != null ? acHandling.name() : "";
    }

    public String getAcHandlingLabel() {
        AccessControlHandling acHandling = pckgDef.getAccessControlHandling();
        return acHandling != null ? acHandling.name() : "-- -- ";
    }

    public boolean getRequiresRestart() {
        return pckgDef.getBoolean(PackageUtil.DEF_REQUIRES_RESTART);
    }

    public boolean getRequiresRoot() {
        return pckgDef.getBoolean(PackageUtil.DEF_REQUIRES_ROOT);
    }

    public String getProviderName() {
        return pckgDef.get(PackageUtil.DEF_PROVIDER_NAME);
    }

    public String getProviderUrl() {
        return pckgDef.get(PackageUtil.DEF_PROVIDER_URL);
    }

    public String getProviderLink() {
        return pckgDef.get(PackageUtil.DEF_PROVIDER_LINK);
    }

    public String[] getDependencies() {
        return PackageUtil.getMultiProperty(pckgDef, PackageUtil.DEF_DEPENDENCIES);
    }

    public String[] getReplaces() {
        return PackageUtil.getMultiProperty(pckgDef, PackageUtil.DEF_REPLACES);
    }
}
