package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetentionPolicyTest {
    @Test
    void zeroOrNegativeDisablesCleanup() {
        assertFalse(RetentionPolicy.cleanupEnabled(0));
        assertFalse(RetentionPolicy.cleanupEnabled(-1));
        assertNull(RetentionPolicy.cutoffMillis(0, 1000L));
    }

    @Test
    void computesCutoffFromRetentionDays() {
        long now = 10_000_000L;
        assertTrue(RetentionPolicy.cleanupEnabled(1));
        assertEquals(now - Duration.ofDays(2).toMillis(), RetentionPolicy.cutoffMillis(2, now));
    }
}
