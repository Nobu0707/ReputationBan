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

    @Test
    void summaryMetadataIncludesLookupLocationAndApiVersion() {
        String metadata = CoreProtectEvidenceSummary.metadata(3, 3600, 20, "griefing", "world", 100, 64, -20, 11);

        assertTrue(metadata.contains("\"resultCount\":\"3\""));
        assertTrue(metadata.contains("\"lookupSeconds\":\"3600\""));
        assertTrue(metadata.contains("\"radius\":\"20\""));
        assertTrue(metadata.contains("\"category\":\"griefing\""));
        assertTrue(metadata.contains("\"world\":\"world\""));
        assertTrue(metadata.contains("\"x\":\"100\""));
        assertTrue(metadata.contains("\"y\":\"64\""));
        assertTrue(metadata.contains("\"z\":\"-20\""));
        assertTrue(metadata.contains("\"apiVersion\":\"11\""));
    }

    @Test
    void maxResultsZeroSummaryKeepsCountWithoutEntryLines() {
        CoreProtectEvidenceSummary summary = new CoreProtectEvidenceSummary(
                2,
                "CoreProtect: 周辺ログ 2件 world=world x=100 y=64 z=-20 radius=20",
                List.of(),
                CoreProtectEvidenceSummary.metadata(2, 3600, 20, "griefing", "world", 100, 64, -20, 11)
        );

        assertEquals(2, summary.resultCount());
        assertTrue(summary.lines().isEmpty());
        assertTrue(summary.metadata().contains("\"resultCount\":\"2\""));
    }
}
