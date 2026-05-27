package dev.modplugin.reputationban.integration.discordsrv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.notification.NotificationEventType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscordSrvPolicyTest {
    @Test
    void capturesOnlyConfiguredCategories() {
        assertTrue(DiscordSrvPolicy.shouldCapture(true, true, List.of("griefing"), "griefing"));
        assertFalse(DiscordSrvPolicy.shouldCapture(true, true, List.of("griefing"), "spam"));
        assertFalse(DiscordSrvPolicy.shouldCapture(false, true, List.of("griefing"), "griefing"));
    }

    @Test
    void hidesDiscordIdsByDefault() {
        assertEquals("hidden", DiscordSrvPolicy.discordIdDisplay(false, "123"));
        assertEquals("123", DiscordSrvPolicy.discordIdDisplay(true, "123"));
        assertEquals("-", DiscordSrvPolicy.discordIdDisplay(true, ""));
    }

    @Test
    void notificationEventMustBeEnabled() {
        Map<String, Boolean> events = Map.of("report-created", true, "auto-ban", false);

        assertTrue(DiscordSrvPolicy.shouldNotify(true, true, events, NotificationEventType.REPORT_CREATED));
        assertFalse(DiscordSrvPolicy.shouldNotify(true, true, events, NotificationEventType.AUTO_BAN));
        assertFalse(DiscordSrvPolicy.shouldNotify(false, true, events, NotificationEventType.REPORT_CREATED));
    }

    @Test
    void removesReasonLinesAndTruncates() {
        String sanitized = DiscordSrvPolicy.sanitizeMessage("title\n理由: secret\nメモ: hidden\nkept", false);

        assertEquals("title\nkept", sanitized);
        assertTrue(DiscordSrvPolicy.truncate("abcdef", 5).endsWith("..."));
    }
}
