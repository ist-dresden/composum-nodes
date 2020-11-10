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
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
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

    public static final ResourceFilter NODE_TYPE_FILTER = new ResourceFilter.NodeTypeFilter(
            new StringFilter.WhiteList("nt:folder"));

    public static final ResourceFilter PATH_FILTER = new ResourceFilter.PathFilter(
            new StringFilter.WhiteList(
                    "^/content/test",
                    ".*/mocked/.*"));

    public static final ResourceFilter TYPE_FILTER = new ResourceFilter.TypeFilter("nt:folder");

    public static final ResourceFilter RESOURCE_TYPE_FILTER = new ResourceFilter.ResourceTypeFilter(
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

    public static final ResourceFilter ALL_FILTER = ResourceFilter.ALL;

    public static final ResourceFilter AND_RULE_SET = ResourceFilterMapping.fromString(
            "and{Name(+'^.*test$'),PrimaryType(+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$'),Path(+'^/content/test,.*/mocked/.*'),ResourceType(+'components/mock')}"
    );

    public static final ResourceFilter NONE_RULE_SET = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.none,
            new ResourceFilter.TypeFilter("nt:unstructured"));

    public static final ResourceFilter CONTENT_NODE_FILTER = new ResourceFilter.ContentNodeFilter(true, NAME_FILTER, TYPE_FILTER);

    protected Resource createResource(final ResourceResolver resolver, final String primaryType,
                                      final String path, final String resourceType)
            throws RepositoryException {
        final String name = StringUtils.substringAfterLast(path, "/");
        NodeType nodeType = createMock(NodeType.class);
        expect(nodeType.getName()).andReturn(primaryType).anyTimes();
        expect(nodeType.getSupertypes()).andReturn(new NodeType[0]).anyTimes();
        replay(nodeType);
        Node node = createMock(Node.class);
        expect(node.getPrimaryNodeType()).andReturn(nodeType).anyTimes();
        expect(node.getName()).andReturn(name).anyTimes();
        expect(node.getPath()).andReturn(path).anyTimes();
        expect(node.isNodeType(primaryType)).andReturn(true).anyTimes();
        expect(node.isNodeType(anyObject(String.class))).andReturn(false).anyTimes();
        expect(node.getMixinNodeTypes()).andReturn(new NodeType[0]).anyTimes();
        replay(node);
        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
        properties.put(ResourceUtil.PROP_RESOURCE_TYPE, resourceType);
        Resource resource = createMock(Resource.class);
        expect(resource.getResourceResolver()).andReturn(resolver).anyTimes();
        expect(resource.getValueMap()).andReturn(new ValueMapDecorator(properties)).anyTimes();
        expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();
        expect(resource.getName()).andReturn(name).anyTimes();
        expect(resource.getPath()).andReturn(path).anyTimes();
        expect(resource.getResourceType()).andReturn(resourceType).anyTimes();
        expect(resource.isResourceType(primaryType)).andReturn(true).anyTimes();
        expect(resource.isResourceType(resourceType)).andReturn(true).anyTimes();
        expect(resource.isResourceType(anyObject(String.class))).andReturn(false).anyTimes();
        replay(resource);
        return resource;
    }

    public ResourceResolver createResolver() {
        ResourceResolver resolver = createMock(ResourceResolver.class);
        expect(resolver.getSearchPath()).andReturn(new String[0]).anyTimes();
        replay(resolver);
        return resolver;
    }

    @Test
    public void testResourceFilters() throws RepositoryException {
        ResourceResolver resolver = createResolver();
        Resource matchesResource = createResource(resolver, "nt:folder",
                "/content/test/mocked/resource-test", "test/components/mock");
        Resource notMatchesResource = createResource(resolver, "nt:unstructured",
                "/nowhere", "test/type/not/matching");

        assertThat(NAME_FILTER.accept(matchesResource), is(true));
        assertThat(PAGE_FILTER.accept(matchesResource), is(true));
        assertThat(PATH_FILTER.accept(matchesResource), is(true));
        assertThat(RESOURCE_TYPE_FILTER.accept(matchesResource), is(true));
        assertThat(TYPE_FILTER.accept(matchesResource), is(true));
        assertThat(NODE_TYPE_FILTER.accept(matchesResource), is(true));
        assertThat(FIRST_RULE_SET.accept(matchesResource), is(false));
        assertThat(LAST_RULE_SET.accept(matchesResource), is(false));
        assertThat(OR_RULE_SET.accept(matchesResource), is(true));
        assertThat(AND_RULE_SET.accept(matchesResource), is(true));
        assertThat(ALL_FILTER.accept(matchesResource), is(true));
        assertThat(NONE_RULE_SET.accept(matchesResource), is(true));

        assertThat(NAME_FILTER.accept(notMatchesResource), is(false));
        assertThat(PAGE_FILTER.accept(notMatchesResource), is(false));
        assertThat(PATH_FILTER.accept(notMatchesResource), is(false));
        assertThat(RESOURCE_TYPE_FILTER.accept(notMatchesResource), is(false));
        assertThat(TYPE_FILTER.accept(notMatchesResource), is(false));
        assertThat(NODE_TYPE_FILTER.accept(notMatchesResource), is(false));
        assertThat(FIRST_RULE_SET.accept(notMatchesResource), is(false));
        assertThat(LAST_RULE_SET.accept(notMatchesResource), is(false));
        assertThat(OR_RULE_SET.accept(notMatchesResource), is(false));
        assertThat(AND_RULE_SET.accept(notMatchesResource), is(false));
        assertThat(NONE_RULE_SET.accept(notMatchesResource), is(false));
    }

    @Test
    public void testJsonMapping() {
        JsonTest.testWriteReadWriteEquals(NAME_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(PAGE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(PATH_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(ALL_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(RESOURCE_TYPE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(TYPE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(NODE_TYPE_FILTER, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(FIRST_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(LAST_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(OR_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(AND_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(NONE_RULE_SET, ResourceFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(CONTENT_NODE_FILTER, ResourceFilterTypeAdapter.GSON);
    }

    @Test
    public void testResourceFilterMapping() throws RepositoryException {
        ResourceResolver resolver = createResolver();
        Resource matchesResource = createResource(resolver, "nt:folder",
                "/content/test/mocked/resource-test", "test/components/mock");
        Resource notMatchesResource = createResource(resolver, "nt:unstructured",
                "/nowhere", "test/type/not/matching");

        checkStringRepresentation(matchesResource, notMatchesResource, NAME_FILTER, "Name(+'^.*test$')");
        checkStringRepresentation(matchesResource, notMatchesResource, PAGE_FILTER, "PrimaryType(+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$')");
        checkStringRepresentation(matchesResource, notMatchesResource, PATH_FILTER, "Path(+'^/content/test,.*/mocked/.*')");
        checkStringRepresentation(matchesResource, notMatchesResource, RESOURCE_TYPE_FILTER, "ResourceType(+'components/mock')");
        checkStringRepresentation(matchesResource, notMatchesResource, TYPE_FILTER, "Type(+[nt:folder])");
        checkStringRepresentation(matchesResource, notMatchesResource, NODE_TYPE_FILTER, "NodeType(+'nt:folder')");
        checkStringRepresentation(matchesResource, notMatchesResource, OR_RULE_SET,
                "or{Folder(),Name(+'^.*test$'),PrimaryType(+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$')}");
        checkStringRepresentation(matchesResource, notMatchesResource, AND_RULE_SET,
                "and{Name(+'^.*test$'),PrimaryType(+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$')," +
                        "Path(+'^/content/test,.*/mocked/.*'),ResourceType(+'components/mock')}");
        checkStringRepresentation(matchesResource, notMatchesResource, NONE_RULE_SET, "none{Type(+[nt:unstructured])}");

        // ALL_FILTER matches notMatchesResource, wo we have to check separately:
        assertThat(ResourceFilterMapping.toString(ALL_FILTER), is("All()"));
        assertThat(ResourceFilterMapping.fromString("All()"), CoreMatchers.instanceOf(ResourceFilter.AllFilter.class));

        assertThat(ResourceFilterMapping.fromString(""), sameInstance(ResourceFilter.ALL));
    }

    /**
     * Verifies that the string representation is as expected and that it can be deserialized again and behaves the
     * same .
     */
    private void checkStringRepresentation(Resource matchesResource, Resource notMatchesResource,
                                           ResourceFilter filter, String expectedStringRep) {
        String stringRep = ResourceFilterMapping.toString(filter);
        assertThat(stringRep, is(expectedStringRep));
        ResourceFilter deserializedFilter = ResourceFilterMapping.fromString(stringRep);

        assertThat(filter.accept(matchesResource), is(true));
        assertThat(deserializedFilter.accept(matchesResource), is(true));

        assertThat(filter.accept(notMatchesResource), is(false));
        assertThat(deserializedFilter.accept(notMatchesResource), is(false));

        assertThat(deserializedFilter.getClass().getName(), is(filter.getClass().getName()));

        assertThat(ResourceFilterMapping.toString(filter), is(stringRep));
        assertThat(ResourceFilterMapping.toString(deserializedFilter), is(stringRep));
    }

    @Test
    public void testSomeDetailsOnLast() throws RepositoryException {
        ResourceResolver resolver = createResolver();
        Resource matchesResource = createResource(resolver, "nt:folder",
                "/content/test/mocked/resource-test", "test/components/mock");
        Resource notMatchesResource = createResource(resolver, "nt:unstructured",
                "/nowhere", "test/type/not/matching");

        ResourceFilter.FilterSet filter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.last, NAME_FILTER);
        assertThat(filter.accept(matchesResource), is(true));
        assertThat(filter.accept(notMatchesResource), is(false));

        filter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.last, NAME_FILTER, NAME_FILTER);
        assertThat(filter.accept(matchesResource), is(true));
        assertThat(filter.accept(notMatchesResource), is(false));

        filter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.last, new ResourceFilter[0]);
        assertThat(filter.accept(matchesResource), is(false));
        assertThat(filter.accept(notMatchesResource), is(false));
    }

    @Test
    public void contentNodeFilter() throws RepositoryException {
        ResourceResolver resolver = createResolver();
        Resource matchesResource = createResource(resolver, "nt:folder",
                "/content/test/mocked/resource-test", "test/components/mock");
        Resource notMatchesResource = createResource(resolver, "nt:unstructured",
                "/nowhere", "test/type/not/matching");

        // fails always on our setup - that'd require a quite expensive setup. It's tested in use from other stuff
        // assertThat(CONTENT_NODE_FILTER.accept(matchesResource), is(true));
        // this succeeds since the applyFilter fails:
        assertThat(CONTENT_NODE_FILTER.accept(notMatchesResource), is(true));
        assertThat(CONTENT_NODE_FILTER.isRestriction(), is(true));

        String stringRep = ResourceFilterMapping.toString(CONTENT_NODE_FILTER);
        assertThat(stringRep, is("ContentNode(-,Name(+'^.*test$')=jcr:content=>Type(+[nt:folder]))"));

        ResourceFilter.ContentNodeFilter deserializedFilter = (ResourceFilter.ContentNodeFilter) ResourceFilterMapping.fromString(stringRep);
        String stringRep2 = ResourceFilterMapping.toString(deserializedFilter);
        assertThat(stringRep2, is(stringRep));
        assertThat(deserializedFilter.accept(notMatchesResource), is(true));
    }

    /** An really evil filter that broke the old parsing without nested parentheses check - works now. */
    @Test
    public void parenthesesNestingWorks() {
        ResourceFilter filter = ResourceFilter.FilterSet.Rule.and.of(AND_RULE_SET,
                new ResourceFilter.ContentNodeFilter(true, AND_RULE_SET, AND_RULE_SET), AND_RULE_SET);
        String stringRep = ResourceFilterMapping.toString(filter);
        ResourceFilter deserializedFilter = ResourceFilterMapping.fromString(stringRep);
        String stringRep2 = ResourceFilterMapping.toString(deserializedFilter);
        assertThat(stringRep2, is(stringRep));
    }

}
