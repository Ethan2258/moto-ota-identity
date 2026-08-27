package io.github.ethan2258.motootaidentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OtaChannelAliasTest {
    @Test
    public void supportedAliasIsNormalized() {
        assertTrue(OtaChannelAlias.isValid(" RETGB "));
        assertEquals("retgb", OtaChannelAlias.normalize(" RETGB "));
    }

    @Test
    public void offIsSupported() {
        assertTrue(OtaChannelAlias.isValid(""));
        assertEquals(OtaChannelAlias.OFF, OtaChannelAlias.normalize(""));
    }

    @Test
    public void unsupportedAliasFailsClosed() {
        assertFalse(OtaChannelAlias.isValid("retcn"));
        assertEquals(OtaChannelAlias.OFF, OtaChannelAlias.normalize("retcn"));
    }
}
