package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThresholdReportPolicyTest {
    @Test
    void thresholdOneMeansImmediateAutoAcceptance() {
        assertFalse(ThresholdReportPolicy.shouldQueueForThreshold(1));
        assertTrue(ThresholdReportPolicy.thresholdReached(1, 1));
    }

    @Test
    void thresholdThreeWaitsUntilThirdUniqueReport() {
        assertTrue(ThresholdReportPolicy.shouldQueueForThreshold(3));
        assertFalse(ThresholdReportPolicy.thresholdReached(3, 1));
        assertFalse(ThresholdReportPolicy.thresholdReached(3, 2));
        assertTrue(ThresholdReportPolicy.thresholdReached(3, 3));
    }
}
