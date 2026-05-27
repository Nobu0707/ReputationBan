package dev.modplugin.reputationban.integration.worldguard;

import java.util.Map;

public record WorldGuardRegionEntry(
        String id,
        int priority,
        String owners,
        String members,
        Map<String, String> flags
) {
}
