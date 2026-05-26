package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuditMetadataTest {
    @Test
    void omitsSecretLikeKeys() {
        String metadata = AuditMetadata.create()
                .put("category", "griefing")
                .put("discord-webhook-url", "WEBHOOK_VALUE_EXAMPLE")
                .put("token", "secret")
                .toJson();

        assertTrue(metadata.contains("category"));
        assertFalse(metadata.contains("WEBHOOK_VALUE_EXAMPLE"));
        assertFalse(metadata.contains("secret"));
    }

    @Test
    void detectsSecretLikeKeys() {
        assertTrue(AuditMetadata.isSecretLike("sessionId"));
        assertTrue(AuditMetadata.isSecretLike("password_hash"));
    }
}
