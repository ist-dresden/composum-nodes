package com.composum.sling.cpnl;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static com.composum.sling.cpnl.CpnlElFunctions.contains;

import java.util.Arrays;

public class CpnlElFunctionsTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Test
    public void cdata() {
        assertEquals(null, CpnlElFunctions.cdata(null));
        assertEquals("<![CDATA[<script>alert('hello');</script>]]>", CpnlElFunctions.cdata("<script>alert('hello');</script>"));
        assertEquals("<![CDATA[abc]]>", CpnlElFunctions.cdata("abc"));
        assertEquals("<![CDATA[abc]]]]><![CDATA[>xyz]]>", CpnlElFunctions.cdata("abc]]>xyz"));
    }

    @Test
    public void testContains() {
        ec.checkThat(contains(null, null), Matchers.is(false));
        ec.checkThat(contains(3, 5), Matchers.is(false));
        ec.checkThat(contains(new int[]{1,3}, 5), Matchers.is(false));
        // ec.checkThat(contains(new int[]{1,3,5}, 3), Matchers.is(true)); not supported - we'd have to check all primitive types
        ec.checkThat(contains(new Object[]{"a", "3", "x"}, 3), Matchers.is(false));
        ec.checkThat(contains(new Object[]{"a", "3", "x"}, "3"), Matchers.is(true));
        ec.checkThat(contains(Arrays.asList("a", "b"), "c"), Matchers.is(false));
        ec.checkThat(contains(Arrays.asList("a", "c", "b"), "c"), Matchers.is(true));
        ec.checkThat(contains(Arrays.asList(), "c"), Matchers.is(false));
    }

}
