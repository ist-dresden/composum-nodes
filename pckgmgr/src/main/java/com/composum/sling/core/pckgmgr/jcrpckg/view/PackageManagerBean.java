package com.composum.sling.core.pckgmgr.jcrpckg.view;

import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.pckgmgr.Packages;
import com.composum.sling.core.pckgmgr.Packages.Mode;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.JcrPackageItem;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeItem;
import com.composum.sling.core.pckgmgr.jcrpckg.tree.TreeNode;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class PackageManagerBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PackageManagerBean.class);

    private transient String path;
    private transient PackageUtil.ViewType type;
    private transient String downloadUrl;

    @Override
    public String getPath() {
        if (path == null) {
            path = PackageUtil.getPath(getRequest());
        }
        return path;
    }

    public Mode getMode() {
        return Packages.getMode(getRequest());
    }

    public String getViewType() {
        if (type == null) {
            type = PackageUtil.getViewType(context, getRequest(), getPath());
        }
        return type != null ? type.name() : "";
    }

    public String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "general";
    }

    public List<JcrPackageItem> getCurrentGroupPackages() {
        List<JcrPackageItem> items = new ArrayList<>();
        try {
            JcrPackageManager manager = PackageUtil.getPackageManager(context.getService(Packaging.class), getRequest());
            TreeNode treeNode = PackageUtil.getTreeNode(manager, getRequest());
            for (TreeItem item : treeNode) {
                if (item instanceof JcrPackageItem) {
                    if (((JcrPackageItem) item).getDefinition() != null) {
                        items.add((JcrPackageItem) item);
                    }
                }
            }
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return items;
    }
}
