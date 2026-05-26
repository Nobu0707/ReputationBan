package dev.modplugin.reputationban.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscordWebhookConfigTest {
    @Test
    void defaultsAreDisabledAndSafe() {
        DiscordWebhookConfig config = DiscordWebhookConfig.defaults();

        assertFalse(config.enabled());
        assertFalse(config.hasUsableUrl());
        assertTrue(config.includeReasons());
        assertFalse(config.includePlayerUuids());
        assertEquals(5, config.timeoutSeconds());
        assertEquals(60, config.rateLimitFailureLogSeconds());
        assertTrue(config.eventEnabled(NotificationEventType.REPORT_CREATED));
        assertFalse(config.eventEnabled(NotificationEventType.RECOVERY_SUMMARY));
    }

    @Test
    void legacyBooleanKeepsUrlEmpty() {
        DiscordWebhookConfig enabled = DiscordWebhookConfig.legacyBoolean(true);
        DiscordWebhookConfig disabled = DiscordWebhookConfig.legacyBoolean(false);

        assertTrue(enabled.enabled());
        assertFalse(enabled.hasUsableUrl());
        assertFalse(disabled.enabled());
    }

    @Test
    void clampsTimeoutAndFailureLogSeconds() {
        DiscordWebhookConfig config = new DiscordWebhookConfig(
                true,
                " ",
                "",
                60,
                -5,
                false,
                true,
                Map.of()
        );

        assertEquals(30, config.timeoutSeconds());
        assertEquals(1, config.rateLimitFailureLogSeconds());
        assertFalse(config.includeReasons());
        assertTrue(config.includePlayerUuids());
    }

    @Test
    void eventSpecificToggleControlsDelivery() {
        DiscordWebhookConfig config = DiscordWebhookConfig.defaults()
                .withEvent(NotificationEventType.REPORT_CREATED, false)
                .withEvent(NotificationEventType.RECOVERY_SUMMARY, true);

        assertFalse(config.eventEnabled(NotificationEventType.REPORT_CREATED));
        assertTrue(config.eventEnabled(NotificationEventType.RECOVERY_SUMMARY));
    }
}
