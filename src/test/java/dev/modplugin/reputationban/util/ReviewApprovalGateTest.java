package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReviewApprovalGateTest {
    @Test
    void rejectsApprovalCrossingBanThresholdWithoutBanPermission() {
        assertFalse(ReviewApprovalGate.canApprove(false, 1, 0, 0, false));
    }

    @Test
    void allowsApprovalCrossingBanThresholdWithBanPermission() {
        assertTrue(ReviewApprovalGate.canApprove(false, 1, 0, 0, true));
    }

    @Test
    void allowsApprovalThatDoesNotCrossBanThreshold() {
        assertTrue(ReviewApprovalGate.canApprove(false, 10, 5, 0, false));
    }

    @Test
    void rejectsProtectedTargets() {
        assertFalse(ReviewApprovalGate.canApprove(true, 10, 5, 0, true));
    }
}
