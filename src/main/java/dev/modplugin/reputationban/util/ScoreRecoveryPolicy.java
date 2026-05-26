package dev.modplugin.reputationban.util;

import java.time.Duration;

public final class ScoreRecoveryPolicy {
    private ScoreRecoveryPolicy() {
    }

    public static boolean recentlyRecovered(Long lastRecoveryAt, long now) {
        return lastRecoveryAt != null && lastRecoveryAt > now - Duration.ofDays(1).toMillis();
    }

    public static boolean hasEnoughNoReportTime(Long lastValidReportAt, int requiredDays, long now) {
        if (lastValidReportAt == null) {
            return true;
        }
        return lastValidReportAt <= now - Duration.ofDays(Math.max(0, requiredDays)).toMillis();
    }

    public static int recoveredScore(int currentScore, int pointsPerDay, int maxScore) {
        return Math.min(currentScore + Math.max(0, pointsPerDay), maxScore);
    }
}
