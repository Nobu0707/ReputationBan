package dev.modplugin.reputationban.integration.griefprevention;

import java.util.List;

public final class GriefPreventionClaimPolicy {
    private GriefPreventionClaimPolicy() {
    }

    public static boolean shouldCapture(boolean enabled, boolean reportContextEnabled, List<String> categories, String category) {
        if (!enabled || !reportContextEnabled || category == null) {
            return false;
        }
        return categories.stream().anyMatch(value -> value.equalsIgnoreCase(category));
    }

    public static String ownerValue(boolean include, String value) {
        if (!include) {
            return "hidden";
        }
        return value == null || value.isBlank() ? "unknown" : truncateSummary(value, 80);
    }

    public static String trustCountsValue(boolean include, int builders, int containers, int accessors, int managers) {
        if (!include) {
            return "hidden";
        }
        return "builders=%d,containers=%d,accessors=%d,managers=%d".formatted(
                Math.max(0, builders),
                Math.max(0, containers),
                Math.max(0, accessors),
                Math.max(0, managers)
        );
    }

    public static String boundsValue(boolean include, String value) {
        if (!include) {
            return "hidden";
        }
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public static String truncateSummary(String summary, int maxLength) {
        if (summary == null) {
            return "";
        }
        if (summary.length() <= maxLength) {
            return summary;
        }
        return summary.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
