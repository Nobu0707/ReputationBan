package dev.modplugin.reputationban.model;

import java.util.UUID;

public record AuditEvent(
        Long id,
        AuditEventType eventType,
        UUID actorUuid,
        String actorName,
        UUID targetUuid,
        String targetName,
        Long reportId,
        Long banId,
        Long scoreHistoryId,
        Integer oldScore,
        Integer newScore,
        Integer delta,
        String reason,
        String metadata,
        long createdAt
) {
    public static AuditEvent create(
            AuditEventType eventType,
            UUID actorUuid,
            String actorName,
            UUID targetUuid,
            String targetName,
            Long reportId,
            Long banId,
            Long scoreHistoryId,
            Integer oldScore,
            Integer newScore,
            Integer delta,
            String reason,
            String metadata,
            long createdAt
    ) {
        return new AuditEvent(
                null,
                eventType,
                actorUuid,
                actorName,
                targetUuid,
                targetName,
                reportId,
                banId,
                scoreHistoryId,
                oldScore,
                newScore,
                delta,
                reason,
                metadata,
                createdAt
        );
    }
}
