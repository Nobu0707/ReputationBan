package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReporterPenaltyTest {
    @Test
    void detectsActiveReportBan() {
        long now = 1_000L;
        assertTrue(ReporterPenalty.isReportBanned(1_001L, now));
        assertFalse(ReporterPenalty.isReportBanned(1_000L, now));
        assertFalse(ReporterPenalty.isReportBanned(null, now));
    }

    @Test
    void createsBanUntilWhenThresholdIsReached() {
        long now = 10_000L;
        assertNull(ReporterPenalty.nextReportBannedUntil(true, 4, 5, 7, now));
        assertEquals(now + Duration.ofDays(7).toMillis(), ReporterPenalty.nextReportBannedUntil(true, 5, 5, 7, now));
        assertNull(ReporterPenalty.nextReportBannedUntil(false, 5, 5, 7, now));
    }
}
