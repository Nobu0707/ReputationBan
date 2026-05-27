package dev.modplugin.reputationban.integration.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorldGuardRegionPolicyTest {
    @Test
    void matchesConfiguredCategories() {
        assertTrue(WorldGuardRegionPolicy.shouldCapture(true, true, List.of("griefing", "harassment"), "griefing"));
        assertFalse(WorldGuardRegionPolicy.shouldCapture(false, true, List.of("griefing"), "griefing"));
        assertFalse(WorldGuardRegionPolicy.shouldCapture(true, false, List.of("griefing"), "griefing"));
        assertFalse(WorldGuardRegionPolicy.shouldCapture(true, true, List.of("griefing"), "spam"));
    }

    @Test
    void clampsMaxRegions() {
        assertEquals(0, WorldGuardRegionPolicy.clampMaxRegions(-1, 5));
        assertEquals(3, WorldGuardRegionPolicy.clampMaxRegions(3, 5));
        assertEquals(5, WorldGuardRegionPolicy.clampMaxRegions(10, 5));
    }

    @Test
    void hidesOwnersAndMembersByDefault() {
        assertEquals("hidden", WorldGuardRegionPolicy.ownerMemberValue(false, 3));
        assertEquals("count=3", WorldGuardRegionPolicy.ownerMemberValue(true, 3));
    }
}
