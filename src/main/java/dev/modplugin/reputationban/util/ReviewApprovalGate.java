package dev.modplugin.reputationban.util;

public final class ReviewApprovalGate {
    private ReviewApprovalGate() {
    }

    public static boolean canApprove(
            boolean targetProtected,
            int oldScore,
            int newScore,
            int banThreshold,
            boolean hasBanPermission
    ) {
        return !targetProtected
                && (!ScoreMath.crossedThresholdDownward(oldScore, newScore, banThreshold) || hasBanPermission);
    }

    public static boolean requiresBanPermission(int oldScore, int newScore, int banThreshold) {
        return ScoreMath.crossedThresholdDownward(oldScore, newScore, banThreshold);
    }
}
