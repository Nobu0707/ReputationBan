package dev.modplugin.reputationban.integration.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorldGuardRegionSummaryTest {
    @Test
    void includesLocationAndRegionMetadata() {
        WorldGuardRegionSummary summary = WorldGuardRegionSummary.create(
                2,
                "world",
                100,
                64,
                -20,
                1,
                List.of(
                        new WorldGuardRegionEntry("spawn", 10, "hidden", "hidden", Map.of("build", "DENY")),
                        new WorldGuardRegionEntry("market", 5, "hidden", "hidden", Map.of())
                )
        );

        assertEquals(2, summary.regionCount());
        assertEquals(1, summary.regions().size());
        assertTrue(summary.metadata().contains("\"world\":\"world\""));
        assertTrue(summary.metadata().contains("\"x\":\"100\""));
        assertTrue(summary.metadata().contains("\"y\":\"64\""));
        assertTrue(summary.metadata().contains("\"z\":\"-20\""));
        assertTrue(summary.metadata().contains("\"regionCount\":\"2\""));
        assertTrue(summary.metadata().contains("\"maxRegions\":\"1\""));
        assertTrue(summary.summary().contains("#1 id=spawn priority=10 owners=hidden members=hidden"));
    }

    @Test
    void truncatesLongSummary() {
        WorldGuardRegionSummary summary = WorldGuardRegionSummary.create(
                1,
                "world",
                0,
                64,
                0,
                1,
                List.of(new WorldGuardRegionEntry("a".repeat(1200), 1, "hidden", "hidden", Map.of()))
        );

        assertTrue(summary.summary().length() <= 1000);
        assertTrue(summary.summary().endsWith("..."));
    }
}
