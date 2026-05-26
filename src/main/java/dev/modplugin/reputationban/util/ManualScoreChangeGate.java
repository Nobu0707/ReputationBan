package dev.modplugin.reputationban.util;

public final class ManualScoreChangeGate {
    private ManualScoreChangeGate() {
    }

    public static boolean requiresBanPermission(int oldScore, int newScore, int banThreshold) {
        return ScoreMath.crossedThresholdDownward(oldScore, newScore, banThreshold);
    }
}
