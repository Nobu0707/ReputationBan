package dev.modplugin.reputationban.integration.worldguard;

public record WorldGuardStatus(
        boolean configuredEnabled,
        boolean worldEditPresent,
        boolean worldGuardPresent,
        boolean apiAvailable,
        boolean active,
        boolean reportContextEnabled,
        int maxRegions
) {
}
