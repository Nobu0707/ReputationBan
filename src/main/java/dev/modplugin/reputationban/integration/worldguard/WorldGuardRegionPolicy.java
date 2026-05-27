package dev.modplugin.reputationban.integration.worldguard;

import java.util.List;

public final class WorldGuardRegionPolicy {
    private WorldGuardRegionPolicy() {
    }

    public static boolean shouldCapture(boolean enabled, boolean reportContextEnabled, List<String> categories, String category) {
        if (!enabled || !reportContextEnabled || category == null) {
            return false;
        }
        return categories.stream().anyMatch(value -> value.equalsIgnoreCase(category));
    }

    public static int clampMaxRegions(int maxRegions, int size) {
        return Math.max(0, Math.min(maxRegions, size));
    }

    public static String ownerMemberValue(boolean include, int count) {
        return include ? "count=" + Math.max(0, count) : "hidden";
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
