/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: ResourceHandleTest.java
 * Autor: Mirko Zeibig
 * Datum: 11.01.2013 09:56:26
 */

package com.composum.sling.core;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mirko Zeibig
 *
 */
public class ResourceHandleTest {

    private Resource resource;

    @Before
    public void setUp() {
        resource = createMock(Resource.class);
    }

    @Test
    public void testAdaptToWithSameType() {
        ResourceHandle rh = new ResourceHandle(null);
        ResourceHandle to = rh.adaptTo(ResourceHandle.class);
        assertSame(rh, to);
    }
}
