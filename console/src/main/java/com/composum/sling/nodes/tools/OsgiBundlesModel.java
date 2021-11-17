package com.composum.sling.nodes.tools;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.Restricted;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Restricted(key = OsgiBundlesServlet.SERVICE_KEY)
public class OsgiBundlesModel extends AbstractSlingBean {

    private transient Map<String, OsgiBundleModel> bundles;
    private transient BundleContext bundleContext;

    public int getCountTotal() {
        return getBundles().size();
    }

    public int getCountActive() {
        int active = 0;
        for (OsgiBundleModel bundle : getBundles()) {
            if (bundle.isActive()) {
                active++;
            }
        }
        return active;
    }

    @NotNull
    public Collection<OsgiBundleModel> getBundles() {
        if (bundles == null) {
            bundles = new LinkedHashMap<>();
            for (final Bundle bundle : getBundleContext().getBundles()) {
                final OsgiBundleModel model = new OsgiBundleModel(context, bundle);
                bundles.put(model.getSymbolicName(), model);
            }
        }
        return bundles.values();
    }

    protected BundleContext getBundleContext() {
        if (bundleContext == null) {
            bundleContext = FrameworkUtil.getBundle(BeanContext.class).getBundleContext();
        }
        return bundleContext;
    }

    public OsgiBundlesModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public OsgiBundlesModel(BeanContext context) {
        super(context);
    }

    public OsgiBundlesModel() {
        super();
    }
}
