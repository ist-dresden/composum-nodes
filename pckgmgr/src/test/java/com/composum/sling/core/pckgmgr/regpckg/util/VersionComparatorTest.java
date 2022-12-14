package com.composum.sling.core.pckgmgr.regpckg.util;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Some tests for VersionComparator.
 *
 * @see "https://issues.apache.org/jira/browse/JCRVLT-672"
 */
public class VersionComparatorTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    private final VersionComparator comparator = new VersionComparator();

    /**
     * Original test from file-vault, as of version 3.6.6 - does not respect maven ordering in commented out cases.
     */
    @Test
    public void testCompareLikeVersion() {
        compare("1.0.0", "1.0.0", 0);
        compare("1.0.1", "1.0.0", 1);
        compare("1.1", "1.0.0", 1);
        compare("1.11", "1.9", 1);
        compare("1.1-SNAPSHOT", "1.0.0", 1);
        compare("2.0", "2.0-beta-8", 1); // original -1
        compare("2.0", "2.0-SNAPSHOT", 1); // original -1
        compare("1.11", "1.9-SNAPSHOT", 1);
        compare("1.11-SNAPSHOT", "1.9-SNAPSHOT", 1);
        compare("1.11-SNAPSHOT", "1.9", 1);
        compare("1.1", "1.1-SNAPSHOT", 1); // original -1
        compare("1.1-SNAPSHOT", "1.1-R12345", 1);
        compare("2.1.492-NPR-12954-R012", "2.1.476", 1);
        compare("6.1.58", "6.1.58-FP3", 1); // original -1
        compare("6.1.58", "6.1.58.FP3", 1); // original -1 , doubtful
        compare("6.1.59", "6.1.58.FP3", 1);
        compare("6.1.58-FP3", "6.1.58-FP2", 1);
        compare("6.1.58-FP3", "6.1.58.FP3", 0);
        compare("6.1.58-FP3", "6.1.58.FP4", -1);
        compare("6.1.58.FP3", "6.1.58-FP4", -1);
        compare("6.1.58.FP3", "6.1.58.FP4", -1);
        compare("6.1.0", "6.1-FP3", -1);
        compare("6.1", "6.1-FP3", 1); // original -1
    }

    private void compare(String v1, String v2, int comp) {
        int ret = comparator.compare(v1, v2);
        ec.checkThat(v1 + " compare to " + v2 + " must return " + comp, Math.signum(ret), CoreMatchers.is((float) comp));
    }

}
