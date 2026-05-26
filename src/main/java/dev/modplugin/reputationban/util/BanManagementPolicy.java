package dev.modplugin.reputationban.util;

public final class BanManagementPolicy {
    private BanManagementPolicy() {
    }

    public static boolean isActive(Long expiresAt, Long unbannedAt, long now) {
        return unbannedAt == null && (expiresAt == null || expiresAt > now);
    }

    public static int pardonTargetScore(int currentScore, int maxScore) {
        return Math.max(currentScore, maxScore);
    }
}
