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

import com.composum.sling.core.mapping.jcr.StringFilterMapping;
import com.composum.sling.core.mapping.json.StringFilterTypeAdapter;
import com.composum.sling.core.util.JsonTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * some short tests for the StringFilter implementation
 */
public class StringFilterTest {

    public static final StringFilter PAGE_FILTER = new StringFilter.WhiteList(
            "^(nt|sling):.*[Ff]older$,^[a-z]+:Page$");

    public static final StringFilter FIRST_RULE_SET = new StringFilter.FilterSet(
            StringFilter.FilterSet.Rule.first,
            PAGE_FILTER,
            new StringFilter.BlackList(new String[]{"^[a-z]+:Page$", "^.*:.*[Ff]older$"})
    );

    public static final StringFilter LAST_RULE_SET = new StringFilter.FilterSet(
            StringFilter.FilterSet.Rule.last,
            new StringFilter.BlackList(new String[]{"^[a-z]+:Page$", "^.*:.*[Ff]older$"}),
            PAGE_FILTER
    );

    public static final StringFilter OR_RULE_SET = new StringFilter.FilterSet(
            StringFilter.FilterSet.Rule.or,
            new StringFilter.BlackList(new String[]{"^(nt|sling):.*$", "^[a-z]+:PageContent$"}),
            PAGE_FILTER
    );

    public static final StringFilter AND_RULE_SET = StringFilterMapping.fromString(
            "and{-'^[a-z]+:PageContent$',+'^(nt|sling):.*[Ff]older$,^[a-z]+:Page$'}");

    @Test
    public void testStringFilter() {
        testStringFilter(PAGE_FILTER);
    }

    @Test
    public void testFilterFirst() {
        testStringFilter(FIRST_RULE_SET);
    }

    @Test
    public void testFilterLast() {
        testStringFilter(LAST_RULE_SET);
    }

    @Test
    public void testFilterOrSet() {
        testStringFilter(OR_RULE_SET);
    }

    @Test
    public void testFilterAndSet() {
        testStringFilter(AND_RULE_SET);
    }

    public void testStringFilter(StringFilter filter) {
        assertThat(filter.accept("nt:folder"), is(true));
        assertThat(filter.accept("sling:Folder"), is(true));
        assertThat(filter.accept("sling:OrderedFolder"), is(true));
        assertThat(filter.accept("cq:Page"), is(true));
        assertThat(filter.accept("cq:PageContent"), is(false));
        assertThat(filter.accept("sling:Mapping"), is(false));
        assertThat(filter.accept("nt:unstructured"), is(false));
    }

    @Test
    public void testJsonMapping() {
        JsonTest.testWriteReadWriteEquals(FIRST_RULE_SET, StringFilterTypeAdapter.GSON);
        JsonTest.testWriteReadWriteEquals(AND_RULE_SET, StringFilterTypeAdapter.GSON);
    }
}
