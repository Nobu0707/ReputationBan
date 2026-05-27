package dev.modplugin.reputationban.integration.griefprevention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GriefPreventionClaimPolicyTest {
    @Test
    void matchesConfiguredCategoriesOnlyWhenEnabled() {
        assertTrue(GriefPreventionClaimPolicy.shouldCapture(true, true, List.of("griefing", "scam"), "griefing"));
        assertTrue(GriefPreventionClaimPolicy.shouldCapture(true, true, List.of("griefing"), "GRIEFING"));
        assertFalse(GriefPreventionClaimPolicy.shouldCapture(false, true, List.of("griefing"), "griefing"));
        assertFalse(GriefPreventionClaimPolicy.shouldCapture(true, false, List.of("griefing"), "griefing"));
        assertFalse(GriefPreventionClaimPolicy.shouldCapture(true, true, List.of("griefing"), "spam"));
    }

    @Test
    void hidesOwnerAndTrustCountsByDefault() {
        assertEquals("hidden", GriefPreventionClaimPolicy.ownerValue(false, "owner"));
        assertEquals("hidden", GriefPreventionClaimPolicy.trustCountsValue(false, 1, 2, 3, 4));
    }

    @Test
    void includesTrustCountsWhenConfigured() {
        assertEquals(
                "builders=1,containers=2,accessors=3,managers=4",
                GriefPreventionClaimPolicy.trustCountsValue(true, 1, 2, 3, 4)
        );
    }
}
