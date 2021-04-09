package com.composum.sling.core.osgi.pckginstall;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;

public class PackageTransformerTest {

    @Test
    public void testVersionNumbering() throws Exception {
        verify("1", "1.0.0");
        verify("1.2", "1.2.0");
        verify("1.3.4", "1.3.4");
        verify("1.3.4.5", "1.3.4.5"); // nonstandard, not recommended by maven

        verify("1-SNAPSHOT", "1.0.0.SNAPSHOT");
        verify("1.2-SNAPSHOT", "1.2.0.SNAPSHOT");
        verify("1.3.4-SNAPSHOT", "1.3.4.SNAPSHOT");
        verify("1.3.4.5-SNAPSHOT", "1.3.4.5-SNAPSHOT");

        // build numbers
        verify("1-12", "1.0.0.12");
        verify("1.2-12", "1.2.0.12");
        verify("1.3.4-12", "1.3.4.12");
        verify("1.3.4.5-12", "1.3.4.5-12");

        // some qualifier
        verify("1-beta-2", "1.0.0.beta-2");
        verify("1.2-beta-2", "1.2.0.beta-2");
        verify("1.3.4-beta-2", "1.3.4.beta-2");
        verify("1.3.4.5-beta-2", "1.3.4.5-beta-2");

        verify("1.3.4.5broken", "1.3.4.5broken");
        verify("1.2.3broken", "1.2.0.3broken");

        verify("1.2.3-()@#)@", "1.2.3.______");
    }

    private void verify(String rawVersion, String expected) {
        String cleanedVersionOur = PackageTransformer.cleanupVersion(rawVersion);
        String osgiOur = osgifyVersion(cleanedVersionOur);
        Assert.assertEquals(expected, osgiOur);
    }

    private String osgifyVersion(String version) {
        try {
            Version osgiversion = new Version(version);
            return osgiversion.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }


}
