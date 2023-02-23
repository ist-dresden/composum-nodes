package com.composum.sling.core.pckgmgr.regpckg.util;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Version;

import java.util.Comparator;

/**
 * Since as of version 3.6.6 {@link Version#compareTo(Version)} is incompatible with maven version ordering,
 * this implements maven semantics.  (it'll order e.g. 1.2.3-SNAPSHOT after 1.2.3, while 1.2.3 is the release that is created after all snapshots).
 *
 * <p>
 * The functionality is specifically:
 * <ul>
 * <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,</li>
 * <li>transition between characters and digits also constitutes a separator:
 *     <code>1.0alpha1 =&gt; [1, 0, alpha, 1]</code></li>
 * <li>unlimited number of version components,</li>
 * <li>version components in the text can be digits or strings,</li>
 * <li>
 *   String qualifiers are ordered lexically (case insensitive), with the following exceptions:
 *   <ul>
 *     <li> 'snapshot' &lt; '' &lt; 'sp' </li>
 *   </ul>
 *   and alias -&gt; replacement (all case insensitive):
 *   <ul>
 *     <li> 'a' -&gt; 'alpha' </li>
 *     <li> 'b' -&gt; 'beta' </li>
 *     <li> 'm' -&gt; 'milestone' </li>
 *     <li> 'cr' -&gt; 'rc' </li>
 *     <li> 'final' -&gt; '' </li>
 *     <li> 'final' -&gt; '' </li>
 *     <li> 'final' -&gt; '' </li>
 *   </ul>
 * </li>
 * <li>
 *   Following semver rules is encouraged, and some qualifiers are discouraged (no matter the case):
 *   <ul>
 *     <li> The usage of 'CR' qualifier is discouraged. Use 'RC' instead. </li>
 *     <li> The usage of 'final', 'ga', and 'release' qualifiers is discouraged. Use no qualifier instead. </li>
 *     <li> The usage of 'SP' qualifier is discouraged. Increment the patch version instead. </li>
 *   </ul>
 *   For other qualifiers, natural ordering is used (case insensitive):
 *   <ul>
 *     <li> alpha = a &lt; beta = b &lt; milestone = m &lt; rc = cr &lt; snapshot &lt; '' = final = ga = release &lt; sp </li>
 *   </ul>
 * </li>
 * <li>a hyphen usually precedes a qualifier, and is always less important than digits/number, for example
 *   1.0.RC2 &lt; 1.0-RC3 &lt; 1.0.1 ; but prefer '1.0.0-RC1' over '1.0.0.RC1' </li>
 * </ul>
 *
 * @see "https://github.com/apache/maven/blob/maven-3.8.6/maven-artifact/src/main/java/org/apache/maven/artifact/versioning/ComparableVersion.java"
 */
public class VersionComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        ComparableVersion v1 = new ComparableVersion(o1);
        ComparableVersion v2 = new ComparableVersion(o2);
        return v1.compareTo(v2);
    }

    /**
     * Comparator that uses {@link VersionComparator#compare(String, String)} with PackageIds,
     * since {@link org.apache.jackrabbit.vault.packaging.PackageId#compareTo(PackageId)}
     * uses (as of filevault-3.6.6) the wrt. maven ignorant version comparison.
     */
    public static class PackageIdComparator implements Comparator<PackageId> {

        protected final Comparator<String> myVersionComparator;

        public PackageIdComparator(boolean reverseVersionComparison) {
            if (reverseVersionComparison) {
                myVersionComparator = new VersionComparator().reversed();
            } else {
                myVersionComparator = new VersionComparator();
            }
        }

        @Override
        public int compare(PackageId o1, PackageId o2) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(o1.getGroup(), o2.getGroup());
            builder.append(o1.getName(), o2.getName());
            builder.appendSuper(myVersionComparator.compare(o1.getVersionString(), o2.getVersionString()));
            return builder.toComparison();
        }
    }

    /**
     * Compares {@link PackageId} by group and name only, ignoring the version.
     */
    public static class PackageIdByGroupAndNameComparator implements Comparator<PackageId> {
        @Override
        public int compare(PackageId o1, PackageId o2) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(o1.getGroup(), o2.getGroup());
            builder.append(o1.getName(), o2.getName());
            return builder.toComparison();
        }
    }

}
