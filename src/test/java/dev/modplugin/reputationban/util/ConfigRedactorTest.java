package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigRedactorTest {
    @Test
    void redactsDiscordWebhookUrlAndKeepsNormalSettings() {
        String yaml = """
                # keep this comment
                initial-score: 100
                notify:
                  discord-webhook:
                    enabled: true
                    url: "https://example.invalid/webhook/123/secret"
                """;

        String redacted = ConfigRedactor.redactYaml(yaml);

        assertTrue(redacted.contains("# keep this comment"));
        assertTrue(redacted.contains("initial-score: 100"));
        assertTrue(redacted.contains("discord-webhook:"));
        assertTrue(redacted.contains("enabled: true"));
        assertTrue(redacted.contains("url: \"<redacted>\""));
        assertFalse(redacted.contains("example.invalid"));
    }

    @Test
    void redactsSensitiveKeyValues() {
        String yaml = """
                password: hunter2
                token: abc
                secret: xyz
                session: live
                cookie: baked
                name: visible
                """;

        String redacted = ConfigRedactor.redactYaml(yaml);

        assertTrue(redacted.contains("password: \"<redacted>\""));
        assertTrue(redacted.contains("token: \"<redacted>\""));
        assertTrue(redacted.contains("secret: \"<redacted>\""));
        assertTrue(redacted.contains("session: \"<redacted>\""));
        assertTrue(redacted.contains("cookie: \"<redacted>\""));
        assertTrue(redacted.contains("name: visible"));
    }

    @Test
    void redactsUrlLikeValuesEvenWhenKeyIsNotSensitive() {
        String redacted = Redactor.redactSecretLikeValue("reason before https://example.com/hook after");

        assertTrue(redacted.contains("<redacted>"));
        assertFalse(redacted.contains("https://example.com"));
    }
}
