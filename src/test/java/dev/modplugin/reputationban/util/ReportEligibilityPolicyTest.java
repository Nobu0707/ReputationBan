package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReportEligibilityPolicyTest {
    @Test
    void rejectsInsufficientPlaytime() {
        assertFalse(ReportEligibilityPolicy.checkPlaytime(23, 60).allowed());
    }

    @Test
    void rejectsInsufficientFirstSeenAge() {
        long now = 10_000_000L;
        long firstSeen = now - Duration.ofHours(12).toMillis();

        assertFalse(ReportEligibilityPolicy.checkAccountAge(firstSeen, 1, now).allowed());
    }

    @Test
    void disabledRequirementsPass() {
        assertTrue(ReportEligibilityPolicy.checkPlaytime(0, 0).allowed());
        assertTrue(ReportEligibilityPolicy.checkAccountAge(null, 0, System.currentTimeMillis()).allowed());
    }
}
