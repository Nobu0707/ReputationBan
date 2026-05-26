package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManualScoreChangeGateTest {
    @Test
    void requiresBanPermissionOnlyForFirstCrossingIntoThreshold() {
        assertTrue(ManualScoreChangeGate.requiresBanPermission(1, 0, 0));
        assertTrue(ManualScoreChangeGate.requiresBanPermission(5, -2, 0));
        assertFalse(ManualScoreChangeGate.requiresBanPermission(0, -5, 0));
        assertFalse(ManualScoreChangeGate.requiresBanPermission(5, 4, 0));
        assertFalse(ManualScoreChangeGate.requiresBanPermission(5, 10, 0));
    }
}
