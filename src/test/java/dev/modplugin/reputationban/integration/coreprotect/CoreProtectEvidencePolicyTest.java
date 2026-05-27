package dev.modplugin.reputationban.integration.coreprotect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreProtectEvidencePolicyTest {
    @Test
    void matchesConfiguredCategories() {
        assertTrue(CoreProtectEvidencePolicy.shouldCapture(true, true, List.of("griefing"), "griefing"));
        assertTrue(CoreProtectEvidencePolicy.shouldCapture(true, true, List.of("griefing"), "GRIEFING"));
        assertFalse(CoreProtectEvidencePolicy.shouldCapture(false, true, List.of("griefing"), "griefing"));
        assertFalse(CoreProtectEvidencePolicy.shouldCapture(true, false, List.of("griefing"), "griefing"));
        assertFalse(CoreProtectEvidencePolicy.shouldCapture(true, true, List.of("spam"), "griefing"));
    }

    @Test
    void limitsActionsAndSummary() {
        assertEquals(2, CoreProtectEvidencePolicy.clampMaxResults(2, 10));
        assertEquals(0, CoreProtectEvidencePolicy.clampMaxResults(0, 10));
        assertEquals(List.of(0, 1), CoreProtectEvidencePolicy.actionIds(List.of("block-break", "block-place", "unknown")));
        assertEquals("abcde...", CoreProtectEvidencePolicy.truncateSummary("abcdefghi", 8));
    }
}
