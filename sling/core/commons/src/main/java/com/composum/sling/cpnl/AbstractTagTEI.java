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

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTagTEI extends TagExtraInfo {

    @Override
    public VariableInfo[] getVariableInfo(TagData data) {
        List<VariableInfo> variables = new ArrayList<>();
        collectVariables(data, variables);
        return variables.toArray(new VariableInfo[variables.size()]);
    }

    protected abstract void collectVariables(TagData data, List<VariableInfo> variables);
}
