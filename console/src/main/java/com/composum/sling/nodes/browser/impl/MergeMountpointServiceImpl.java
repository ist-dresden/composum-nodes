package com.composum.sling.nodes.browser.impl;

import com.composum.sling.nodes.browser.MergeMountpointService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.composum.sling.core.util.CoreConstants.DEFAULT_OVERLAY_ROOT;
import static com.composum.sling.core.util.CoreConstants.DEFAULT_OVERRIDE_ROOT;

@Component(service = MergeMountpointService.class)
public class MergeMountpointServiceImpl implements MergeMountpointService {

    private static final Logger LOG = LoggerFactory.getLogger(MergeMountpointServiceImpl.class);

    private BundleContext bundleContext;

    private volatile String overrideMountPoint;
    private volatile String overlayMountPoint;

    @Activate
    @Modified
    protected void activate(final BundleContext bundleContext) throws InvalidSyntaxException {
        this.bundleContext = bundleContext;
        overlayMountPoint = null;
        overrideMountPoint = null;
        for (ServiceReference<ResourceProvider> sr : bundleContext.getServiceReferences(ResourceProvider.class, "(provider.name=Merging)")) {
            String root = (String) sr.getProperty(ResourceProvider.PROPERTY_ROOT);
            // Unfortunately it doesn't seem possible to distinguish those other than guessing :-(
            if (StringUtils.contains(root, "overlay")) {
                overlayMountPoint = root;
            } else if (StringUtils.contains(root, "override")) {
                overrideMountPoint = root;
            }
        }
    }

    private void init(ResourceResolver resolver) {
        if (overlayMountPoint != null) {
            return;
        }
        try {
            List<String> mountpoints = bundleContext.getServiceReferences(ResourceProvider.class, "(provider.name=Merging)").stream()
                    .map(sr -> (String) sr.getProperty(ResourceProvider.PROPERTY_ROOT)).collect(Collectors.toList());
            // Unfortunately I don't see an easy way to determine which one is which one from the service reference nor from the provider.
            // so we try to distinguish them from their content: the override contains subresources according to the search path,
            // but overlay doesn't.
            overrideMountPoint = mountpoints.stream()
                    .filter(m -> isOverrideMountpoint(m, resolver))
                    .findFirst().orElse(DEFAULT_OVERRIDE_ROOT);
            mountpoints.remove(overrideMountPoint);
            overlayMountPoint = mountpoints.isEmpty() ? DEFAULT_OVERLAY_ROOT : mountpoints.get(0);
        } catch (InvalidSyntaxException e) { // bug.
            LOG.error("" + e, e);
        }
    }

    private boolean isOverrideMountpoint(String mountpoint, ResourceResolver resolver) {
        boolean hasMissingEntry = Arrays.asList(resolver.getSearchPath()).stream()
                .map(p -> resolver.getResource(mountpoint + p))
                .filter(Objects::isNull)
                .findAny().isPresent();
        return !hasMissingEntry;
    }

    @Override
    public String overrideMergeMountPoint(ResourceResolver resolver) {
        init(resolver);
        return StringUtils.defaultString(overrideMountPoint, DEFAULT_OVERRIDE_ROOT);
    }

    @Override
    public String overlayMergeMountPoint(ResourceResolver resolver) {
        init(resolver);
        return StringUtils.defaultString(overlayMountPoint, DEFAULT_OVERLAY_ROOT);
    }
}
