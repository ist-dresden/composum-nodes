package com.composum.sling.core.resource;

import com.composum.sling.core.filter.ResourceFilter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Iterator;

public class ResourceNodeIterator implements Iterator<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceNodeIterator.class);

    protected final ResourceResolver resolver;
    protected final NodeIterator nodeIterator;
    protected final ResourceFilter filter;

    public ResourceNodeIterator(@Nonnull final ResourceResolver resolver, @Nonnull final NodeIterator nodeIterator) {
        this(resolver, nodeIterator, null);
    }

    public ResourceNodeIterator(@Nonnull final ResourceResolver resolver, @Nonnull final NodeIterator nodeIterator,
                                @Nullable ResourceFilter filter) {
        this.resolver = resolver;
        this.nodeIterator = nodeIterator;
        this.filter = filter != null ? filter : ResourceFilter.ALL;
    }

    @Override
    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    @Override
    public Resource next() {
        try {
            Node node = nodeIterator.nextNode();
            return resolver.getResource(node.getPath());
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void remove() {
    }
}
