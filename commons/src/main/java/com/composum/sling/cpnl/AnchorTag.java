/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag to build a page anchor link
 */
public class AnchorTag extends TagBase {

    protected String name;

    @Override
    protected void clear() {
        name = null;
        super.clear();
    }

    @Override
    protected String getDefaultTagName() {
        return "a";
    }

    /**
     * @param anchor the identifier of the anchor link tag to set
     */
    public void setName(String anchor) {
        this.name = anchor;
    }

    protected String getName() {
        return name;
    }

    @Override
    protected void writeAttributes(JspWriter writer) throws IOException {
        writer.append(" name=\"").append(CpnlElFunctions.text(getName())).append("\"");
        super.writeAttributes(writer);
    }

    /**
     * @return the 'test' result combined with the name not blank check
     */
    @Override
    protected boolean getTestResult() {
        return super.getTestResult() && StringUtils.isNotBlank(getName());
    }
}
