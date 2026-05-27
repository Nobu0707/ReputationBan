package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
