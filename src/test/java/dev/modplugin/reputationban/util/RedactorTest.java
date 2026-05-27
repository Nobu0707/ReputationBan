package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedactorTest {
    @Test
    void redactsDiscordWebhookUrls() {
        String redacted = Redactor.redactSecretLikeValue(
                "before https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz after");

        assertTrue(redacted.contains("<redacted>"));
        assertFalse(redacted.contains("discord.com/api/webhooks"));
    }

    @Test
    void redactsGeneralUrls() {
        String redacted = Redactor.redactSecretLikeValue("see https://example.invalid/path?token=abc now");

        assertEquals("see <redacted> now", redacted);
    }

    @Test
    void redactsFreeTextSecretValues() {
        assertEquals("backup before update token <redacted>",
                Redactor.redactSecretLikeValue("backup before update token abc123"));
        assertEquals("token: <redacted>", Redactor.redactSecretLikeValue("token: abc123"));
        assertEquals("token=<redacted>", Redactor.redactSecretLikeValue("token=abc123"));
        assertEquals("password <redacted>", Redactor.redactSecretLikeValue("password is hunter2"));
        assertEquals("sessionId <redacted>", Redactor.redactSecretLikeValue("sessionId abc"));
    }

    @Test
    void keepsNormalTextMostlyIntact() {
        assertEquals("backup before update because tables changed",
                Redactor.redactSecretLikeValue("backup before update because tables changed"));
    }
}
