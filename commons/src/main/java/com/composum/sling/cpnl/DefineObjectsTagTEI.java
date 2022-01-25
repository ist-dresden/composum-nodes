package com.composum.sling.cpnl;

import com.composum.sling.core.BeanContext;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.composum.sling.cpnl.DefineObjectsTag.RA_BEAN_CONTEXT;
import static com.composum.sling.cpnl.DefineObjectsTag.RA_COMPOSUM_BASE;
import static com.composum.sling.cpnl.DefineObjectsTag.RA_CONTEXT_PATH;

public class DefineObjectsTagTEI extends org.apache.sling.scripting.jsp.taglib.DefineObjectsTEI {

    public VariableInfo[] getVariableInfo(TagData data) {
        List<VariableInfo> variables = new ArrayList<>(Arrays.asList(super.getVariableInfo(data)));
        collectVariables(data, variables);
        return variables.toArray(new VariableInfo[0]);
    }

    protected void collectVariables(TagData data, List<VariableInfo> variables) {
        variables.add(new VariableInfo(RA_CONTEXT_PATH, String.class.getName(), true, VariableInfo.AT_END));
        variables.add(new VariableInfo(RA_BEAN_CONTEXT, BeanContext.class.getName(), true, VariableInfo.AT_END));
        variables.add(new VariableInfo(RA_COMPOSUM_BASE, String.class.getName(), true, VariableInfo.AT_END));
    }
}
