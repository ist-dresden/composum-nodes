/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: ResourceHandleTest.java
 * Autor: Mirko Zeibig
 * Datum: 11.01.2013 09:56:26
 */

package com.composum.sling.core.filter;

import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.mapping.json.ResourceFilterTypeAdapter;
import com.composum.sling.core.util.JsonTest;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * some short tests for the ResourceFilter implementation
 */
public class ResourceFilterTest {

    public static final ResourceFilter NAME_FILTER = new ResourceFilter.NameFilter(
            new StringFilter.WhiteList("^.*test$"));

    public static final ResourceFilter PAGE_FILTER = new ResourceFilter.PrimaryTypeFilter(
            new StringFilter.WhiteList(
                    "^(nt|sling):.*[Ff]older$",
                    "^[a-z]+:Page$"));

    public static final ResourceFilter NO_FOLDER_FILTER = new ResourceFilter.PrimaryTypeFilter(
            new StringFilter.BlackList(
                    "^(nt|sling):.*[Ff]older$"));

    public static final ResourceFilter PATH_FILTER = new ResourceFilter.PathFilter(
            new StringFilter.WhiteList(
                    "^/content/test",
                    ".*/mocked/.*"));

    public static final ResourceFilter TYPE_FILTER = new ResourceFilter.ResourceTypeFilter(
            new StringFilter.WhiteList(
                    "components/mock"));

    public static final ResourceFilter FIRST_RULE_SET = new ResourceFilter.FilterSet(
            ResourceFilter.FilterSet.Rule.first,
            NO_FOLDER_FILTER, NAME_FILTER, PAGE_FILTER);

    public static final ResourceFilter LAST_RULE_SET = new ResourceFilter.FilterSet(
            ResourceFilter.FilterSet.Rule.last,
            NAME_FILTER, PAGE_FILTER, NO_FOLDER_FILTER);

    public static final ResourceFilter OR_RULE_SET = new ResourceFilter.FilterSet(
            ResourceFilter.FilterSet.Rule.or,
            ResourceFilter.FOLDER, NAME_FILTER, PAGE_FILTER);

    public static final ResourceFilter AND_RULE_SET = ResourceFilterMapping.fromString(
            "and{Name(+'^.*test$'),PrimaryType(+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$'),Path(+'^/content/test,.*/mocked/.*'),ResourceType(+'components/mock')}"
    );

    protected Resource createTestResource() throws RepositoryException {
        NodeType nodeType = createMock(NodeType.class);
        expect(nodeType.getName()).andReturn("nt:folder").anyTimes();
        replay(nodeType);
        Node node = createMock(Node.class);
        expect(node.getPrimaryNodeType()).andReturn(nodeType).anyTimes();
        expect(node.getName()).andReturn("resource-test").anyTimes();
        expect(node.getPath()).andReturn("/content/test/mocked/resource-test").anyTimes();
        replay(node);
        Resource resource = createMock(Resource.class);
        expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();
        expect(resource.getName()).andReturn(node.getName()).anyTimes();
        expect(resource.getPath()).andReturn(node.getPath()).anyTimes();
        expect(resource.getResourceType()).andReturn("test/components/mock").anyTimes();
        replay(resource);
        return resource;
    }

    @Test
    public void testResourceFilters() throws RepositoryException {
        Resource resource = createTestResource();
        assertThat(NAME_FILTER.accept(resource), is(true));
        assertThat(PAGE_FILTER.accept(resource), is(true));
        assertThat(PATH_FILTER.accept(resource), is(true));
        assertThat(TYPE_FILTER.accept(resource), is(true));
        assertThat(FIRST_RULE_SET.accept(resource), is(false));
        assertThat(LAST_RULE_SET.accept(resource), is(false));
        assertThat(OR_RULE_SET.accept(resource), is(true));
        assertThat(AND_RULE_SET.accept(resource), is(true));
    }

    @Test
    public void testJsonMapping() {
        JsonTest.testWriteReadWriteEquals(NAME_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(PAGE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(PATH_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(TYPE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(FIRST_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(OR_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(AND_RULE_SET, ResourceFilterTypeAdapter.GSON);
    }
}
