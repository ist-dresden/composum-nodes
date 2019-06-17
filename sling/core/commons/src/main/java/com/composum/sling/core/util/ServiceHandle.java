package com.composum.sling.core.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * a service handler to enable static access to service instances
 */
public class ServiceHandle<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHandle.class);

    public static final long WAIT_TIMEOUT = 5 /* sec */ * 1000L;

    protected final Class<T> type;

    protected transient T service;

    protected transient InstanceTracker serviceTracker;

    @Nonnull
    public T getService() {
        if (service == null) {
            if (serviceTracker == null) {
                Bundle serviceBundle = FrameworkUtil.getBundle(type);
                if (serviceBundle != null) {
                    BundleContext bundleContext = serviceBundle.getBundleContext();
                    ServiceReference<T> serviceReference = bundleContext.getServiceReference(type);
                    if (serviceReference != null) {
                        serviceTracker = new InstanceTracker(bundleContext, serviceReference);
                        serviceTracker.open();
                    }
                }
            }
            if (serviceTracker != null) {
                service = serviceTracker.waitForService();
            }
            Objects.requireNonNull(service);
        }
        return service;
    }

    /**
     * @param type the concrete service type instance
     */
    public ServiceHandle(@Nonnull final Class<T> type) {
        this.type = type;
    }

    protected class InstanceTracker extends ServiceTracker<T, T> {

        public InstanceTracker(BundleContext context, ServiceReference<T> reference) {
            super(context, reference, null);
        }

        @Override
        public T addingService(ServiceReference<T> reference) {
            service = super.addingService(reference);
            return service;
        }

        @Override
        public void removedService(ServiceReference<T> reference, T service) {
            super.removedService(reference, service);
            ServiceHandle.this.service = null;
            ServiceHandle.this.serviceTracker = null; // the ServiceReference is probably invalid now.
            close();
        }

        public T waitForService() {
            try {
                return waitForService(WAIT_TIMEOUT);
            } catch (InterruptedException tmex) {
                LOG.error("timeout on wait for service '{}' ({})", type.getName(), tmex.toString());
                ServiceHandle.this.serviceTracker = null; // not clear whether the service reference is still valid. There were cases where this never recovers.
                close();
                return null;
            }
        }
    }
}
