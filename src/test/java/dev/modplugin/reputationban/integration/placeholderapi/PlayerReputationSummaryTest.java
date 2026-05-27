package dev.modplugin.reputationban.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerReputationSummaryTest {
    @Test
    void computesReportBanStateAndScorePercent() {
        long now = 1_700_000_000_000L;
        PlayerReputationSummary summary = new PlayerReputationSummary(
                UUID.randomUUID(),
                "Alex",
                75,
                100,
                1,
                2,
                now + 60_000L,
                now,
                now
        );

        assertTrue(summary.reportBanned(now));
        assertFalse(summary.reportBanned(now + 120_000L));
        assertEquals(75, summary.scorePercent());
    }

    @Test
    void zeroMaxScoreProducesZeroPercent() {
        PlayerReputationSummary summary = new PlayerReputationSummary(
                UUID.randomUUID(), "Alex", 50, 0, 0, 0, null, null, 0L
        );

        assertEquals(0, summary.scorePercent());
    }
}
