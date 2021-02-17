package com.composum.sling.core.pckgmgr.registry.util;

import org.apache.jackrabbit.vault.packaging.Version;

import java.util.Comparator;

/**
 * modified Version.compareTo() to order releases after pre-releases
 */
public class VersionComparator implements Comparator<String> {

    public static class Inverted extends VersionComparator {

        @Override
        public int compare(String o1, String o2) {
            return super.compare(o2, o1);
        }
    }

    @Override
    public int compare(String o1, String o2) {
        String[] segs1 = Version.create(o1).getNormalizedSegments();
        String[] segs2 = Version.create(o2).getNormalizedSegments();
        for (int i = 0; i < Math.min(segs1.length, segs2.length); i++) {
            String s1 = segs1[i];
            String s2 = segs2[i];
            int strCompare = s1.compareTo(s2);
            if (strCompare == 0) {
                continue;
            }
            try {
                int v1 = Integer.parseInt(segs1[i]);
                int v2 = Integer.parseInt(segs2[i]);
                return v1 - v2;
            } catch (NumberFormatException e) {
                // no numbers, use string compare
                return strCompare;
            }
        }
        // inserted to compare rleases and pre-releases
        if (segs1.length < segs2.length) {
            try {
                Integer.parseInt(segs2[segs1.length]);
            } catch (NumberFormatException e) {
                return 1;
            }
        } else if (segs1.length > segs2.length) {
            try {
                Integer.parseInt(segs1[segs2.length]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        //
        return segs1.length - segs2.length;
    }
}
