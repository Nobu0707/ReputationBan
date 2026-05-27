package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportContextTest {
    @Test
    void normalizesProviderAndRedactsMetadata() {
        ReportContext context = new ReportContext(
                1L,
                2L,
                "LuckPerms",
                "primaryGroup=trusted reporterWeight=1.2",
                "{\"webhookUrl\":\"https://discord.com/api/webhooks/123/abcdef\"}",
                3L
        );

        assertEquals("luckperms", context.provider());
        assertFalse(context.metadata().contains("discord.com/api/webhooks"));
    }

    @Test
    void formatsLuckPermsEvidenceFromMetadata() {
        ReportContext context = new ReportContext(
                1L,
                2L,
                "luckperms",
                "primaryGroup=trusted reporterWeight=1.2",
                "{\"primaryGroup\":\"trusted\",\"reporterWeight\":\"1.2\",\"bypassGroup\":\"false\",\"applyWeightToDeduction\":\"false\"}",
                3L
        );

        List<String> lines = ReportContextFormatter.formatEvidence(List.of(context));

        assertEquals("LuckPerms:", lines.get(0));
        assertTrue(lines.contains("  primaryGroup=trusted"));
        assertTrue(lines.contains("  reporterWeight=1.2"));
        assertTrue(lines.contains("  bypassGroup=false"));
        assertTrue(lines.contains("  applyWeightToDeduction=false"));
    }

    @Test
    void formatsCoreProtectEvidenceSummaryLines() {
        ReportContext context = new ReportContext(
                1L,
                2L,
                "coreprotect",
                "CoreProtect: 周辺ログ 2件 world=world x=100 y=64 z=-20 radius=20\n"
                        + "#1 action=Removal player=Target x=100 y=64 z=-20 type=STONE",
                "{\"resultCount\":\"2\"}",
                3L
        );

        List<String> lines = ReportContextFormatter.formatEvidence(List.of(context));

        assertEquals("CoreProtect:", lines.get(0));
        assertTrue(lines.contains("  CoreProtect: 周辺ログ 2件 world=world x=100 y=64 z=-20 radius=20"));
        assertTrue(lines.contains("  #1 action=Removal player=Target x=100 y=64 z=-20 type=STONE"));
    }

    @Test
    void formatsWorldGuardEvidenceSummaryLines() {
        ReportContext context = new ReportContext(
                1L,
                2L,
                "worldguard",
                "WorldGuard: regions 2 world=world x=100 y=64 z=-20\n"
                        + "#1 id=spawn priority=10 owners=hidden members=hidden",
                "{\"regionCount\":\"2\"}",
                3L
        );

        List<String> lines = ReportContextFormatter.formatEvidence(List.of(context));

        assertEquals("WorldGuard:", lines.get(0));
        assertTrue(lines.contains("  WorldGuard: regions 2 world=world x=100 y=64 z=-20"));
        assertTrue(lines.contains("  #1 id=spawn priority=10 owners=hidden members=hidden"));
    }

    @Test
    void formatsNoEvidenceMessage() {
        assertEquals(
                List.of("この通報に保存された連携情報はありません。"),
                ReportContextFormatter.formatEvidence(List.of())
        );
    }
}
