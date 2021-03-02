package com.composum.sling.nodes.mount.remote;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.composum.sling.nodes.mount.remote.RemoteClientBuilder.ASPECT_KEY;

@Component
public class RemoteClientSetupImpl implements RemoteClientSetup {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteClientSetupImpl.class);

    private BundleContext bundleContext;

    private final Map<String, ClientBuilderSet> clientBuilders = new HashMap<>();

    @Override
    @Nonnull
    public Set<RemoteClientBuilder> getBuilders(@Nonnull final Collection<String> aspectKeys) {
        Set<RemoteClientBuilder> result = new LinkedHashSet<>();
        for (String key : aspectKeys) {
            if (StringUtils.isNotBlank(key)) {
                ClientBuilderSet set = clientBuilders.get(key);
                if (set != null && set.size() > 0) {
                    RemoteClientBuilder service = bundleContext.getService(set.iterator().next());
                    if (service != null) {
                        result.add(service);
                    }
                }
            }
        }
        return result;
    }

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Reference(
            service = RemoteClientBuilder.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void bindRemoteClientBuilder(ServiceReference<RemoteClientBuilder> reference) {
        String aspectKey = (String) reference.getProperty(ASPECT_KEY);
        LOG.info("bindRemoteClientBuilder({}): '{}'", reference, aspectKey);
        ClientBuilderSet set = clientBuilders.get(aspectKey);
        if (set == null) {
            set = new ClientBuilderSet();
            clientBuilders.put(aspectKey, set);
        }
        set.add(reference);
    }

    protected void unbindRemoteClientBuilder(ServiceReference<RemoteClientBuilder> reference) {
        String aspectKey = (String) reference.getProperty(ASPECT_KEY);
        LOG.info("unbindRemoteClientBuilder({}): '{}'", reference, aspectKey);
        ClientBuilderSet set = clientBuilders.get(aspectKey);
        if (set != null) {
            set.remove(reference);
        }
    }

    protected class ClientBuilderSet extends TreeSet<ServiceReference<RemoteClientBuilder>> {

        public ClientBuilderSet() {
            super(new Comparator<ServiceReference<RemoteClientBuilder>>() {

                protected Integer ranking(ServiceReference<RemoteClientBuilder> reference) {
                    return (Integer) reference.getProperty(Constants.SERVICE_RANKING);
                }

                @Override
                public int compare(ServiceReference<RemoteClientBuilder> o1,
                                   ServiceReference<RemoteClientBuilder> o2) {
                    return ranking(o2).compareTo(ranking(o1)); // reverse order
                }
            });
        }
    }
}
