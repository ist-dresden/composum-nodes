package com.composum.sling.cpnl;

import static org.junit.Assert.*;

import org.junit.Test;

public class CpnlElFunctionsTest {

    @Test
    public void cdata() {
        assertEquals(null, CpnlElFunctions.cdata(null));
        assertEquals("<![CDATA[<script>alert('hello');</script>]]>", CpnlElFunctions.cdata("<script>alert('hello');</script>"));
        assertEquals("<![CDATA[abc]]>", CpnlElFunctions.cdata("abc"));
        assertEquals("<![CDATA[abc]]]]><![CDATA[>xyz]]>", CpnlElFunctions.cdata("abc]]>xyz"));
    }

}
