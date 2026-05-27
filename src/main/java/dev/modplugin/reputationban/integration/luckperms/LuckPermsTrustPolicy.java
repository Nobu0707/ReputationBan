package dev.modplugin.reputationban.integration.luckperms;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LuckPermsTrustPolicy {
    private LuckPermsTrustPolicy() {
    }

    public static double weightForGroup(
            boolean useGroupWeight,
            double defaultWeight,
            Map<String, Double> groupWeights,
            String primaryGroup
    ) {
        if (!useGroupWeight || primaryGroup == null || primaryGroup.isBlank()) {
            return defaultWeight;
        }
        return groupWeights.getOrDefault(primaryGroup.toLowerCase(Locale.ROOT), defaultWeight);
    }

    public static boolean isBypassGroup(Set<String> bypassGroups, String primaryGroup) {
        return primaryGroup != null
                && !primaryGroup.isBlank()
                && bypassGroups.contains(primaryGroup.toLowerCase(Locale.ROOT));
    }
}
