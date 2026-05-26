package dev.modplugin.reputationban.model;

import java.util.UUID;

public record PlayerRecord(
        UUID uuid,
        String name,
        int score,
        int banCount,
        int falseReportCount,
        Long reportBannedUntil,
        Long lastRecoveryAt,
        Long firstSeen,
        Long lastSeen
) {
}
