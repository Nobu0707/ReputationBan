package dev.modplugin.reputationban.util;

public final class ScoreMath {
    private ScoreMath() {
    }

    public static int clampToMax(int score, int maxScore) {
        return Math.min(score, maxScore);
    }

    public static boolean crossedThresholdDownward(int oldScore, int newScore, int threshold) {
        return oldScore > threshold && newScore <= threshold;
    }
}
