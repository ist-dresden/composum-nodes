package com.composum.sling.core.osgi.pckginstall;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.UNINSTALLED;

/**
 * Deinstalls the obsolete Core V1 bundle if it's still present.
 */
public class PackageTransformerBundleActivator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(PackageTransformerBundleActivator.class);

    @Override
    public void start(BundleContext bundleContext) {
        LOG.info("starting");
        for (Bundle bundle : bundleContext.getBundles()) {
            if ("com.composum.core.pckginstall".equals(bundle.getSymbolicName()) &&
                    bundle.getLocation().matches(".*composum-sling-osgi-package-installer-.*")) {
                LOG.info("Found obsolete bundle: {} at {} state {}", bundle.getSymbolicName(), bundle.getLocation(), bundle.getState());
                int state = bundle.getState();
                if (state != UNINSTALLED) {
                    try {
                        if (state == Bundle.ACTIVE || state == STARTING) {
                            bundle.stop();
                        }
                    } catch (BundleException | RuntimeException ex) {
                        LOG.error("Trouble stopping bundle {}", bundle.getLocation(), ex);
                    }
                    try {
                        bundle.uninstall();
                    } catch (BundleException | RuntimeException ex) {
                        LOG.error("Trouble uninstalling bundle {}", bundle.getLocation(), ex);
                    }
                    LOG.info("Uninstall {} done. (state {})", bundle.getLocation(), bundle.getState());
                }
            }
        }
    }

    @Override
    public void stop(BundleContext context) {
        // nothing to do
    }
}
