package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SupportBundleSafetyCheckerTest {
    @Test
    void identifiesForbiddenEntryNames() {
        assertTrue(SupportBundleSafetyChecker.isForbiddenEntryName("reputationban.db"));
        assertTrue(SupportBundleSafetyChecker.isForbiddenEntryName("reputationban.db-wal"));
        assertTrue(SupportBundleSafetyChecker.isForbiddenEntryName("logs/latest.log"));
        assertTrue(SupportBundleSafetyChecker.isForbiddenEntryName("../config.yml"));
        assertFalse(SupportBundleSafetyChecker.isForbiddenEntryName("config-redacted.yml"));
        assertFalse(SupportBundleSafetyChecker.isForbiddenEntryName("meta.txt"));
    }

    @Test
    void identifiesWebhookUrlsAndAbsolutePaths() {
        assertTrue(SupportBundleSafetyChecker.containsDiscordWebhook(
                "https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz"));
        assertTrue(SupportBundleSafetyChecker.containsUrl("see https://example.invalid/path"));
        assertTrue(SupportBundleSafetyChecker.containsAbsolutePath("pluginDataFolder=/home/user/server/plugins/ReputationBan"));
        assertTrue(SupportBundleSafetyChecker.containsAbsolutePath("pluginDataFolder=C:\\Users\\user\\server"));
        assertFalse(SupportBundleSafetyChecker.containsAbsolutePath("pluginDataFolder=<plugin-data-folder>"));
    }
}
