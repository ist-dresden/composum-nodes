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
import java.util.ArrayList;
import java.util.List;

public class ComponentTagTEI extends TagExtraInfo {

    @Override
    public VariableInfo[] getVariableInfo(TagData data) {
        List<VariableInfo> variables = new ArrayList<>();
        createVariables(data, variables);
        return variables.toArray(new VariableInfo[variables.size()]);
    }

    protected void createVariables(TagData data, List<VariableInfo> variables) {
        String var = getVar(data);
        String type = data.getAttributeString("type");
        variables.add(new VariableInfo(var, type, true, VariableInfo.NESTED));
    }

    protected String getVar(TagData data) {
        String varname = data.getAttributeString("var");
        if (StringUtils.isBlank(varname)) {
            varname = data.getId();
        }
        return varname;
    }
}
