package dev.modplugin.reputationban.integration.griefprevention;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GriefPreventionClaimSummaryTest {
    @Test
    void formatsPresentClaimMetadataAndBounds() {
        GriefPreventionClaimSummary summary = GriefPreventionClaimSummary.present(
                "world",
                100,
                64,
                -20,
                new GriefPreventionClaimEntry("123", false, "hidden", "(90,60,-30)-(120,90,0)", "hidden")
        );

        assertTrue(summary.claimPresent());
        assertTrue(summary.summary().contains("claimPresent=true"));
        assertTrue(summary.summary().contains("claimId=123"));
        assertTrue(summary.summary().contains("bounds=(90,60,-30)-(120,90,0)"));
        assertTrue(summary.metadata().contains("\"claimPresent\":\"true\""));
        assertTrue(summary.metadata().contains("\"owner\":\"hidden\""));
    }

    @Test
    void formatsAbsentClaimMetadata() {
        GriefPreventionClaimSummary summary = GriefPreventionClaimSummary.absent("world", 100, 64, -20);

        assertFalse(summary.claimPresent());
        assertTrue(summary.summary().contains("claimPresent=false"));
        assertTrue(summary.metadata().contains("\"claimPresent\":\"false\""));
    }

    @Test
    void truncatesSummary() {
        String longOwner = "x".repeat(1200);
        GriefPreventionClaimSummary summary = GriefPreventionClaimSummary.present(
                "world",
                1,
                2,
                3,
                new GriefPreventionClaimEntry("123", false, longOwner, "unknown", "hidden")
        );

        assertTrue(summary.summary().length() <= 1000);
    }
}
