package dev.modplugin.reputationban.integration.griefprevention;

public record GriefPreventionStatus(
        boolean configuredEnabled,
        boolean pluginPresent,
        boolean apiAvailable,
        boolean active,
        boolean reportContextEnabled,
        boolean includeClaimOwner,
        boolean includeTrustCounts,
        boolean includeBoundaries
) {
}
