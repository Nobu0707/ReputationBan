package dev.modplugin.reputationban.integration.placeholderapi;

import java.util.Map;

public final class PlaceholderStatusPolicy {
    private PlaceholderStatusPolicy() {
    }

    public static String status(int score, Map<String, Integer> thresholds) {
        if (score <= threshold(thresholds, "ban", 0)) {
            return "banned-threshold";
        }
        if (score <= threshold(thresholds, "final-warning", 10)) {
            return "final-warning";
        }
        if (score <= threshold(thresholds, "restricted", 30)) {
            return "restricted";
        }
        if (score <= threshold(thresholds, "watch", 50)) {
            return "watch";
        }
        if (score <= threshold(thresholds, "warning", 70)) {
            return "warning";
        }
        return "normal";
    }

    private static int threshold(Map<String, Integer> thresholds, String key, int fallback) {
        Integer value = thresholds.get(key);
        return value == null ? fallback : value;
    }
}
