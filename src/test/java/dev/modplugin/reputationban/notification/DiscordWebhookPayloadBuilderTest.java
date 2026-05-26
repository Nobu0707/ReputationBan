package dev.modplugin.reputationban.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiscordWebhookPayloadBuilderTest {
    @Test
    void buildsEscapedContentPayload() {
        assertEquals(
                "{\"username\":\"ReputationBan\",\"content\":\"line1\\n\\\"line2\\\"\"}",
                DiscordWebhookPayloadBuilder.buildContentPayload("ReputationBan", "line1\n\"line2\"")
        );
    }

    @Test
    void truncatesLongContent() {
        String content = "x".repeat(2_000);
        String truncated = DiscordWebhookPayloadBuilder.truncateContent(content);

        assertEquals(DiscordWebhookPayloadBuilder.MAX_CONTENT_LENGTH, truncated.length());
        assertTrue(truncated.endsWith("...(truncated)"));
    }
}
