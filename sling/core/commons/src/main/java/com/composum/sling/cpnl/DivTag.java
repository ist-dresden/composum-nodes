/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.sling.cpnl;

/**
 * a tag to render a 'div' HTML tag with support for a 'test' condition
 */
public class DivTag extends TagBase {

    protected String getDefaultTagName() {
        return "div";
    }
}
