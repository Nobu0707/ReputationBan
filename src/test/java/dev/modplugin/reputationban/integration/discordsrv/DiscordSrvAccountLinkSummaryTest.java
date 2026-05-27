package dev.modplugin.reputationban.integration.discordsrv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiscordSrvAccountLinkSummaryTest {
    @Test
    void summaryShowsLinkedStateAndHidesIds() {
        DiscordSrvAccountLinkSummary summary = new DiscordSrvAccountLinkSummary(true, false, false, "111", "");

        assertTrue(summary.summary().contains("reporterLinked=true"));
        assertTrue(summary.summary().contains("targetLinked=false"));
        assertTrue(summary.summary().contains("discordIds=hidden"));
        assertFalse(summary.metadata().contains("111"));
    }

    @Test
    void metadataIncludesIdsOnlyWhenEnabled() {
        DiscordSrvAccountLinkSummary summary = new DiscordSrvAccountLinkSummary(true, true, true, "111", "222");

        assertTrue(summary.summary().contains("discordIds=visible"));
        assertTrue(summary.metadata().contains("111"));
        assertTrue(summary.metadata().contains("222"));
    }
}
