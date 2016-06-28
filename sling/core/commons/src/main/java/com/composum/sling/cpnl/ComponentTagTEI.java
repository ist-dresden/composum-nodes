/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: ComponentTagTEI.java
 * Autor: Mirko Zeibig
 * Datum: 25.01.2013 22:10:43
 */

package com.composum.sling.cpnl;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

public class ComponentTagTEI extends TagExtraInfo {

    public ComponentTagTEI() {
    }

    @Override
    public VariableInfo[] getVariableInfo(TagData data) {
        String varname = data.getAttributeString("var");
        if (StringUtils.isBlank(varname)) {
            varname = data.getId();
        }
        String type = data.getAttributeString("type");
        VariableInfo variableInfo = new VariableInfo(varname, type, true, VariableInfo.NESTED);
        return new VariableInfo[]{variableInfo};
    }

}
