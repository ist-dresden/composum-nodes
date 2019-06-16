package com.composum.sling.core.setup.hook;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHook;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

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

    @Override
    public void execute(InstallContext ctx) throws PackageException {
        //noinspection SwitchStatementWithTooFewBranches
        switch (ctx.getPhase()) {
            case INSTALLED:
                LOG.info("installed: execute...");
                moveClientlibsRoot(ctx);
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
                Workspace workspace = session.getWorkspace();
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
                    workspace.move(OLD_CLIENTLIBS, VAR_CLIENTLIBS);
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
}
