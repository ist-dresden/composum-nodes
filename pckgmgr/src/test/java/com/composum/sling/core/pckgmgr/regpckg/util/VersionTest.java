package com.composum.sling.core.pckgmgr.regpckg.util;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.jackrabbit.vault.packaging.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Checks some stuff about {@link org.apache.jackrabbit.vault.packaging.Version}. Not a real unittest, more of a documentation.
 */
public class VersionTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Test
    public void testSnapshotSegmentation() {
        Version v1 = Version.create("1.2.3-SNAPSHOT-1");
        assertThat(asList(v1.getNormalizedSegments()), is(asList("1", "2", "3", "SNAPSHOT-1")));
        assertThat(v1.toString(), is("1.2.3-SNAPSHOT-1"));
    }

    /**
     * According to the maven ComparableVersion spec, 1.0alpha1 should be segmented as 1 0 alpha 1 , but isn't. Impedance mismatch.
     */
    @Test
    public void testBrokenAlphaBorderSegmentation() {
        Version v1 = Version.create("1.0alpha1");
        assertThat(asList(v1.getNormalizedSegments()), is(asList("1", "0alpha1"))); // no good.
        assertThat(v1.toString(), is("1.0alpha1"));
    }

    @Test
    public void sortComparableVersion() {
        List<String> versions = new ArrayList<>();
        for (String s : Arrays.asList("a", "alpha", "ap", "bi", "b", "beta", "bf", "a-1", "a1", "a.1",
                "x.1", "x-1", "x1",
                "SNAPSHOT", "sp", "sa", "su", "final", "cr", "rc", "z", "10", "3", "1", "d10", "d2", "d.2", "d-2",
                "sp-1", "sp.1", "sp2", "final-1", "final-2")) {
            versions.add("1-" + s);
            // versions.add("1." + s);
        }
        versions.addAll(Arrays.asList("1", "1.0.0-RC1", "1.0.0.RC1", "1.0-20081117.213112-16"));
        compareAllAlgorithms(versions);

        compareAllAlgorithms(Arrays.asList("1.0.0", "1.0", "1", "1.1.0", "1.1", "1.2", "1.2.0", "1.2.1", "1.0-20081117.213112-16",
                "2.1-RC1", "2.1-SNAPSHOT", "2.1-R123456"));
    }

    private void compareAllAlgorithms(List<String> versions) {
        sortAndPrintVersions("ComparableVersion", versions, Comparator.comparing(ComparableVersion::new));
        sortAndPrintVersions("Version", versions, Comparator.comparing((String v) -> Version.create(v)));
        sortAndPrintVersions("VersionComparator", versions, new VersionComparator());
    }

    private void sortAndPrintVersions(String name, List<String> versions, Comparator<String> comparator) {
        System.out.println(name);
        Collections.sort(versions, comparator);
        for (int i = 0; i < versions.size(); ++i) {
            for (int j = 0; j < versions.size(); ++j) {
                String v1 = versions.get(i);
                String v2 = versions.get(j);
                ec.checkThat(v1 + " compareTo " + v2, v1.compareTo(v2), is(-v2.compareTo(v1)));
            }
        }
        List<List<String>> sortedAndGrouped = new ArrayList<>();
        List<String> sorted = new LinkedList<>(versions);
        while (!sorted.isEmpty()) {
            String head = sorted.remove(0);
            List<String> lastGroup = sortedAndGrouped.isEmpty() ? Collections.emptyList() : sortedAndGrouped.get(sortedAndGrouped.size() - 1);
            if (!lastGroup.isEmpty() && comparator.compare(head, lastGroup.get(0)) == 0) {
                lastGroup.add(head);
            } else {
                sortedAndGrouped.add(new ArrayList<>(Arrays.asList(head)));
            }
        }
        System.out.println(sortedAndGrouped);
        System.out.println();
    }

}
