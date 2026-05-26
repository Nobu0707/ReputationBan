package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BanManagementPolicyTest {
    @Test
    void activeBanHasNoUnbanAndNoPastExpiry() {
        long now = 1_000L;
        assertTrue(BanManagementPolicy.isActive(null, null, now));
        assertTrue(BanManagementPolicy.isActive(1_001L, null, now));
    }

    @Test
    void expiredOrUnbannedBanIsNotActive() {
        long now = 1_000L;
        assertFalse(BanManagementPolicy.isActive(1_000L, null, now));
        assertFalse(BanManagementPolicy.isActive(1_001L, 900L, now));
    }

    @Test
    void pardonRestoresAtLeastMaxScore() {
        assertEquals(100, BanManagementPolicy.pardonTargetScore(20, 100));
        assertEquals(120, BanManagementPolicy.pardonTargetScore(120, 100));
    }
}
