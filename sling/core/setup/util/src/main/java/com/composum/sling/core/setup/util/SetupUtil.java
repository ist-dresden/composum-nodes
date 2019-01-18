package com.composum.sling.core.setup.util;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SetupUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SetupUtil.class);

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
