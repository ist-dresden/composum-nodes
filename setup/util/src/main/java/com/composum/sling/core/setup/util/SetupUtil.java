package com.composum.sling.core.setup.util;

import com.composum.sling.core.usermanagement.core.UserManagementService;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SetupUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SetupUtil.class);

    /**
     * Creates or updates a set of groups, users and system users
     *
     * @param ctx         the installation context
     * @param groups      map of group names (including path, e.g. composum/plaform/composum-platform-users) to list of
     *                    users (have to exist already)
     * @param systemUsers map of system user names (including path, e.g. system/composum/platform/composum-platform-service)
     *                    to list of group names (without path). The groups have to exist already, or be created with parameter groups
     * @param users       map of user names (including path) to list of group names (without path). The groups
     *                    have to exist already, or be created with parameter groups
     * @throws PackageException
     */
    public static void setupGroupsAndUsers(InstallContext ctx,
                                           Map<String, List<String>> groups,
                                           Map<String, List<String>> systemUsers,
                                           Map<String, List<String>> users) throws PackageException {
        UserManagementService userManagementService = getService(UserManagementService.class);
        try {
            JackrabbitSession session = (JackrabbitSession) ctx.getSession();
            UserManager userManager = session.getUserManager();
            if (groups != null) {
                for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                    Group group = userManagementService.getOrCreateGroup(session, userManager, entry.getKey());
                    if (group != null) {
                        for (String memberName : entry.getValue()) {
                            userManagementService.assignToGroup(session, userManager, memberName, group);
                        }
                    }
                }
                session.save();
            }
            if (systemUsers != null) {
                for (Map.Entry<String, List<String>> entry : systemUsers.entrySet()) {
                    Authorizable user = userManagementService.getOrCreateUser(session, userManager, entry.getKey(), true);
                    if (user != null) {
                        for (String groupName : entry.getValue()) {
                            userManagementService.assignToGroup(session, userManager, user, groupName);
                        }
                    }
                }
                session.save();
            }
            if (users != null) {
                for (Map.Entry<String, List<String>> entry : users.entrySet()) {
                    Authorizable user = userManagementService.getOrCreateUser(session, userManager, entry.getKey(), false);
                    if (user != null) {
                        for (String groupName : entry.getValue()) {
                            userManagementService.assignToGroup(session, userManager, user, groupName);
                        }
                    }
                }
                session.save();
            }
        } catch (RepositoryException | RuntimeException rex) {
            LOG.error(rex.getMessage(), rex);
            throw new PackageException(rex);
        }
    }

    /**
     * check a list of bundles (symbolic name, version) for the right version and active state
     *
     * @param ctx                the package install context
     * @param bundlesToCheck     the 'list' to check; key: symbolic name, value: version
     * @param waitToStartSeconds the seconds to wait to check start; must be greater than 1
     * @param timeoutSeconds     the timeout for the bundle check in seconds
     * @throws PackageException if the check fails
     */
    public static void checkBundles(InstallContext ctx, Map<String, String> bundlesToCheck,
                                    int waitToStartSeconds, int timeoutSeconds)
            throws PackageException {
        try {
            // wait to give the bundle installer a chance to install bundles
            Thread.sleep(waitToStartSeconds * 1000);
        } catch (InterruptedException ignore) {
        }
        LOG.info("Check bundles...");
        BundleContext bundleContext = FrameworkUtil.getBundle(ctx.getSession().getClass()).getBundleContext();
        int ready = 0;
        for (int i = 0; i < timeoutSeconds; i++) {
            ready = 0;
            for (Bundle bundle : bundleContext.getBundles()) {
                String version = bundlesToCheck.get(bundle.getSymbolicName());
                if (version != null && version.equals(bundle.getVersion().toString())
                        && bundle.getState() == Bundle.ACTIVE) {
                    ready++;
                }
            }
            if (ready == bundlesToCheck.size()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if (ready < bundlesToCheck.size()) {
            LOG.error("Checked bundles not ready - installation failed!");
            throw new PackageException("bundles not ready");
        } else {
            LOG.info("Checked bundles are up and ready.");
        }
    }

    /**
     * retrieve a service during setup
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> type) {
        Bundle serviceBundle = FrameworkUtil.getBundle(type);
        BundleContext serviceBundleContext = serviceBundle.getBundleContext();
        ServiceReference serviceReference = serviceBundleContext.getServiceReference(type.getName());
        return (T) serviceBundleContext.getService(serviceReference);
    }

    /**
     * Updates existing nodetypes. This might be neccesary for changes in nodetypes.cnd, since the normal package installation does not change
     * existing node types. It's probably sensible to do this only after checking that the current definition in the
     * {@link javax.jcr.nodetype.NodeTypeManager} of the changed node type is not current.
     */
    public static void updateNodeTypes(InstallContext ctx) throws PackageException {
        Archive archive = ctx.getPackage().getArchive();
        try {
            try (InputStream stream = archive.openInputStream(archive.getEntry("/META-INF/vault/nodetypes.cnd"))) {
                InputStreamReader cndReader = new InputStreamReader(stream);
                CndImporter.registerNodeTypes(cndReader, ctx.getSession(), true);
            }
        } catch (ParseException | RepositoryException | IOException e) {
            LOG.error("Failed to update node types.", e);
            throw new PackageException("Failed to update node types.", e);
        }
    }

    /**
     * Updates existing nodetypes if an up to date check fails. This might be neccesary for changes in nodetypes.cnd, since the normal package installation does not change
     * existing node types.
     *
     * @param upToDateCheck if this does return true, the update is skipped. Probably involves <code>ctx.getSession().getWorkspace().getNodeTypeManager()</code>.
     */
    public static void updateNodeTypesConditionally(InstallContext ctx, Callable<Boolean> upToDateCheck) throws PackageException {
        try {
            if (!Boolean.TRUE.equals(upToDateCheck.call())) {
                LOG.info("up to date check failed - updating node types.");

                updateNodeTypes(ctx);

                if (!Boolean.TRUE.equals(upToDateCheck.call())) {
                    LOG.error("up to date check still fails even after package installation!");
                    // That's probably a bug or obsolete check - we do not throw up here since this is likely not a showstopper
                }
            }
        } catch (PackageException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error during current check", e);
        }
    }

}
