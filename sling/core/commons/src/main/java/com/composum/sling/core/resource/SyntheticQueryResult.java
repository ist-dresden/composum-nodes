package com.composum.sling.core.resource;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class SyntheticQueryResult extends SyntheticResource {

    private static final Logger LOG = LoggerFactory.getLogger(SyntheticQueryResult.class);

    protected final QueryResult queryResult;
    protected final ModifiableValueMap valueMap;
    protected final ResourceFilter filter;

    public SyntheticQueryResult(ResourceResolver resolver, String path, QueryResult queryResult) {
        this(resolver, path, queryResult, ResourceFilter.ALL);
    }

    public SyntheticQueryResult(ResourceResolver resolver, String path, QueryResult queryResult,
                                ResourceFilter filter) {
        this(resolver, path, queryResult, filter, ResourceUtil.TYPE_SLING_FOLDER);
    }

    public SyntheticQueryResult(ResourceResolver resolver, String path, QueryResult queryResult,
                                ResourceFilter filter, String resourceType) {
        super(resolver, path, resourceType);
        this.queryResult = queryResult;
        this.filter = filter;
        valueMap = new ModifiableValueMapDecorator(new HashMap<String, Object>());
        putValue(ResourceUtil.PROP_RESOURCE_TYPE, resourceType);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (ValueMap.class.isAssignableFrom(type)) {
            return (AdapterType) valueMap;
        }
        return super.adaptTo(type);
    }

    public void putValue(String path, Object value) {
        valueMap.put(path, value);
    }

    @Override
    public Iterator<Resource> listChildren() {
        try {
            return new ResourceNodeIterator(getResourceResolver(), queryResult.getNodes(), filter);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            return Collections.<Resource>emptyList().iterator();
        }
    }
}
