package dev.modplugin.reputationban.integration.coreprotect;

import java.util.List;
import java.util.Locale;

public final class CoreProtectEvidencePolicy {
    private CoreProtectEvidencePolicy() {
    }

    public static boolean shouldCapture(boolean enabled, boolean reportContextEnabled, List<String> categories, String category) {
        if (!enabled || !reportContextEnabled || category == null) {
            return false;
        }
        return categories.stream().anyMatch(value -> value.equalsIgnoreCase(category));
    }

    public static int clampMaxResults(int maxResults, int size) {
        return Math.max(0, Math.min(maxResults, size));
    }

    public static List<Integer> actionIds(List<String> includeActions) {
        if (includeActions == null || includeActions.isEmpty()) {
            return List.of();
        }
        return includeActions.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .map(CoreProtectEvidencePolicy::actionId)
                .filter(value -> value >= 0)
                .distinct()
                .toList();
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

    private static int actionId(String action) {
        return switch (action) {
            case "block-break" -> 0;
            case "block-place" -> 1;
            default -> -1;
        };
    }
}
