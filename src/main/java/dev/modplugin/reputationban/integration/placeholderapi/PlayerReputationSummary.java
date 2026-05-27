package dev.modplugin.reputationban.integration.placeholderapi;

import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.util.ReporterPenalty;
import java.util.UUID;

public record PlayerReputationSummary(
        UUID uuid,
        String name,
        int score,
        int maxScore,
        int banCount,
        int falseReportCount,
        Long reportBannedUntil,
        Long lastSeen,
        long loadedAt
) {
    public static PlayerReputationSummary fromRecord(PlayerRecord record, int maxScore, long loadedAt) {
        return new PlayerReputationSummary(
                record.uuid(),
                record.name(),
                record.score(),
                maxScore,
                record.banCount(),
                record.falseReportCount(),
                record.reportBannedUntil(),
                record.lastSeen(),
                loadedAt
        );
    }

    public boolean reportBanned(long now) {
        return ReporterPenalty.isReportBanned(reportBannedUntil, now);
    }

    public int scorePercent() {
        if (maxScore <= 0) {
            return 0;
        }
        return Math.round((score * 100.0F) / maxScore);
    }
}
