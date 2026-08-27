package io.github.ethan2258.motootaidentity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionComparatorTest {
    @Test
    public void newerMajorMinorAndPatchAreDetected() {
        assertTrue(VersionComparator.isNewer("v2.0.0", "1.9.9"));
        assertTrue(VersionComparator.isNewer("1.2.0", "1.1.9"));
        assertTrue(VersionComparator.isNewer("1.1.1", "1.1.0"));
    }

    @Test
    public void equalAndOlderVersionsAreRejected() {
        assertFalse(VersionComparator.isNewer("v1.1.0", "1.1"));
        assertFalse(VersionComparator.isNewer("1.0.9", "1.1.0"));
    }

    @Test
    public void releaseSuffixDoesNotBreakComparison() {
        assertTrue(VersionComparator.isNewer("v1.2.0-stable", "1.1.0"));
    }
}
