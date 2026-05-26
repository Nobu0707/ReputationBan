package dev.modplugin.reputationban.util;

import java.time.Duration;

public final class ReporterPenalty {
    private ReporterPenalty() {
    }

    public static boolean isReportBanned(Long reportBannedUntil, long now) {
        return reportBannedUntil != null && reportBannedUntil > now;
    }

    public static Long nextReportBannedUntil(
            boolean enabled,
            int falseReportCount,
            int threshold,
            int banDays,
            long now
    ) {
        if (!enabled || threshold <= 0 || banDays <= 0 || falseReportCount < threshold) {
            return null;
        }
        return now + Duration.ofDays(banDays).toMillis();
    }
}
