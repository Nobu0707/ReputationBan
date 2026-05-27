package dev.modplugin.reputationban.integration.griefprevention;

public record GriefPreventionClaimEntry(
        String claimId,
        boolean adminClaim,
        String owner,
        String bounds,
        String trustCounts
) {
}
