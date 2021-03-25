package com.composum.sling.core.setup.hook;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHook;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupHook implements InstallHook {

    private static final Logger LOG = LoggerFactory.getLogger(SetupHook.class);

    public static final String NAME_COMPOSUM = "composum";
    public static final String NAME_CLIENTLIBS = "clientlibs";

    public static final String VAR_PATH = "/var";
    public static final String VAR_COMPOSUM = VAR_PATH + "/" + NAME_COMPOSUM;
    public static final String VAR_CLIENTLIBS = VAR_COMPOSUM + "/" + NAME_CLIENTLIBS;

    public static final String VAR_CACHE = VAR_PATH + "/cache";
    public static final String OLD_CLIENTLIBS = VAR_CACHE + "/" + NAME_CLIENTLIBS;

    public static final String TYPE_FOLDER = "sling:Folder";

    public static final String INSTALL_EXT = ".install";
    public static final String BUNDLES_PATH = "/libs/composum/nodes/install";
    public static final String PCKGMGR_FOLDER = "/libs/composum/nodes/pckgmgr";
    public static final String PCKGMGR_INSTALL_FOLDER = PCKGMGR_FOLDER + INSTALL_EXT;
    public static final Pattern PCKGMGR_BUNDLE = Pattern.compile("^composum-nodes-pckgmgr-(.+)\\.jar$");

    @Override
    public void execute(InstallContext ctx) throws PackageException {
        //noinspection SwitchStatementWithTooFewBranches
        switch (ctx.getPhase()) {
            case INSTALLED:
                LOG.info("installed: execute...");
                moveClientlibsRoot(ctx);
                setupPackageManager(ctx);
                LOG.info("installed: execute ends.");
                break;
        }
    }

    protected void moveClientlibsRoot(InstallContext ctx) throws PackageException {
        try {
            Session session = ctx.getSession();
            try {
                session.getNode(VAR_CLIENTLIBS);
            } catch (PathNotFoundException rnf) {
                LOG.info("move clientlibs root...");
                // make changes only if new clientlibs root doesn't exist
                // move clientlibs from '/var/cache'
                Node var;
                try {
                    var = session.getNode(VAR_COMPOSUM);
                } catch (PathNotFoundException vnf) {
                    var = session.getNode(VAR_PATH);
                    var = var.addNode(NAME_COMPOSUM, TYPE_FOLDER);
                    session.save();
                }
                try {
                    Node clientlibs = session.getNode(OLD_CLIENTLIBS);
                    LOG.info("move from '" + VAR_CACHE + "' to '" + VAR_COMPOSUM + "'");
                    // move clientlibs root to new path
                    session.move(OLD_CLIENTLIBS, VAR_CLIENTLIBS);
                    Node cache = session.getNode(VAR_CACHE);
                    if (!cache.getNodes().hasNext()) {
                        LOG.info("delete '" + VAR_CACHE + "'");
                        cache.remove(); // remove cache folder if empty
                    }
                    session.save();
                } catch (PathNotFoundException onf) {
                    // create clientlibs root
                    var.addNode(NAME_CLIENTLIBS, TYPE_FOLDER);
                    session.save();
                }
            }
        } catch (RepositoryException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new PackageException(ex);
        }
    }

    protected void setupPackageManager(InstallContext ctx) throws PackageException {
        Session session = ctx.getSession();
        try {
            // switch to new package manager bundle
            Node newPckgMgrFolder = session.getNode(PCKGMGR_INSTALL_FOLDER);
            Node newPckgMgrBundle = null;
            NodeIterator iterator = newPckgMgrFolder.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                Matcher matcher = PCKGMGR_BUNDLE.matcher(node.getName());
                if (matcher.matches()) {
                    newPckgMgrBundle = node;
                    break;
                }
            }
            if (newPckgMgrBundle != null) {
                LOG.info("installed: install Package Manager bundle ({})", newPckgMgrBundle.getPath());
                try {
                    Node bundlesFolder = session.getNode(BUNDLES_PATH);
                    try {
                        iterator = bundlesFolder.getNodes();
                        while (iterator.hasNext()) {
                            Node oldPckgMgrBundle = iterator.nextNode();
                            Matcher matcher = PCKGMGR_BUNDLE.matcher(oldPckgMgrBundle.getName());
                            if (matcher.matches()) {
                                LOG.info("installed: remove Package Manager bundle ({})", oldPckgMgrBundle.getPath());
                                oldPckgMgrBundle.remove();
                                session.save();
                            }
                        }
                    } finally {
                        String newBundlePath = BUNDLES_PATH + "/" + newPckgMgrBundle.getName();
                        LOG.info("installed: move Package Manager bundle ({}): '{}'", newPckgMgrBundle.getPath(), newBundlePath);
                        session.move(newPckgMgrBundle.getPath(), newBundlePath);
                        session.save();
                    }
                } catch (PathNotFoundException ignore) {
                }
            }
            try {
                Node oldPckgMgrFolder = session.getNode(PCKGMGR_FOLDER);
                oldPckgMgrFolder.remove();
                session.save();
            } catch (PathNotFoundException ignore) {
            } finally {
                session.move(newPckgMgrFolder.getPath(), PCKGMGR_FOLDER);
                session.save();
            }
        } catch (PathNotFoundException ignore) {
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new PackageException(ex);
        }
    }
}
