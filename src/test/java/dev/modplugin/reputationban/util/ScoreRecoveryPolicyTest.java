package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ScoreRecoveryPolicyTest {
    @Test
    void skipsPlayersRecoveredWithinLastDay() {
        long now = Duration.ofDays(10).toMillis();
        assertTrue(ScoreRecoveryPolicy.recentlyRecovered(now - Duration.ofHours(1).toMillis(), now));
        assertFalse(ScoreRecoveryPolicy.recentlyRecovered(now - Duration.ofDays(1).toMillis(), now));
        assertFalse(ScoreRecoveryPolicy.recentlyRecovered(null, now));
    }

    @Test
    void requiresEnoughTimeSinceLastValidReport() {
        long now = Duration.ofDays(30).toMillis();
        assertTrue(ScoreRecoveryPolicy.hasEnoughNoReportTime(null, 7, now));
        assertTrue(ScoreRecoveryPolicy.hasEnoughNoReportTime(now - Duration.ofDays(7).toMillis(), 7, now));
        assertFalse(ScoreRecoveryPolicy.hasEnoughNoReportTime(now - Duration.ofDays(6).toMillis(), 7, now));
    }

    @Test
    void recoversWithoutExceedingMaxScore() {
        assertEquals(82, ScoreRecoveryPolicy.recoveredScore(80, 2, 100));
        assertEquals(100, ScoreRecoveryPolicy.recoveredScore(99, 2, 100));
        assertEquals(80, ScoreRecoveryPolicy.recoveredScore(80, -5, 100));
    }
}
