package com.composum.sling.core.setup.util;

import com.composum.sling.core.usermanagement.core.UserManagementService;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

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
}
