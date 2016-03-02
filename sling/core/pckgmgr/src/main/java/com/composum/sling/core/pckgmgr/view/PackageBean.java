package com.composum.sling.core.pckgmgr.view;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.pckgmgr.util.PackageUtil;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PackageBean extends AbstractSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PackageBean.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private transient JcrPackageManager pckgMmgr;
    private transient JcrPackage pckg;
    private transient JcrPackageDefinition pckgDef;

    private transient String group;
    private transient String name;
    private transient String version;

    private transient String filename;
    private transient String downloadUrl;

    private transient Calendar created;
    private transient String createdBy;
    private transient Calendar lastModified;
    private transient String lastModifiedBy;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        SlingHttpServletRequest request = context.getRequest();
        String path = PackageUtil.getPath(request);
        try {
            resource = PackageUtil.getResource(request, path);
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        super.initialize(context, resource);
    }

    protected JcrPackageManager getPckgMgr() {
        if (pckgMmgr == null) {
            pckgMmgr = PackageUtil.createPackageManager(getRequest());
        }
        return pckgMmgr;
    }

    protected JcrPackage getPckg() {
        if (pckg == null) {
            try {
                pckg = getPckgMgr().open(getNode());
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return pckg;
    }

    protected JcrPackageDefinition getPckgDef() {
        if (pckgDef == null) {
            getPckg();
            if (pckg != null) {
                try {
                    pckgDef = pckg.getDefinition();
                } catch (RepositoryException rex) {
                    LOG.error(rex.getMessage(), rex);
                }
            }
        }
        return pckgDef;
    }

    protected String format(Calendar date) {
        return date != null ? new SimpleDateFormat(DATE_FORMAT).format(date.getTime()) : "";
    }

    public String getCssClasses() {
        StringBuilder cssClasses = new StringBuilder();
        try {
            getPckg();
            if (pckg.isInstalled()) {
                addCssClass(cssClasses, "installed");
            }
            if (!pckg.isSealed()) {
                addCssClass(cssClasses, "modified");
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return cssClasses.toString();
    }

    public String getPath() {
        JcrPackageManager pckgMgr = getPckgMgr();
        JcrPackage pckg = getPckg();
        String path = PackageUtil.getPackagePath(pckgMgr, pckg);
        return path;
    }

    public String getGroup() {
        if (group == null) {
            group = PackageUtil.getDefAttr(getPckgDef(), JcrPackageDefinition.PN_GROUP, "");
        }
        return group;
    }

    public String getName() {
        if (name == null) {
            name = PackageUtil.getDefAttr(getPckgDef(), JcrPackageDefinition.PN_NAME, "");
        }
        return name;
    }

    public String getVersion() {
        if (version == null) {
            version = PackageUtil.getDefAttr(getPckgDef(), JcrPackageDefinition.PN_VERSION, "");
        }
        return version;
    }

    public String getFilename() {
        if (filename == null) {
            filename = PackageUtil.getFilename(getPckg());
        }
        return filename;
    }

    public String getDownloadUrl() {
        if (downloadUrl == null) {
            downloadUrl = PackageUtil.getDownloadUrl(getPckg());
        }
        return downloadUrl;
    }

    public String getCreated() {
        if (created == null) {
            created = PackageUtil.getCreated(getPckg());
        }
        return format(created);
    }

    public String getCreatedBy() {
        if (createdBy == null) {
            createdBy = PackageUtil.getCreatedBy(getPckg());
        }
        return createdBy;
    }

    public String getLastModified() {
        if (lastModified == null) {
            lastModified = PackageUtil.getLastModified(getPckg());
        }
        return format(lastModified);
    }

    public String getLastModifiedBy() {
        if (lastModifiedBy == null) {
            lastModifiedBy = PackageUtil.getLastModifiedBy(getPckg());
        }
        return lastModifiedBy;
    }
}
